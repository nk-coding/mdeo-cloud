import type { InferenceProblem, Type, TypeAssignability, TypeEquality, TypirProblem, TypirSpecifics } from "typir";
import { isSubTypeEdge, isConversionEdge } from "typir";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { CustomFunctionType } from "../kinds/custom-function/custom-function-type.js";
import type { CustomLambdaType } from "../kinds/custom-lambda/custom-lambda-type.js";
import type { CustomValueType } from "../kinds/custom-value/custom-value-type.js";
import type { CustomVoidType } from "../kinds/custom-void/custom-void-type.js";
import type { FunctionSignature, ValueType, FunctionType, ReturnType } from "../config/type.js";
import { ClassTypeRef, GenericTypeRef, LambdaType, VoidType } from "../config/type.js";
import type { TypeDefinitionService } from "../service/type-definition-service.js";
import { findCommonParentType, findSuperTypeWithTypeArgs } from "./commonParentType.js";
import { getClassTypeIdentifier } from "./util.js";
import { assertUnreachable } from "@mdeo/language-common";

/**
 * Result of validating a function signature against provided arguments.
 *
 * @template TProblem The type of problems/errors to collect
 */
interface FunctionSignatureValidationResult<TProblem> {
    /**
     * The function signature being validated
     */
    signature: FunctionSignature;
    /**
     * List of validation errors found for this signature
     */
    errors: TProblem[];
    /**
     * The resolved return type, if it could be determined
     */
    returnType: CustomValueType | CustomVoidType | undefined;
    /**
     * The generic resolver used for this signature, needed for score calculation
     */
    genericResolver: GenericResolver<any>;
}

/**
 * Helper class for validating function and lambda calls.
 * Handles type inference, generic type resolution, and argument validation.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 * @template TProblem The type of validation problems to collect
 */
export abstract class CallValidationHelper<Specifics extends TypirSpecifics, TProblem> {
    /**
     * Cache for inferred generic argument types
     */
    private cachedGenericArgumentTypes: (CustomValueType | InferenceProblem<Specifics>[])[] | undefined = undefined;

    /**
     * Gets the inferred types for generic type arguments.
     * Lazily computes and caches the result.
     */
    get genericArgumentTypes(): (CustomValueType | InferenceProblem<Specifics>[])[] {
        if (this.cachedGenericArgumentTypes == undefined) {
            const { InferenceProblem } = this.services.context.typir;
            this.cachedGenericArgumentTypes = this.genericArgumentsNodes.map((node) => {
                const inferredType = this.services.Inference.inferType(node);
                if (this.services.factory.CustomValues.isCustomValueType(inferredType)) {
                    return inferredType;
                } else if (Array.isArray(inferredType)) {
                    return inferredType;
                } else {
                    return [
                        {
                            $problem: InferenceProblem,
                            languageNode: node,
                            location: `Type '${inferredType.getName()}' is not a valid generic argument.`,
                            subProblems: []
                        }
                    ];
                }
            });
        }
        return this.cachedGenericArgumentTypes;
    }

    /**
     * List of validation errors collected during validation
     */
    readonly errors: TProblem[] = [];

    /**
     * The inferred return type of the call, if it could be determined
     */
    inferredReturnType: CustomValueType | CustomVoidType | undefined = undefined;

    /**
     * Creates a new call validation helper.
     * Automatically validates the call during construction.
     *
     * @param languageNode The AST node representing the entire call expression
     * @param functionReferenceNode The AST node representing the function/lambda being called
     * @param genericArgumentsNodes AST nodes for explicit generic type arguments
     * @param argumentNodes AST nodes for the call arguments
     * @param services Extended Typir services for type operations
     * @param isInferenceMode Whether this is used for type inference (vs validation)
     */
    constructor(
        private readonly languageNode: Specifics["LanguageType"],
        private readonly functionReferenceNode: Specifics["LanguageType"],
        private readonly genericArgumentsNodes: Specifics["LanguageType"][],
        private readonly argumentNodes: Specifics["LanguageType"][],
        readonly services: ExtendedTypirServices<Specifics>,
        private readonly isInferenceMode: boolean
    ) {
        const functionReferenceType = services.Inference.inferType(functionReferenceNode);
        if (services.factory.CustomFunctions.isCustomFunctionType(functionReferenceType)) {
            this.validateFunction(functionReferenceType);
        } else if (services.factory.CustomLambdas.isCustomLambdaType(functionReferenceType)) {
            this.validateLambda(functionReferenceType);
        } else if (Array.isArray(functionReferenceType)) {
            if (isInferenceMode) {
                this.errors.push(
                    this.createError(
                        this.functionReferenceNode,
                        `Function reference type could not be determined.`,
                        functionReferenceType
                    )
                );
            }
        } else {
            this.errors.push(
                this.createError(this.functionReferenceNode, `Function reference type is not a function or lambda.`)
            );
        }
    }

    /**
     * Validates a call to a custom function type.
     * Handles overload resolution and generic type inference.
     *
     * @param type The function type being called
     */
    private validateFunction(type: CustomFunctionType) {
        const definition = type.details.definition;

        if (this.trySimpleFunctionInference(type, definition)) {
            return;
        }

        const signatureValidationResults = this.validateAllSignatures(type, definition);

        this.processFunctionSignatureResults(signatureValidationResults);
    }

    /**
     * Attempts a fast-path inference for simple single-signature non-generic functions.
     *
     * @param type The function type being called
     * @param definition The function definition
     * @returns true if inference succeeded and no further validation is needed
     */
    private trySimpleFunctionInference(type: CustomFunctionType, definition: FunctionType): boolean {
        if (
            this.isInferenceMode &&
            definition.signatures.length === 1 &&
            (definition.signatures[0]!.generics ?? []).length === 0
        ) {
            const signature = definition.signatures[0]!;
            const genericResolver = new GenericResolver(type, signature, false, this);
            if (genericResolver.isFullyDefined(signature.returnType)) {
                this.inferredReturnType = genericResolver.resolveType(signature.returnType);
                return true;
            }
        }
        return false;
    }

    /**
     * Validates all signatures of a function against the provided arguments.
     *
     * @param type The function type being called
     * @param definition The function definition containing all signatures
     * @returns Array of validation results for each signature
     */
    private validateAllSignatures(
        type: CustomFunctionType,
        definition: FunctionType
    ): FunctionSignatureValidationResult<TProblem>[] {
        const signatureValidationResults: FunctionSignatureValidationResult<TProblem>[] = [];

        for (const signature of definition.signatures) {
            const result = this.validateSingleSignature(type, signature);
            signatureValidationResults.push(result);
        }

        return signatureValidationResults;
    }

    /**
     * Validates a single function signature against the provided arguments.
     *
     * @param type The function type being called
     * @param signature The specific signature to validate
     * @returns Validation result including errors and return type
     */
    private validateSingleSignature(
        type: CustomFunctionType,
        signature: FunctionSignature
    ): FunctionSignatureValidationResult<TProblem> {
        const genericResolver = new GenericResolver(type, signature, !this.isInferenceMode, this);
        const errors: TProblem[] = [];

        const args = this.argumentNodes.map((argNode) => this.services.Inference.inferType(argNode));
        this.validateSignatureArguments(args, signature, genericResolver, errors);

        this.validateRequiredParameters(args.length, signature, errors);

        return {
            signature,
            errors,
            returnType: genericResolver.isFullyDefined(signature.returnType)
                ? genericResolver.resolveType(signature.returnType)
                : undefined,
            genericResolver
        };
    }

    /**
     * Calculates a score for how well the arguments match the signature.
     * Lower scores are better matches.
     * - Exact type matches have cost 0
     * - Subtype relationships have cost 1 per edge
     * - Conversions have cost 2 per edge
     *
     * @param args The inferred types of the arguments
     * @param signature The signature being validated
     * @param genericResolver The generic type resolver
     * @returns The total cost/score for this signature match
     */
    private calculateSignatureScore(
        args: (Type | InferenceProblem<Specifics>[])[],
        signature: FunctionSignature,
        genericResolver: GenericResolver<Specifics>
    ): number {
        let totalScore = 0;

        for (let i = 0; i < args.length; i++) {
            const argType = args[i];
            if (Array.isArray(argType) || !this.services.factory.CustomValues.isCustomValueType(argType)) {
                continue;
            }

            if (i >= signature.parameters.length) {
                if (signature.isVarArgs !== true) {
                    continue;
                }
            }

            const paramIndex = Math.min(i, signature.parameters.length - 1);
            const paramType = signature.parameters[paramIndex]!.type;
            const declaredType = genericResolver.resolveType(paramType);

            const assignabilityResult = this.services.Assignability.getAssignabilityResult(argType, declaredType);
            if (assignabilityResult.result) {
                for (const edge of assignabilityResult.path) {
                    if (isSubTypeEdge(edge)) {
                        totalScore += 1;
                    } else if (isConversionEdge(edge)) {
                        totalScore += 2;
                    } else {
                        assertUnreachable(edge);
                    }
                }
            }
        }

        return totalScore;
    }

    /**
     * Validates that all provided arguments match the signature's parameters.
     *
     * @param args The inferred types of the arguments
     * @param signature The signature being validated
     * @param genericResolver The generic type resolver
     * @param errors Array to collect validation errors
     */
    private validateSignatureArguments(
        args: (Type | InferenceProblem<Specifics>[])[],
        signature: FunctionSignature,
        genericResolver: GenericResolver<Specifics>,
        errors: TProblem[]
    ): void {
        for (let i = 0; i < args.length; i++) {
            const argType = args[i];
            if (Array.isArray(argType)) {
                errors.push(this.createError(this.argumentNodes[i], `Argument type could not be determined.`, argType));
            } else if (!this.services.factory.CustomValues.isCustomValueType(argType)) {
                errors.push(this.createError(this.argumentNodes[i], `Argument type is not a valid type.`));
            } else if (i >= signature.parameters.length && signature.isVarArgs !== true) {
                errors.push(this.createError(this.argumentNodes[i], `Too many arguments provided for function call.`));
            } else if (!genericResolver.checkAndUpdateArgumentType(i, argType)) {
                errors.push(
                    this.createError(
                        this.argumentNodes[i],
                        `Argument type '${argType.getName()}' is not compatible with parameter type.`
                    )
                );
            }
        }
    }

    /**
     * Validates that all required parameters have been provided.
     *
     * @param argsLength Number of arguments provided
     * @param signature The signature being validated
     * @param errors Array to collect validation errors
     */
    private validateRequiredParameters(argsLength: number, signature: FunctionSignature, errors: TProblem[]): void {
        const requiredParamCount = signature.parameters.length - (signature.isVarArgs === true ? 1 : 0);
        for (let i = argsLength; i < requiredParamCount; i++) {
            errors.push(
                this.createError(
                    this.languageNode,
                    `Not enough arguments provided for function call. Expected at least ${requiredParamCount} but got ${argsLength}.`
                )
            );
        }
    }

    /**
     * Processes signature validation results to determine the best match.
     * Sets either the inferred return type or validation errors.
     *
     * @param signatureValidationResults All signature validation results
     */
    private processFunctionSignatureResults(
        signatureValidationResults: FunctionSignatureValidationResult<TProblem>[]
    ): void {
        const validResults = signatureValidationResults.filter((result) => result.errors.length === 0);

        if (validResults.length === 0) {
            this.handleNoValidSignatures(signatureValidationResults);
            return;
        }

        if (validResults.length === 1) {
            this.inferredReturnType = validResults[0]!.returnType;
            return;
        }

        this.handleMultipleValidSignatures(validResults);
    }

    /**
     * Handles the case when there are no valid signatures.
     * Finds the best matching signatures (with fewest errors) and reports errors.
     *
     * @param signatureValidationResults All signature validation results
     */
    private handleNoValidSignatures(signatureValidationResults: FunctionSignatureValidationResult<TProblem>[]): void {
        signatureValidationResults.sort((a, b) => a.errors.length - b.errors.length);

        const bestResults = signatureValidationResults.filter(
            (result) => result.errors.length === signatureValidationResults[0]!.errors.length
        );

        const subErrors: TypirProblem[] = [];
        for (const result of bestResults) {
            subErrors.push(...(result.errors as TypirProblem[]));
        }

        this.errors.push(this.createError(this.languageNode, `No valid signature found for function call.`, subErrors));

        this.tryInferReturnTypeFromBestResults(bestResults);
    }

    /**
     * Handles the case when there are multiple valid signatures.
     * Uses scoring to find the best match(es).
     *
     * @param validResults All valid signature results
     */
    private handleMultipleValidSignatures(validResults: FunctionSignatureValidationResult<TProblem>[]): void {
        const args = this.argumentNodes.map((argNode) => this.services.Inference.inferType(argNode));

        const resultsWithScores = validResults.map((result) => ({
            result,
            score: this.calculateSignatureScore(args, result.signature, result.genericResolver)
        }));

        resultsWithScores.sort((a, b) => a.score - b.score);

        const bestScore = resultsWithScores[0]!.score;
        const bestScoringResults = resultsWithScores
            .filter((item) => item.score === bestScore)
            .map((item) => item.result);

        if (bestScoringResults.length === 1) {
            this.inferredReturnType = bestScoringResults[0]!.returnType;
            return;
        }

        this.reportAmbiguousSignatures(bestScoringResults);
        this.tryInferReturnTypeFromBestResults(bestScoringResults);
    }

    /**
     * Reports an error for ambiguous function calls with multiple best-matching signatures.
     *
     * @param bestScoringResults The best scoring signature results
     */
    private reportAmbiguousSignatures(bestScoringResults: FunctionSignatureValidationResult<TProblem>[]): void {
        this.errors.push(
            this.createError(
                this.languageNode,
                `Ambiguous function call: multiple valid signatures found with equal scores.`
            )
        );
    }

    /**
     * Attempts to infer the return type from the best matching signatures,
     * even if they have errors, if all have the same return type.
     *
     * @param bestResults Signatures with the fewest errors
     */
    private tryInferReturnTypeFromBestResults(bestResults: FunctionSignatureValidationResult<TProblem>[]): void {
        const allHaveReturnType = bestResults.every((result) => result.returnType !== undefined);
        if (!allHaveReturnType) {
            return;
        }
        const allHaveSameReturnType = bestResults.every((result) =>
            this.services.Equality.areTypesEqual(result.returnType!, bestResults[0]!.returnType!)
        );
        if (allHaveSameReturnType) {
            this.inferredReturnType = bestResults[0]!.returnType;
        }
    }

    /**
     * Validates a call to a lambda type.
     *
     * @param type The lambda type being called
     */
    private validateLambda(type: CustomLambdaType) {
        this.inferredReturnType = type.details.returnType ?? undefined;

        const args = this.argumentNodes.map((argNode) => this.services.Inference.inferType(argNode));

        for (let i = 0; i < args.length; i++) {
            const argType = args[i];
            if (Array.isArray(argType)) {
                this.errors.push(
                    this.createError(this.argumentNodes[i], `Argument type could not be determined.`, argType)
                );
            } else if (!this.services.factory.CustomValues.isCustomValueType(argType)) {
                this.errors.push(this.createError(this.argumentNodes[i], `Argument type is not a valid type.`));
            } else if (i >= type.details.parameterTypes.length) {
                this.errors.push(
                    this.createError(this.argumentNodes[i], `Too many arguments provided for lambda call.`)
                );
            } else {
                const paramType = type.details.parameterTypes[i]!;
                const isAssignable = this.services.Assignability.isAssignable(argType, paramType);
                if (!isAssignable) {
                    this.errors.push(
                        this.createError(
                            this.argumentNodes[i],
                            `Argument type '${argType.getName()}' is not compatible with parameter type '${paramType.getName()}'.`
                        )
                    );
                }
            }
        }

        const requiredParamCount = type.details.parameterTypes.length;
        if (args.length < requiredParamCount) {
            this.errors.push(
                this.createError(
                    this.languageNode,
                    `Not enough arguments provided for lambda call. Expected ${requiredParamCount} but got ${args.length}.`
                )
            );
        }
    }

    /**
     * Creates an error/problem object for a validation failure.
     * Must be implemented by subclasses to create language-specific error objects.
     *
     * @param languageNode The AST node where the error occurred
     * @param message Human-readable error message
     * @param subProblems Optional nested problems that caused this error
     * @returns A problem object of type TProblem
     */
    protected abstract createError(
        languageNode: Specifics["LanguageType"],
        message: string,
        subProblems?: TypirProblem[]
    ): TProblem;
}

/**
 * States for generic type resolution during call validation.
 */
/**
 * States for generic type resolution during call validation.
 */
enum TypeResolutionState {
    /**
     * No type known yet
     */
    Undefined,
    /**
     * Defined, but still generalizable
     */
    Defined,
    /**
     * Defined and cannot be changed anymore
     */
    DefinedInvariant,
    /**
     * Defined via generic argument, cannot be changed
     */
    Fixed,
    /**
     * Has a conflict
     */
    Conflict
}

/**
 * Resolves generic type parameters during function call validation.
 * Manages type inference, variance checking, and type compatibility.
 *
 * @template Specifics Language-specific types extending TypirSpecifics
 */
class GenericResolver<Specifics extends TypirSpecifics> {
    /**
     * Map of generic type names to their resolved types for this call
     */
    private readonly callResolvedGenericArguments = new Map<string, CustomValueType>();

    /**
     * Map of generic type names to their resolution states
     */
    private readonly callResolvedGenericArgumentStates = new Map<string, TypeResolutionState>();

    private readonly typeDefinitionsService: TypeDefinitionService;
    private readonly assignabilityService: TypeAssignability;
    private readonly equalityService: TypeEquality;

    /**
     * Creates a new generic resolver for a specific function call.
     *
     * @param functionType The function type being called
     * @param signature The specific signature being validated
     * @param strict Whether to perform strict validation
     * @param callValidator The parent call validator
     */
    constructor(
        readonly functionType: CustomFunctionType,
        readonly signature: FunctionSignature,
        readonly strict: boolean,
        readonly callValidator: CallValidationHelper<Specifics, any>
    ) {
        this.typeDefinitionsService = callValidator.services.TypeDefinitions;
        this.assignabilityService = callValidator.services.Assignability;
        this.equalityService = callValidator.services.Equality;
        const genericArgumentTypes = callValidator.genericArgumentTypes;
        if (this.signature.generics != undefined && genericArgumentTypes.length > 0) {
            for (const generic of this.signature.generics ?? []) {
                this.callResolvedGenericArgumentStates.set(generic, TypeResolutionState.Conflict);
            }
            if (!strict || genericArgumentTypes.length === this.signature.generics.length) {
                for (let i = 0; i < Math.min(genericArgumentTypes.length, this.signature.generics.length); i++) {
                    const type = genericArgumentTypes[i]!;
                    if (Array.isArray(type)) {
                        continue;
                    }
                    this.callResolvedGenericArguments.set(this.signature.generics![i]!, type);
                    this.callResolvedGenericArgumentStates.set(this.signature.generics![i]!, TypeResolutionState.Fixed);
                }
            }
        } else {
            for (const generic of this.signature.generics ?? []) {
                this.callResolvedGenericArgumentStates.set(generic, TypeResolutionState.Undefined);
            }
        }
    }

    /**
     * Checks if a value type is fully defined (all generic parameters resolved).
     *
     * @param type The value type to check
     * @returns true if all generic parameters are resolved
     */
    isFullyDefined(type: ReturnType): boolean {
        if (VoidType.is(type)) {
            return true;
        } else if (GenericTypeRef.is(type)) {
            const localStatus = this.callResolvedGenericArgumentStates.get(type.generic);
            if (localStatus != undefined) {
                return localStatus !== TypeResolutionState.Undefined && localStatus !== TypeResolutionState.Conflict;
            } else if (this.functionType.details.typeArgs?.has(type.generic)) {
                return true;
            } else {
                return false;
            }
        } else if (ClassTypeRef.is(type)) {
            if (type.typeArgs != undefined) {
                for (const typeArg of type.typeArgs.values()) {
                    if (!this.isFullyDefined(typeArg)) {
                        return false;
                    }
                }
            }
            return true;
        } else if (LambdaType.is(type)) {
            if (!this.isFullyDefined(type.returnType)) {
                return false;
            }
            for (const param of type.parameters) {
                if (!this.isFullyDefined(param.type)) {
                    return false;
                }
            }
            return true;
        } else {
            assertUnreachable(type);
        }
    }

    /**
     * Resolves a value type to a concrete CustomValueType using the current generic mappings.
     *
     * @param type The value type to resolve
     * @returns The resolved concrete type
     */
    resolveType(type: ValueType): CustomValueType;
    resolveType(type: ReturnType): CustomValueType | CustomVoidType;
    resolveType(type: ValueType | ReturnType): CustomValueType | CustomVoidType {
        if (VoidType.is(type)) {
            return this.callValidator.services.factory.CustomVoid.getOrCreate();
        }
        return this.typeDefinitionsService.resolveCustomClassOrLambdaType(
            type,
            new Map([...this.functionType.details.typeArgs.entries(), ...this.callResolvedGenericArguments.entries()])
        );
    }

    /**
     * Checks and updates the type for a specific argument index.
     *
     * @param index The argument index
     * @param type The actual type of the argument
     * @returns true if the argument type is compatible
     */
    checkAndUpdateArgumentType(index: number, type: CustomValueType): boolean {
        if (index < 0 || (index >= this.signature.parameters.length && this.signature.isVarArgs !== true)) {
            return false;
        }
        const paramType = this.signature.parameters[Math.min(index, this.signature.parameters.length - 1)]!.type;
        return this.checkAndUpdateType(paramType, type, true);
    }

    /**
     * Checks if an actual type matches a declared type and updates generic type resolution.
     * Dispatches to specific handlers based on the declared type's structure.
     *
     * @param declaredType The expected type (from the signature)
     * @param actualType The actual type being checked
     * @param allowSubtypes Whether subtypes are acceptable (covariance)
     * @returns true if the types are compatible
     */
    private checkAndUpdateType(declaredType: ValueType, actualType: CustomValueType, allowSubtypes: boolean): boolean {
        if (GenericTypeRef.is(declaredType)) {
            return this.checkAndUpdateGenericType(declaredType, actualType, allowSubtypes);
        } else if (ClassTypeRef.is(declaredType)) {
            return this.checkAndUpdateClassType(declaredType, actualType, allowSubtypes);
        } else {
            return this.checkAndUpdateLambdaType(declaredType, actualType);
        }
    }

    /**
     * Checks and updates a class type, including type arguments.
     *
     * @param declaredType The expected class type
     * @param actualType The actual type being checked
     * @param allowSubtypes Whether to allow subtype relationships
     * @returns true if the types are compatible
     */
    private checkAndUpdateClassType(
        declaredType: {
            type: string;
            typeArgs?: Map<string, ValueType>;
            isNullable?: boolean;
        },
        actualType: CustomValueType,
        allowSubtypes: boolean
    ): boolean {
        const declaredTypeDefinition = this.typeDefinitionsService.getClassType(declaredType.type);
        if (!this.callValidator.services.factory.CustomClasses.isCustomClassType(actualType)) {
            return false;
        }
        if (actualType.isNullable && !declaredType.isNullable) {
            return false;
        }

        let isAllowed: boolean;
        let actualTypeArgs: Map<string, CustomValueType> | undefined = undefined;

        if (!allowSubtypes) {
            isAllowed =
                getClassTypeIdentifier(actualType.details.definition) ===
                getClassTypeIdentifier(declaredTypeDefinition);
            actualTypeArgs = actualType.details.typeArgs;
        } else {
            const result = findSuperTypeWithTypeArgs(actualType, declaredTypeDefinition, this.callValidator.services);
            isAllowed = result != undefined;
            actualTypeArgs = result?.typeArgs;
        }

        if (!isAllowed) {
            return false;
        }

        let isArgsValid = true;
        for (const [typeArgName, typeArgType] of declaredType.typeArgs ?? []) {
            isArgsValid &&= this.checkAndUpdateType(typeArgType, actualTypeArgs!.get(typeArgName)!, false);
        }
        return isArgsValid;
    }

    /**
     * Checks and updates a lambda type, including parameter and return types.
     *
     * @param declaredType The expected lambda type
     * @param actualType The actual type being checked
     * @param allowSubtypes Whether to allow subtype relationships (unused for lambdas)
     * @returns true if the types are compatible
     */
    private checkAndUpdateLambdaType(
        declaredType: {
            returnType: ReturnType;
            parameters: Array<{ type: ValueType }>;
            isNullable?: boolean;
        },
        actualType: CustomValueType
    ): boolean {
        if (!this.callValidator.services.factory.CustomLambdas.isCustomLambdaType(actualType)) {
            return false;
        }
        if (actualType.isNullable && !declaredType.isNullable) {
            return false;
        }

        // Handle void return type
        const declaredReturnType = declaredType.returnType;
        const actualReturnType = actualType.details.returnType;

        let returnTypeValid: boolean;
        if (VoidType.is(declaredReturnType)) {
            // Declared type is void - actual type must also be void
            returnTypeValid = this.callValidator.services.factory.CustomVoid.isCustomVoidType(actualReturnType);
        } else if (this.callValidator.services.factory.CustomVoid.isCustomVoidType(actualReturnType)) {
            // Actual type is void - declared type must also be void
            returnTypeValid = VoidType.is(declaredReturnType);
        } else {
            // Neither is void - check as normal value types
            returnTypeValid = this.checkAndUpdateType(
                declaredReturnType as ValueType,
                actualReturnType as CustomValueType,
                false
            );
        }

        let paramsValid = true;
        for (let i = 0; i < Math.min(declaredType.parameters.length, actualType.details.parameterTypes.length); i++) {
            paramsValid &&= this.checkAndUpdateType(
                declaredType.parameters[i]!.type,
                actualType.details.parameterTypes[i]!,
                false
            );
        }

        return (
            returnTypeValid &&
            paramsValid &&
            declaredType.parameters.length === actualType.details.parameterTypes.length
        );
    }

    /**
     * Checks and updates a generic type parameter resolution.
     * Handles various states: undefined, defined, fixed, and conflict.
     * Supports both covariant (allowSubtypes=true) and invariant (allowSubtypes=false) checking.
     *
     * @param genericType The generic type reference
     * @param actualType The actual type being matched
     * @param allowSubtypes Whether to allow covariant matching
     * @returns true if the types are compatible
     */
    private checkAndUpdateGenericType(
        genericType: GenericTypeRef,
        actualType: CustomValueType,
        allowSubtypes: boolean
    ): boolean {
        const genericName = genericType.generic;
        const isNullable = genericType.isNullable === true;
        const state = this.callResolvedGenericArgumentStates.get(genericName);

        if (state === undefined) {
            return false;
        }

        if (state === TypeResolutionState.Conflict) {
            return false;
        }

        if (state === TypeResolutionState.Undefined) {
            return this.handleUndefinedGenericState(genericName, actualType, isNullable, allowSubtypes);
        }

        return this.handleDefinedGenericState(genericName, actualType, isNullable, allowSubtypes, state);
    }

    /**
     * Handles the case when a generic type parameter is undefined.
     * Sets the initial type and state for the generic parameter.
     *
     * @param genericName The name of the generic parameter
     * @param actualType The actual type being assigned
     * @param isNullable Whether the generic reference is nullable
     * @param allowSubtypes Whether to allow covariant matching
     * @returns true (always succeeds)
     */
    private handleUndefinedGenericState(
        genericName: string,
        actualType: CustomValueType,
        isNullable: boolean,
        allowSubtypes: boolean
    ): boolean {
        this.callResolvedGenericArguments.set(genericName, isNullable ? actualType.asNonNullable : actualType);
        this.callResolvedGenericArgumentStates.set(
            genericName,
            allowSubtypes ? TypeResolutionState.Defined : TypeResolutionState.DefinedInvariant
        );
        return true;
    }

    /**
     * Handles the case when a generic type parameter is already defined.
     * This includes states Defined, DefinedInvariant, and Fixed.
     * Checks compatibility and potentially updates the generic type resolution.
     *
     * @param genericName The name of the generic parameter
     * @param actualType The actual type being checked
     * @param isNullable Whether the generic reference is nullable
     * @param allowSubtypes Whether to allow covariant matching
     * @param state The current resolution state
     * @returns true if the types are compatible
     */
    private handleDefinedGenericState(
        genericName: string,
        actualType: CustomValueType,
        isNullable: boolean,
        allowSubtypes: boolean,
        state: TypeResolutionState
    ): boolean {
        const current = this.callResolvedGenericArguments.get(genericName)!;
        const normalizedActualType = current.isNullable && isNullable ? actualType.asNonNullable : actualType;

        const isEqual = this.equalityService.areTypesEqual(current, normalizedActualType);
        if (isEqual) {
            if (!allowSubtypes && state === TypeResolutionState.Defined) {
                this.callResolvedGenericArgumentStates.set(genericName, TypeResolutionState.DefinedInvariant);
            }
            return true;
        }

        if (state === TypeResolutionState.DefinedInvariant) {
            return this.handleDefinedInvariantGenericState(genericName, actualType, isNullable, allowSubtypes, current);
        }

        if (state === TypeResolutionState.Fixed) {
            return this.handleFixedGenericState(actualType, isNullable, allowSubtypes, current);
        }

        return this.handleDefinedCovariantState(genericName, actualType, isNullable, allowSubtypes, current);
    }

    /**
     * Handles the DefinedInvariant state where the generic type was previously resolved invariantly.
     * In covariant mode, checks if the new type is assignable to the current type.
     * In invariant mode, marks as conflict.
     *
     * @param genericName The name of the generic parameter
     * @param actualType The actual type being checked
     * @param isNullable Whether the generic reference is nullable
     * @param allowSubtypes Whether to allow covariant matching
     * @param current The currently resolved type
     * @returns true if compatible
     */
    private handleDefinedInvariantGenericState(
        genericName: string,
        actualType: CustomValueType,
        isNullable: boolean,
        allowSubtypes: boolean,
        current: CustomValueType
    ): boolean {
        if (!allowSubtypes) {
            this.callResolvedGenericArgumentStates.set(genericName, TypeResolutionState.Conflict);
            return false;
        }

        const isAssignable = this.assignabilityService.isAssignable(
            isNullable ? actualType.asNonNullable : actualType,
            current
        );

        if (!isAssignable) {
            this.callResolvedGenericArgumentStates.set(genericName, TypeResolutionState.Conflict);
            return false;
        }

        return true;
    }

    /**
     * Handles the Fixed state where the generic type was explicitly provided.
     * Only allows subtypes in covariant mode.
     *
     * @param actualType The actual type being checked
     * @param isNullable Whether the generic reference is nullable
     * @param allowSubtypes Whether to allow covariant matching
     * @param current The currently resolved type
     * @returns true if compatible
     */
    private handleFixedGenericState(
        actualType: CustomValueType,
        isNullable: boolean,
        allowSubtypes: boolean,
        current: CustomValueType
    ): boolean {
        if (!allowSubtypes) {
            return false;
        }

        const isAssignable = this.assignabilityService.isAssignable(
            isNullable ? actualType.asNonNullable : actualType,
            current
        );

        return isAssignable;
    }

    /**
     * Handles the Defined (covariant) state where the generic type was previously resolved covariantly.
     * In covariant mode, finds and sets the common parent type.
     * In invariant mode, checks if current is assignable to actual and narrows the type.
     *
     * @param genericName The name of the generic parameter
     * @param actualType The actual type being checked
     * @param isNullable Whether the generic reference is nullable
     * @param allowSubtypes Whether to allow covariant matching
     * @param current The currently resolved type
     * @returns true if compatible
     */
    private handleDefinedCovariantState(
        genericName: string,
        actualType: CustomValueType,
        isNullable: boolean,
        allowSubtypes: boolean,
        current: CustomValueType
    ): boolean {
        if (allowSubtypes) {
            return this.handleCovariantMerge(genericName, actualType, isNullable, current);
        } else {
            return this.handleInvariantNarrowing(genericName, actualType, isNullable, current);
        }
    }

    /**
     * Handles merging types in covariant mode by finding a common parent type.
     *
     * @param genericName The name of the generic parameter
     * @param actualType The actual type being checked
     * @param isNullable Whether the generic reference is nullable
     * @param current The currently resolved type
     * @returns true if a common parent type was found
     */
    private handleCovariantMerge(
        genericName: string,
        actualType: CustomValueType,
        isNullable: boolean,
        current: CustomValueType
    ): boolean {
        const commonType = findCommonParentType(
            current.asNonNullable,
            actualType.asNonNullable,
            this.callValidator.services
        );

        if (commonType !== undefined) {
            this.callResolvedGenericArguments.set(
                genericName,
                current.isNullable || (actualType.isNullable && !isNullable) ? commonType.asNullable : commonType
            );
            return true;
        } else {
            this.callResolvedGenericArgumentStates.set(genericName, TypeResolutionState.Conflict);
            return false;
        }
    }

    /**
     * Handles narrowing the type in invariant mode.
     * Checks if the current type is assignable to the actual type and narrows if so.
     *
     * @param genericName The name of the generic parameter
     * @param actualType The actual type being checked
     * @param isNullable Whether the generic reference is nullable
     * @param current The currently resolved type
     * @returns true if the types are compatible and narrowing succeeded
     */
    private handleInvariantNarrowing(
        genericName: string,
        actualType: CustomValueType,
        isNullable: boolean,
        current: CustomValueType
    ): boolean {
        const normalizedActualType = current.isNullable && isNullable ? actualType.asNonNullable : actualType;
        const isCurrentAssignable = this.assignabilityService.isAssignable(current, normalizedActualType);

        if (isCurrentAssignable) {
            this.callResolvedGenericArguments.set(genericName, normalizedActualType);
            this.callResolvedGenericArgumentStates.set(genericName, TypeResolutionState.DefinedInvariant);
            return true;
        } else {
            this.callResolvedGenericArgumentStates.set(genericName, TypeResolutionState.Conflict);
            return false;
        }
    }
}

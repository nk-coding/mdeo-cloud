import type { TypirLangiumSpecifics } from "typir-langium";
import { PartialTypeSystem } from "./partialTypeSystem.js";
import type { TypeTypes } from "../grammar/typeTypes.js";
import type { ExpressionTypirServices } from "./services.js";
import type { CustomValueType } from "../typir-extensions/kinds/custom-value/custom-value-type.js";
import type { CustomVoidType } from "../typir-extensions/kinds/custom-void/custom-void-type.js";
import type { CustomClassType } from "../typir-extensions/kinds/custom-class/custom-class-type.js";

/**
 * Partial type system implementation for type-related AST nodes.
 * This class handles type inference and validation rules for type declarations,
 * including class types with generics, lambda types, and void types.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class TypePartialTypeSystem<Specifics extends TypirLangiumSpecifics> extends PartialTypeSystem<
    Specifics,
    TypeTypes
> {
    constructor(
        typir: ExpressionTypirServices<Specifics>,
        types: TypeTypes,
        protected readonly nullableAny: CustomClassType
    ) {
        super(typir, types);
    }

    override registerRules(): void {
        this.registerClassTypeInferenceRule();
        this.registerClassTypeValidationRule();
        this.registerVoidTypeInferenceRule();
        this.registerLambdaTypeInferenceRule();
        this.registerLambdaTypeValidationRule();
    }

    /**
     * Registers type inference rule for class type declarations.
     * Handles type resolution with generic type arguments.
     */
    private registerClassTypeInferenceRule(): void {
        this.registerInferenceRule(this.types.classTypeType, (node) => {
            const typeName = node.name;
            const providedTypeArgs = node.typeArgs;

            const classTypeDef = this.typir.TypeDefinitions.getClassTypeIfExisting(typeName);
            if (classTypeDef == undefined) {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `Type '${typeName}' is not defined.`,
                    subProblems: []
                };
            }

            const expectedGenericCount = classTypeDef.generics?.length ?? 0;
            const providedGenericCount = providedTypeArgs.length;

            const inferredTypeArgs: CustomValueType[] = [];
            for (let i = 0; i < Math.min(expectedGenericCount, providedGenericCount); i++) {
                const typeArg = providedTypeArgs[i];
                const inferredType = this.inference.inferType(typeArg);
                if (Array.isArray(inferredType)) {
                    inferredTypeArgs.push(this.nullableAny);
                } else if (this.typir.factory.CustomValues.isCustomValueType(inferredType)) {
                    inferredTypeArgs.push(inferredType);
                } else {
                    inferredTypeArgs.push(this.nullableAny);
                }
            }

            for (let i = providedGenericCount; i < expectedGenericCount; i++) {
                inferredTypeArgs.push(this.nullableAny);
            }

            const typeArgMap = new Map<string, CustomValueType>();
            if (classTypeDef.generics) {
                for (let i = 0; i < expectedGenericCount; i++) {
                    const genericName = classTypeDef.generics[i];
                    typeArgMap.set(genericName, inferredTypeArgs[i]);
                }
            }

            return this.typir.factory.CustomClasses.getOrCreate({
                definition: classTypeDef,
                isNullable: node.isNullable,
                typeArgs: typeArgMap
            });
        });
    }

    /**
     * Registers validation rule for class type declarations.
     * Validates generic parameter count.
     */
    private registerClassTypeValidationRule(): void {
        this.registerValidationRule(this.types.classTypeType, (node, accept) => {
            const typeName = node.name;
            const providedTypeArgs = node.typeArgs;

            const classTypeDef = this.typir.TypeDefinitions.getClassTypeIfExisting(typeName);
            if (!classTypeDef) {
                accept({
                    $problem: this.validationProblem,
                    severity: "error",
                    languageNode: node,
                    message: `Type '${typeName}' is not defined.`,
                    subProblems: []
                });
                return;
            }

            const expectedGenericCount = classTypeDef.generics?.length ?? 0;
            const providedGenericCount = providedTypeArgs.length;

            if (providedGenericCount > expectedGenericCount) {
                accept({
                    $problem: this.validationProblem,
                    severity: "error",
                    languageNode: node,
                    message: `Type '${typeName}' expects ${expectedGenericCount} generic parameter(s), but ${providedGenericCount} were provided.`,
                    subProblems: []
                });
            }

            if (providedGenericCount < expectedGenericCount) {
                accept({
                    $problem: this.validationProblem,
                    severity: "warning",
                    languageNode: node,
                    message: `Type '${typeName}' expects ${expectedGenericCount} generic parameter(s), but only ${providedGenericCount} were provided.`,
                    subProblems: []
                });
            }
        });
    }

    /**
     * Registers type inference rule for void type declarations.
     * Returns the void type singleton.
     */
    private registerVoidTypeInferenceRule(): void {
        this.registerInferenceRule(this.types.voidTypeType, () => {
            return this.typir.factory.CustomVoid.getOrCreate();
        });
    }

    /**
     * Registers type inference rule for lambda type declarations.
     * Handles lambda types with parameter types and return types.
     */
    private registerLambdaTypeInferenceRule(): void {
        this.registerInferenceRule(this.types.lambdaTypeType, (node) => {
            const parameterTypes: CustomValueType[] = [];

            for (let i = 0; i < node.parameters.length; i++) {
                const param = node.parameters[i];
                const inferredType = this.inference.inferType(param);
                if (Array.isArray(inferredType)) {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node,
                        location: `Failed to infer parameter type.`,
                        subProblems: []
                    };
                }
                if (!this.typir.factory.CustomValues.isCustomValueType(inferredType)) {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node,
                        location: `Parameter type must be a value type.`,
                        subProblems: []
                    };
                }
                parameterTypes.push(inferredType);
            }

            const returnTypeNode = node.returnType;
            const inferredReturnType = this.inference.inferType(returnTypeNode);

            if (Array.isArray(inferredReturnType)) {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `Failed to infer return type.`,
                    subProblems: []
                };
            }

            let returnType: CustomValueType | CustomVoidType;

            if (
                this.typir.factory.CustomVoid.isCustomVoidType(inferredReturnType) ||
                this.typir.factory.CustomValues.isCustomValueType(inferredReturnType)
            ) {
                returnType = inferredReturnType;
            } else {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: `Return type must be a value type or void.`,
                    subProblems: []
                };
            }

            return this.typir.factory.CustomLambdas.getOrCreate({
                returnType,
                parameterTypes,
                typeArgs: new Map(),
                isNullable: node.isNullable
            });
        });
    }

    /**
     * Registers validation rule for lambda type declarations.
     * Validates that parameter and return types can be inferred.
     */
    private registerLambdaTypeValidationRule(): void {
        this.registerValidationRule(this.types.lambdaTypeType, (node, accept) => {
            for (const param of node.parameters) {
                const inferredType = this.inference.inferType(param);
                if (Array.isArray(inferredType)) {
                    accept({
                        $problem: this.validationProblem,
                        severity: "error",
                        languageNode: param,
                        message: `Failed to infer parameter type.`,
                        subProblems: []
                    });
                }
            }

            const returnTypeNode = node.returnType;
            const inferredReturnType = this.inference.inferType(returnTypeNode);
            if (Array.isArray(inferredReturnType)) {
                accept({
                    $problem: this.validationProblem,
                    severity: "error",
                    languageNode: returnTypeNode,
                    message: `Failed to infer return type.`,
                    subProblems: []
                });
            }
        });
    }
}

import {
    LambdaType,
    PartialTypeSystem,
    ReturnInferenceAnalyzer,
    ReturnValidationAnalyzer,
    type CustomValueType,
    type CustomVoidType,
    type ExpressionTypirServices,
    type PrimitiveTypes
} from "@mdeo/language-expression";
import { Function, LambdaExpression, Script, type FunctionType, type ScriptType } from "../grammar/types.js";
import { ScriptReturnStatementAccessor } from "./scriptReturnStatementAccessor.js";
import { LambdaScope } from "./lambdaScope.js";
import type { ScriptTypirSpecifics } from "../plugin.js";

/**
 * Partial type system implementation for Script-specific AST nodes.
 * Handles validation rules for Script language constructs like functions with return types.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class ScriptPartialTypeSystem extends PartialTypeSystem<ScriptTypirSpecifics, Record<string, never>> {
    constructor(
        typir: ExpressionTypirServices<ScriptTypirSpecifics>,
        private readonly primitives: PrimitiveTypes
    ) {
        super(typir, {});
    }

    override registerRules(): void {
        this.registerScriptNameConflictsValidationRule();
        this.registerFunctionInferenceRule();
        this.registerFunctionValidationRule();
        this.registerFunctionDuplicateParametersValidationRule();
        this.registerLambdaInferenceRule();
        this.registerLambdaValidationRule();
    }

    /**
     * Registers a validation rule for Script to detect name conflicts.
     * Validates that there are no naming conflicts between function definitions and imports.
     */
    private registerScriptNameConflictsValidationRule(): void {
        this.registerValidationRule(Script, (node, accept) => {
            const scriptNode = node as ScriptType;
            const functionNames = new Set<string>();
            const importNames = new Set<string>();

            for (const func of scriptNode.functions) {
                if (functionNames.has(func.name)) {
                    accept({
                        languageNode: func,
                        languageProperty: "name",
                        message: `Duplicate function name '${func.name}'.`,
                        severity: "error"
                    });
                }
                functionNames.add(func.name);
            }

            for (const fileImport of scriptNode.imports) {
                for (const namedImport of fileImport.imports) {
                    const importName = namedImport.name ?? namedImport.entity?.ref?.name;

                    if (importName == undefined) {
                        continue;
                    }

                    if (importNames.has(importName)) {
                        accept({
                            languageNode: namedImport,
                            message: `Duplicate import name '${importName}'.`,
                            severity: "error"
                        });
                    }
                    importNames.add(importName);

                    if (functionNames.has(importName)) {
                        accept({
                            languageNode: namedImport,
                            languageProperty: namedImport.name != undefined ? "name" : "entity",
                            message: `Name conflict: '${importName}' is already defined as a function.`,
                            severity: "error"
                        });
                    }
                }
            }
        });
    }

    /**
     * Registers a type inference rule for functions.
     * Infers the function's return type and parameter types based on their definitions.
     */
    private registerFunctionInferenceRule(): void {
        this.registerInferenceRule(Function, (node) => {
            let returnType: CustomValueType | CustomVoidType;
            if (node.returnType != undefined) {
                const inferredReturnType = this.inference.inferType(node.returnType);
                if (
                    this.typir.factory.CustomValues.isCustomValueType(inferredReturnType) ||
                    this.typir.factory.CustomVoid.isCustomVoidType(inferredReturnType)
                ) {
                    returnType = inferredReturnType;
                } else {
                    returnType = this.primitives.Any.asNullable;
                }
            } else {
                returnType = this.typir.factory.CustomVoid.getOrCreate();
            }

            const parameters = node.parameters.map((param) => {
                const inferredParamType = this.inference.inferType(param.type);
                const paramType = this.typir.factory.CustomValues.isCustomValueType(inferredParamType)
                    ? inferredParamType
                    : this.primitives.Any.asNullable;
                return {
                    name: param.name,
                    type: paramType.definition
                };
            });

            return this.typir.factory.CustomFunctions.create({
                definition: {
                    signatures: [
                        {
                            returnType: returnType.definition,
                            parameters: parameters
                        }
                    ]
                },
                name: node.name,
                typeArgs: new Map()
            });
        });
    }

    /**
     * Registers a validation rule for function return types.
     * Validates that all return statements in a function body match the declared return type,
     * and that all code paths return a value if the return type is not void.
     */
    private registerFunctionValidationRule(): void {
        const accessor = new ScriptReturnStatementAccessor<ScriptTypirSpecifics>(this.typir);

        this.registerValidationRule(Function, (node, accept) => {
            const functionNode = node as FunctionType;
            const bodyScope = this.typir.ScopeProvider.getScope(functionNode.body);
            if (bodyScope == undefined) {
                return;
            }

            let expectedReturnType: CustomValueType | CustomVoidType;
            let expectedReturnTypeLanguageNode: ScriptTypirSpecifics["LanguageType"];
            if (functionNode.returnType != undefined) {
                const inferredReturnType = this.inference.inferType(functionNode.returnType);
                if (Array.isArray(inferredReturnType)) {
                    return;
                }
                if (
                    !this.typir.factory.CustomValues.isCustomValueType(inferredReturnType) &&
                    !this.typir.factory.CustomVoid.isCustomVoidType(inferredReturnType)
                ) {
                    accept({
                        languageNode: functionNode.returnType,
                        message: "Return type must be a valid value or void type.",
                        severity: "error"
                    });
                    return;
                }
                expectedReturnType = inferredReturnType;
                expectedReturnTypeLanguageNode = functionNode.returnType;
            } else {
                expectedReturnType = this.typir.factory.CustomVoid.getOrCreate();
                expectedReturnTypeLanguageNode = functionNode;
            }

            const analyzer = new ReturnValidationAnalyzer<ScriptTypirSpecifics>(
                bodyScope,
                expectedReturnType,
                expectedReturnTypeLanguageNode,
                this.typir,
                accessor
            );

            for (const error of analyzer.errors) {
                accept(error);
            }
        });
    }

    /**
     * Registers a validation rule for duplicate function parameters.
     * Validates that no function has duplicate parameter names.
     */
    private registerFunctionDuplicateParametersValidationRule(): void {
        this.registerValidationRule(Function, (node, accept) => {
            const functionNode = node as FunctionType;
            const parameterNames = new Set<string>();
            for (const param of functionNode.parameters) {
                if (parameterNames.has(param.name)) {
                    accept({
                        languageNode: param,
                        message: `Duplicate parameter name '${param.name}'.`,
                        severity: "error"
                    });
                }
                parameterNames.add(param.name);
            }
        });
    }

    /**
     * Registers a type inference rule for lambda expressions.
     * Uses a two-step inference process:
     * 1. Get the lambda scope and its type inference result
     * 2. If the result is complete, use it; otherwise infer the return type from the lambda body
     */
    private registerLambdaInferenceRule(): void {
        this.registerInferenceRule(LambdaExpression, (node) => {
            const scope = this.typir.ScopeProvider.getScope(node).scope;

            if (!(scope instanceof LambdaScope)) {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: "Lambda scope could not be created.",
                    subProblems: []
                };
            }

            const lambdaTypeInference = scope.lambdaTypeInference;

            if (Array.isArray(lambdaTypeInference)) {
                return lambdaTypeInference[0];
            }

            if ("type" in lambdaTypeInference) {
                return lambdaTypeInference.type;
            }

            const parameterTypes = lambdaTypeInference.parameterTypes;

            let returnType: CustomValueType | CustomVoidType;
            if (node.expression != undefined) {
                const expressionType = this.inference.inferType(node.expression);
                if (Array.isArray(expressionType)) {
                    return expressionType[0];
                }
                if (!this.typir.factory.CustomValues.isCustomValueType(expressionType)) {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node.expression,
                        location: "Lambda expression must return a value type.",
                        subProblems: []
                    };
                }
                returnType = expressionType;
            } else if (node.body != undefined) {
                const accessor = new ScriptReturnStatementAccessor<ScriptTypirSpecifics>(this.typir);
                const bodyScope = this.typir.ScopeProvider.getScope(node.body);
                if (bodyScope == undefined) {
                    return {
                        $problem: this.inferenceProblem,
                        languageNode: node.body,
                        location: "Lambda body scope could not be created.",
                        subProblems: []
                    };
                }

                const analyzer = new ReturnInferenceAnalyzer<ScriptTypirSpecifics>(bodyScope, this.typir, accessor);

                if (analyzer.errors.length > 0) {
                    return analyzer.errors[0];
                }

                if (analyzer.returnType == undefined) {
                    returnType = this.typir.factory.CustomVoid.getOrCreate();
                } else {
                    returnType = analyzer.returnType;
                }
            } else {
                return {
                    $problem: this.inferenceProblem,
                    languageNode: node,
                    location: "Lambda must have either an expression or a body.",
                    subProblems: []
                };
            }

            const parameters = parameterTypes.map((type: CustomValueType, index: number) => {
                return {
                    name: node.parameters[index]?.name ?? `param${index}`,
                    type: type.definition
                };
            });
            const returnTypeDefinition = returnType.definition;

            const lambdaDefinition: LambdaType = {
                parameters,
                returnType: returnTypeDefinition,
                isNullable: false
            };

            return this.typir.TypeDefinitions.resolveCustomClassOrLambdaType(lambdaDefinition, new Map());
        });
    }

    /**
     * Registers validation rules for lambda expressions.
     * Validates:
     * - Return types in lambda blocks match the inferred return type
     * - No too many parameters (compared to expected type)
     * - No duplicate parameter names
     */
    private registerLambdaValidationRule(): void {
        const accessor = new ScriptReturnStatementAccessor<ScriptTypirSpecifics>(this.typir);

        this.registerValidationRule(LambdaExpression, (node, accept) => {
            const scope = this.typir.ScopeProvider.getScope(node);

            if (!(scope instanceof LambdaScope)) {
                return;
            }

            const lambdaTypeInference = scope.lambdaTypeInference;

            const parameterNames = new Set<string>();
            for (const param of node.parameters) {
                if (parameterNames.has(param.name)) {
                    accept({
                        languageNode: param,
                        message: `Duplicate parameter name '${param.name}'.`,
                        severity: "error"
                    });
                }
                parameterNames.add(param.name);
            }

            if (Array.isArray(lambdaTypeInference)) {
                return;
            }

            const expectedParamCount =
                "type" in lambdaTypeInference
                    ? lambdaTypeInference.type.details.parameterTypes.length
                    : lambdaTypeInference.parameterTypes.length;

            if (node.parameters.length > expectedParamCount) {
                accept({
                    languageNode: node,
                    message: `Lambda has too many parameters. Expected ${expectedParamCount}, got ${node.parameters.length}.`,
                    severity: "error"
                });
            }

            if (node.body != undefined) {
                const inferredLambdaType = this.inference.inferType(node);
                if (Array.isArray(inferredLambdaType)) {
                    return;
                }
                if (!this.typir.factory.CustomLambdas.isCustomLambdaType(inferredLambdaType)) {
                    return;
                }

                const expectedReturnType = inferredLambdaType.details.returnType;
                const bodyScope = this.typir.ScopeProvider.getScope(node.body);
                if (bodyScope == undefined) {
                    return;
                }

                const analyzer = new ReturnValidationAnalyzer<ScriptTypirSpecifics>(
                    bodyScope,
                    expectedReturnType,
                    node,
                    this.typir,
                    accessor
                );

                for (const error of analyzer.errors) {
                    accept(error);
                }
            }
        });
    }
}

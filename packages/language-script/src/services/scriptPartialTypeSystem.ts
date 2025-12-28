import type { TypirLangiumSpecifics } from "typir-langium";
import {
    PartialTypeSystem,
    type CustomValueType,
    type CustomVoidType,
    type ExpressionTypirServices
} from "@mdeo/language-expression";
import { ReturnValidationAnalyzer } from "@mdeo/language-expression";
import { Function, type FunctionType } from "../grammar/types.js";
import { ScriptReturnStatementAccessor } from "./scriptReturnStatementAccessor.js";

/**
 * Partial type system implementation for Script-specific AST nodes.
 * Handles validation rules for Script language constructs like functions with return types.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class ScriptPartialTypeSystem<Specifics extends TypirLangiumSpecifics> extends PartialTypeSystem<
    Specifics,
    Record<string, never>
> {
    constructor(typir: ExpressionTypirServices<Specifics>) {
        super(typir, {} as Record<string, never>);
    }

    override registerRules(): void {
        this.registerFunctionValidationRule();
    }

    /**
     * Registers a validation rule for function return types.
     * Validates that all return statements in a function body match the declared return type,
     * and that all code paths return a value if the return type is not void.
     */
    private registerFunctionValidationRule(): void {
        const accessor = new ScriptReturnStatementAccessor<Specifics>(this.typir);

        this.registerValidationRule(Function, (node, accept) => {
            const functionNode = node as FunctionType;
            const bodyScope = this.typir.ScopeProvider.getScope(functionNode.body);
            if (bodyScope == undefined) {
                return;
            }

            let expectedReturnType: CustomValueType | CustomVoidType;
            let expectedReturnTypeLanguageNode: Specifics["LanguageType"];
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

            const analyzer = new ReturnValidationAnalyzer<Specifics>(
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
}

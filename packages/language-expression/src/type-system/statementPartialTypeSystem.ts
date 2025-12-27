import type { TypirLangiumSpecifics } from "typir-langium";
import { PartialTypeSystem, type PrimitiveTypes } from "./partialTypeSystem.js";
import type { StatementTypes } from "../grammar/statementTypes.js";
import type { ExpressionTypirServices } from "./services.js";
import type { CustomClassType } from "../typir-extensions/kinds/custom-class/custom-class-type.js";

/**
 * Partial type syste m implementation for statement-related AST nodes.
 *
 * @template Specifics The language-specific type system configuration extending TypirLangiumSpecifics
 */
export class StatementPartialTypeSystem<Specifics extends TypirLangiumSpecifics> extends PartialTypeSystem<
    Specifics,
    StatementTypes
> {
    constructor(
        typir: ExpressionTypirServices<Specifics>,
        types: StatementTypes,
        protected readonly primitiveTypes: PrimitiveTypes,
        protected readonly nullablePrimitiveTypes: PrimitiveTypes,
        protected readonly voidType: CustomClassType
    ) {
        super(typir, types);
    }

    override registerRules(): void {
        this.registerInferenceRule(this.types.variableDeclarationStatementType, (node) => {
            if (node.type != undefined) {
                return node.type;
            } else {
                return node.initialValue!;
            }
        });

        this.registerValidationRule(this.types.ifStatementType, (node, accept) => {
            this.validateBooleanCondition(node.condition, accept, "If statement condition must be of type boolean.");
        });

        this.registerValidationRule(this.types.elseIfClauseType, (node, accept) => {
            this.validateBooleanCondition(node.condition, accept, "Else-if clause condition must be of type boolean.");
        });

        this.registerValidationRule(this.types.whileStatementType, (node, accept) => {
            this.validateBooleanCondition(node.condition, accept, "While statement condition must be of type boolean.");
        });

        this.registerValidationRule(this.types.doWhileStatementType, (node, accept) => {
            this.validateBooleanCondition(
                node.condition,
                accept,
                "Do-while statement condition must be of type boolean."
            );
        });
    }

    /**
     * Helper to validate that a condition expression is assignable to boolean.
     * Keeps the repeated logic in one place to reduce duplication.
     */
    private validateBooleanCondition(condition: any, accept: (p: any) => void, message: string): void {
        const conditionType = this.inference.inferType(condition);
        if (Array.isArray(conditionType)) {
            return;
        }
        if (!this.assignability.isAssignable(conditionType, this.primitiveTypes.boolean)) {
            accept({
                $problem: this.validationProblem,
                languageNode: condition,
                message,
                severity: "error"
            });
        }
    }
}

import { createInterface, Optional } from "@mdeo/language-common";
import type { StatementConfig } from "./statementConfig.js";
import type { ExpressionTypes } from "./expressionTypes.js";

/**
 * Generates statement-related type interfaces based on the provided configuration.
 *
 * @param config config used for generating type names
 * @param expressionTypes the expression types to reference in statement types
 * @returns the generated statement types
 */
export function generateStatementTypes(config: StatementConfig, expressionTypes: ExpressionTypes) {
    const baseTypeType = expressionTypes.baseTypeType;
    const baseExpressionType = expressionTypes.baseExpressionType;

    const baseStatementType = createInterface(config.baseStatementTypeName).attrs({});

    const statementsScopeType = createInterface(config.statementsScopeTypeName).attrs({
        statements: [baseStatementType]
    });

    const elseIfClauseType = createInterface(config.elseIfClauseTypeName).attrs({
        condition: baseExpressionType,
        thenBlock: statementsScopeType
    });

    const ifStatementType = createInterface(config.ifStatementTypeName)
        .extends(baseStatementType)
        .attrs({
            condition: baseExpressionType,
            thenBlock: statementsScopeType,
            elseIfs: [elseIfClauseType],
            elseBlock: Optional(statementsScopeType)
        });

    const whileStatementType = createInterface(config.whileStatementTypeName).extends(baseStatementType).attrs({
        condition: baseExpressionType,
        body: statementsScopeType
    });

    const doWhileStatementType = createInterface(config.doWhileStatementTypeName).extends(baseStatementType).attrs({
        body: statementsScopeType,
        condition: baseExpressionType
    });

    const forStatementType = createInterface(config.forStatementTypeName).extends(baseStatementType).attrs({
        variable: String,
        iterable: baseExpressionType,
        body: statementsScopeType
    });

    const variableDeclarationStatementType = createInterface(config.variableDeclarationStatementTypeName)
        .extends(baseStatementType)
        .attrs({
            name: String,
            type: Optional(baseTypeType),
            initialValue: Optional(baseExpressionType)
        });

    const assignmentStatementType = createInterface(config.assignmentStatementTypeName)
        .extends(baseStatementType)
        .attrs({
            left: baseExpressionType,
            right: baseExpressionType
        });

    const expressionStatementType = createInterface(config.expressionStatementTypeName)
        .extends(baseStatementType)
        .attrs({
            expression: baseExpressionType
        });

    return {
        baseStatementType,
        ifStatementType,
        elseIfClauseType,
        whileStatementType,
        doWhileStatementType,
        forStatementType,
        variableDeclarationStatementType,
        assignmentStatementType,
        expressionStatementType,
        statementsScopeType
    };
}

/**
 * Type representing all generated statement types.
 */
export type StatementTypes = ReturnType<typeof generateStatementTypes>;

/**
 * Type representing the base statement type.
 */
export type BaseStatementType = StatementTypes["baseStatementType"];

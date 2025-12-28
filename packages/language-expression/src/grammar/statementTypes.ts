import { createInterface, Optional, type ASTType } from "@mdeo/language-common";
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

    const forStatementVariableDeclarationType = createInterface(config.forStatementVariableDeclarationTypeName).attrs({
        name: String,
        type: Optional(baseTypeType)
    });

    const forStatementType = createInterface(config.forStatementTypeName).extends(baseStatementType).attrs({
        variable: forStatementVariableDeclarationType,
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
            left: expressionTypes.assignableExpressionType,
            right: baseExpressionType
        });

    const expressionStatementType = createInterface(config.expressionStatementTypeName)
        .extends(baseStatementType)
        .attrs({
            expression: baseExpressionType
        });

    const breakStatementType = createInterface(config.breakStatementTypeName).extends(baseStatementType).attrs({});

    const continueStatementType = createInterface(config.continueStatementTypeName)
        .extends(baseStatementType)
        .attrs({});

    return {
        baseStatementType,
        ifStatementType,
        elseIfClauseType,
        whileStatementType,
        forStatementType,
        forStatementVariableDeclarationType,
        variableDeclarationStatementType,
        assignmentStatementType,
        expressionStatementType,
        breakStatementType,
        continueStatementType,
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
export type BaseStatementType = ASTType<StatementTypes["baseStatementType"]>;

/**
 * Type representing the statements scope type.
 */
export type StatementsScopeType = ASTType<StatementTypes["statementsScopeType"]>;

/**
 * Type representing the for statement type.
 */
export type ForStatementType = ASTType<StatementTypes["forStatementType"]>;

/**
 * Type representing the for statement variable declaration type.
 */
export type ForStatementVariableDeclarationType = ASTType<StatementTypes["forStatementVariableDeclarationType"]>;

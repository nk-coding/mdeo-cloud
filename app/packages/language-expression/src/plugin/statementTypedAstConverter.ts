import type { AstNode } from "langium";
import type {
    StatementTypes,
    IfStatementType,
    WhileStatementType,
    ForStatementType,
    VariableDeclarationStatementType,
    AssignmentStatementType,
    ExpressionStatementType,
    ElseIfClauseType
} from "../grammar/statementTypes.js";
import { TypedAstConverter } from "./typedAstConverter.js";
import type {
    TypedCallableBody,
    TypedStatement,
    TypedIfStatement,
    TypedWhileStatement,
    TypedForStatement,
    TypedVariableDeclarationStatement,
    TypedAssignmentStatement,
    TypedIdentifierExpression,
    TypedMemberAccessExpression,
    TypedExpressionStatement,
    TypedBreakStatement,
    TypedContinueStatement
} from "./typedAst.js";

/**
 * Statement converter that extends ExpressionTypedAstConverter with statement handling capabilities.
 * Handles conversion of control flow statements, variable declarations, assignments, etc.
 */
export abstract class StatementTypedAstConverter extends TypedAstConverter {
    /**
     * Statement type identifiers for checking instance types.
     */
    protected abstract statementTypes: StatementTypes;

    /**
     * Extracts statements from a callable body node.
     *
     * @param body The body node
     * @returns Array of statement AST nodes
     */
    protected getStatementsFromBody(body: AstNode): AstNode[] {
        if (this.reflection.isInstance(body, this.statementTypes.statementsScopeType)) {
            return (body as any).statements ?? [];
        }
        return [];
    }

    /**
     * Converts a callable body (function or lambda body) to a TypedCallableBody.
     *
     * @param body The statements scope node
     * @returns The TypedCallableBody representation
     */
    protected convertCallableBody(body: AstNode): TypedCallableBody {
        const statements = this.getStatementsFromBody(body);
        return {
            body: statements.map((statement) => this.convertStatement(statement))
        };
    }

    /**
     * Converts a statement AST node to a TypedStatement.
     *
     * @param statement The statement AST node
     * @returns The TypedStatement representation
     */
    protected convertStatement(statement: AstNode): TypedStatement {
        if (this.reflection.isInstance(statement, this.statementTypes.ifStatementType)) {
            return this.convertIfStatement(statement as IfStatementType);
        } else if (this.reflection.isInstance(statement, this.statementTypes.whileStatementType)) {
            return this.convertWhileStatement(statement as WhileStatementType);
        } else if (this.reflection.isInstance(statement, this.statementTypes.forStatementType)) {
            return this.convertForStatement(statement as ForStatementType);
        } else if (this.reflection.isInstance(statement, this.statementTypes.variableDeclarationStatementType)) {
            return this.convertVariableDeclarationStatement(statement as VariableDeclarationStatementType);
        } else if (this.reflection.isInstance(statement, this.statementTypes.assignmentStatementType)) {
            return this.convertAssignmentStatement(statement as AssignmentStatementType);
        } else if (this.reflection.isInstance(statement, this.statementTypes.expressionStatementType)) {
            return this.convertExpressionStatement(statement as ExpressionStatementType);
        } else if (this.reflection.isInstance(statement, this.statementTypes.breakStatementType)) {
            return this.convertBreakStatement();
        } else if (this.reflection.isInstance(statement, this.statementTypes.continueStatementType)) {
            return this.convertContinueStatement();
        } else {
            return this.convertAdditionalStatement(statement);
        }
    }

    /**
     * Hook for subclasses to handle additional statement types not covered by the base converter.
     * Should throw an error if the statement type is not supported.
     *
     * @param statement The statement AST node
     * @returns The TypedStatement representation
     */
    protected convertAdditionalStatement(statement: AstNode): TypedStatement {
        throw new Error(`Unsupported statement type: ${statement.$type}`);
    }

    /**
     * Converts an if statement.
     *
     * @param statement The if statement AST node
     * @returns The TypedIfStatement representation
     */
    protected convertIfStatement(statement: IfStatementType): TypedIfStatement {
        return {
            kind: "if",
            condition: this.convertExpression(statement.condition),
            thenBlock: this.getStatementsFromBody(statement.thenBlock).map((s) => this.convertStatement(s)),
            elseIfs: (statement.elseIfs ?? []).map((elseIf: ElseIfClauseType) => ({
                condition: this.convertExpression(elseIf.condition),
                thenBlock: this.getStatementsFromBody(elseIf.thenBlock).map((s) => this.convertStatement(s))
            })),
            elseBlock: statement.elseBlock
                ? this.getStatementsFromBody(statement.elseBlock).map((s) => this.convertStatement(s))
                : undefined
        };
    }

    /**
     * Converts a while statement.
     *
     * @param statement The while statement AST node
     * @returns The TypedWhileStatement representation
     */
    protected convertWhileStatement(statement: WhileStatementType): TypedWhileStatement {
        return {
            kind: "while",
            condition: this.convertExpression(statement.condition),
            body: this.getStatementsFromBody(statement.body).map((s) => this.convertStatement(s))
        };
    }

    /**
     * Converts a for statement.
     *
     * @param statement The for statement AST node
     * @returns The TypedForStatement representation
     */
    protected convertForStatement(statement: ForStatementType): TypedForStatement {
        return {
            kind: "for",
            variableName: statement.variable.name,
            variableType: this.getTypeIndex(statement.variable),
            iterable: this.convertExpression(statement.iterable),
            body: this.getStatementsFromBody(statement.body).map((s) => this.convertStatement(s))
        };
    }

    /**
     * Converts a variable declaration statement.
     *
     * @param statement The variable declaration statement AST node
     * @returns The TypedVariableDeclarationStatement representation
     */
    protected convertVariableDeclarationStatement(
        statement: VariableDeclarationStatementType
    ): TypedVariableDeclarationStatement {
        return {
            kind: "variableDeclaration",
            name: statement.name,
            type: this.getTypeIndex(statement),
            initialValue:
                statement.initialValue != undefined ? this.convertExpression(statement.initialValue) : undefined
        };
    }

    /**
     * Converts an assignment statement.
     *
     * @param statement The assignment statement AST node
     * @returns The TypedAssignmentStatement representation
     */
    protected convertAssignmentStatement(statement: AssignmentStatementType): TypedAssignmentStatement {
        const left = this.convertExpression(statement.left);
        const right = this.convertExpression(statement.right);

        if (left.kind !== "identifier" && left.kind !== "memberAccess") {
            throw new Error("Assignment left side must be an identifier or member access");
        }

        return {
            kind: "assignment",
            left: left as TypedIdentifierExpression | TypedMemberAccessExpression,
            right
        };
    }

    /**
     * Converts an expression statement.
     *
     * @param statement The expression statement AST node
     * @returns The TypedExpressionStatement representation
     */
    protected convertExpressionStatement(statement: ExpressionStatementType): TypedExpressionStatement {
        return {
            kind: "expression",
            expression: this.convertExpression(statement.expression)
        };
    }

    /**
     * Converts a break statement.
     *
     * @returns The TypedBreakStatement representation
     */
    protected convertBreakStatement(): TypedBreakStatement {
        return {
            kind: "break"
        };
    }

    /**
     * Converts a continue statement.
     *
     * @returns The TypedContinueStatement representation
     */
    protected convertContinueStatement(): TypedContinueStatement {
        return {
            kind: "continue"
        };
    }
}

/**
 * Configuration for statements
 */
export class StatementConfig {
    /**
     * The name for the BaseStatement type.
     */
    readonly baseStatementTypeName: string;

    /**
     * The name for the IfStatement type.
     */
    readonly ifStatementTypeName: string;

    /**
     * The name for the IfStatement rule.
     */
    readonly ifStatementRuleName: string;

    /**
     * The name for the ElseIfClause type.
     */
    readonly elseIfClauseTypeName: string;

    /**
     * The name for the ElseIfClause rule.
     */
    readonly elseIfClauseRuleName: string;

    /**
     * The name for the WhileStatement type.
     */
    readonly whileStatementTypeName: string;

    /**
     * The name for the WhileStatement rule.
     */
    readonly whileStatementRuleName: string;

    /**
     * The name for the DoWhileStatement type.
     */
    readonly doWhileStatementTypeName: string;

    /**
     * The name for the DoWhileStatement rule.
     */
    readonly doWhileStatementRuleName: string;

    /**
     * The name for the ForStatement type.
     */
    readonly forStatementTypeName: string;

    /**
     * The name for the ForStatement rule.
     */
    readonly forStatementRuleName: string;

    /**
     * The name for the ForStatementVariableDeclaration type.
     */
    readonly forStatementVariableDeclarationTypeName: string;

    /**
     * The name for the ForStatementVariableDeclaration rule.
     */
    readonly forStatementVariableDeclarationRuleName: string;

    /**
     * The name for the VariableDeclarationStatement type.
     */
    readonly variableDeclarationStatementTypeName: string;

    /**
     * The name for the VariableDeclarationStatement rule.
     */
    readonly variableDeclarationStatementRuleName: string;

    /**
     * The name for the AssignmentStatement type.
     */
    readonly assignmentStatementTypeName: string;

    /**
     * The name for the AssignmentStatement rule.
     */
    readonly assignmentStatementRuleName: string;

    /**
     * The name for the ExpressionStatement type.
     */
    readonly expressionStatementTypeName: string;

    /**
     * The name for the ExpressionStatement rule.
     */
    readonly expressionStatementRuleName: string;

    /**
     * The name for the Statement rule.
     */
    readonly statementRuleName: string;

    /**
     * The name for the StatementsScope type.
     */
    readonly statementsScopeTypeName: string;

    /**
     * The name for the StatementsScope rule.
     */
    readonly statementsScopeRuleName: string;

    /**
     * Creates a new StatementConfig.
     * @param prefix Prefix for naming generated rules and types.
     */
    constructor(readonly prefix: string) {
        this.baseStatementTypeName = prefix + "BaseStatement";
        this.ifStatementTypeName = prefix + "IfStatement";
        this.ifStatementRuleName = prefix + "IfStatementRule";
        this.elseIfClauseTypeName = prefix + "ElseIfClause";
        this.elseIfClauseRuleName = prefix + "ElseIfClauseRule";
        this.whileStatementTypeName = prefix + "WhileStatement";
        this.whileStatementRuleName = prefix + "WhileStatementRule";
        this.doWhileStatementTypeName = prefix + "DoWhileStatement";
        this.doWhileStatementRuleName = prefix + "DoWhileStatementRule";
        this.forStatementTypeName = prefix + "ForStatement";
        this.forStatementRuleName = prefix + "ForStatementRule";
        this.forStatementVariableDeclarationTypeName = prefix + "ForStatementVariableDeclaration";
        this.forStatementVariableDeclarationRuleName = prefix + "ForStatementVariableDeclarationRule";
        this.variableDeclarationStatementTypeName = prefix + "VariableDeclarationStatement";
        this.variableDeclarationStatementRuleName = prefix + "VariableDeclarationStatementRule";
        this.assignmentStatementTypeName = prefix + "AssignmentStatement";
        this.assignmentStatementRuleName = prefix + "AssignmentStatementRule";
        this.expressionStatementTypeName = prefix + "ExpressionStatement";
        this.expressionStatementRuleName = prefix + "ExpressionStatementRule";
        this.statementRuleName = prefix + "StatementRule";
        this.statementsScopeTypeName = prefix + "StatementsScope";
        this.statementsScopeRuleName = prefix + "StatementsScopeRule";
    }
}

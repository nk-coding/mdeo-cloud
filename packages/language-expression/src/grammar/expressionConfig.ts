/**
 * Configuration for expressions
 */
export class ExpressionConfig {
    /**
     * The name for the BaseExpression type.
     */
    readonly baseExpressionTypeName: string;

    /**
     * The name for the PrimaryExpression rule.
     */
    readonly primaryExpressionRuleName: string;

    /**
     * The name for the UnaryExpression type.
     */
    readonly unaryExpressionTypeName: string;

    /**
     * The name for the UnaryExpression rule.
     */
    readonly unaryExpressionRuleName: string;

    /**
     * The name for the BinaryExpression type.
     */
    readonly binaryExpressionTypeName: string;

    /**
     * The name for the BinaryExpression rule.
     */
    readonly binaryExpressionRuleName: string;

    /**
     * The name for the TernaryExpression type.
     */
    readonly ternaryExpressionTypeName: string;

    /**
     * The name for the TernaryExpression rule.
     */
    readonly ternaryExpressionRuleName: string;

    /**
     * The name for the CallExpression type.
     */
    readonly callExpressionTypeName: string;

    /**
     * The name for the CallExpression rule.
     */
    readonly callExpressionRuleName: string;

    /**
     * The name for the CallExpressionGenericArgs type.
     */
    readonly callExpressionGenericArgsTypeName: string;

    /**
     * The name for the CallExpressionGenericArgs rule.
     */
    readonly callExpressionGenericArgsRuleName: string;

    /**
     * The name for the MemberAccessExpression type.
     */
    readonly memberAccessExpressionTypeName: string;

    /**
     * The name for the MemberAccessExpression rule.
     */
    readonly memberAccessExpressionRuleName: string;

    /**
     * The name for the IdentifierExpression type.
     */
    readonly identifierExpressionTypeName: string;

    /**
     * The name for the IdentifierExpression rule.
     */
    readonly identifierExpressionRuleName: string;

    /**
     * The name for the StringLiteralExpression type.
     */
    readonly stringLiteralExpressionTypeName: string;

    /**
     * The name for the StringLiteralExpression rule.
     */
    readonly stringLiteralExpressionRuleName: string;

    /**
     * The name for the IntLiteralExpression type.
     */
    readonly intLiteralExpressionTypeName: string;

    /**
     * The name for the IntLiteralExpression rule.
     */
    readonly intLiteralExpressionRuleName: string;

    /**
     * The name for the LongLiteralExpression type.
     */
    readonly longLiteralExpressionTypeName: string;

    /**
     * The name for the LongLiteralExpression rule.
     */
    readonly longLiteralExpressionRuleName: string;

    /**
     * The name for the FloatLiteralExpression type.
     */
    readonly floatLiteralExpressionTypeName: string;

    /**
     * The name for the FloatLiteralExpression rule.
     */
    readonly floatLiteralExpressionRuleName: string;

    /**
     * The name for the DoubleLiteralExpression type.
     */
    readonly doubleLiteralExpressionTypeName: string;

    /**
     * The name for the DoubleLiteralExpression rule.
     */
    readonly doubleLiteralExpressionRuleName: string;

    /**
     * The name for the BooleanLiteralExpression type.
     */
    readonly booleanLiteralExpressionTypeName: string;

    /**
     * The name for the BooleanLiteralExpression rule.
     */
    readonly booleanLiteralExpressionRuleName: string;

    /**
     * The name for the NullLiteralExpression type.
     */
    readonly nullLiteralExpressionTypeName: string;

    /**
     * The name for the BracketedExpression type.
     */
    readonly bracketedExpressionTypeName: string;

    /**
     * The name for the BracketedExpression rule.
     */
    readonly bracketedExpressionRuleName: string;

    /**
     * The name for the Expression rule.
     */
    readonly expressionRuleName: string;

    /**
     * The name for the AssignableExpression type.
     */
    readonly assignableExpressionTypeName: string;

    /**
     * The name for the AssignableExpression rule.
     */
    readonly assignableExpressionRuleName: string;

    /**
     * Creates a new ExpressionConfig.
     * @param prefix Prefix for naming generated rules and types.
     */
    constructor(readonly prefix: string) {
        this.baseExpressionTypeName = prefix + "PrimaryExpression";
        this.primaryExpressionRuleName = prefix + "PrimaryExpressionRule";
        this.unaryExpressionTypeName = prefix + "UnaryExpression";
        this.unaryExpressionRuleName = prefix + "UnaryExpressionRule";
        this.binaryExpressionTypeName = prefix + "BinaryExpression";
        this.binaryExpressionRuleName = prefix + "BinaryExpression";
        this.ternaryExpressionTypeName = prefix + "TernaryExpression";
        this.ternaryExpressionRuleName = prefix + "TernaryExpressionRule";
        this.callExpressionTypeName = prefix + "CallExpression";
        this.callExpressionRuleName = prefix + "CallExpressionRule";
        this.callExpressionGenericArgsTypeName = prefix + "CallExpressionGenericArgs";
        this.callExpressionGenericArgsRuleName = prefix + "CallExpressionGenericArgsRule";
        this.memberAccessExpressionTypeName = prefix + "MemberAccessExpression";
        this.memberAccessExpressionRuleName = prefix + "MemberAccessExpressionRule";
        this.identifierExpressionTypeName = prefix + "IdentifierExpression";
        this.identifierExpressionRuleName = prefix + "IdentifierExpressionRule";
        this.stringLiteralExpressionTypeName = prefix + "StringLiteralExpression";
        this.stringLiteralExpressionRuleName = prefix + "StringLiteralExpressionRule";
        this.intLiteralExpressionTypeName = prefix + "IntLiteralExpression";
        this.intLiteralExpressionRuleName = prefix + "IntLiteralExpressionRule";
        this.longLiteralExpressionTypeName = prefix + "LongLiteralExpression";
        this.longLiteralExpressionRuleName = prefix + "LongLiteralExpressionRule";
        this.floatLiteralExpressionTypeName = prefix + "FloatLiteralExpression";
        this.floatLiteralExpressionRuleName = prefix + "FloatLiteralExpressionRule";
        this.doubleLiteralExpressionTypeName = prefix + "DoubleLiteralExpression";
        this.doubleLiteralExpressionRuleName = prefix + "DoubleLiteralExpressionRule";
        this.booleanLiteralExpressionTypeName = prefix + "BooleanLiteralExpression";
        this.booleanLiteralExpressionRuleName = prefix + "BooleanLiteralExpressionRule";
        this.nullLiteralExpressionTypeName = prefix + "NullLiteralExpression";
        this.bracketedExpressionTypeName = prefix + "BracketedExpression";
        this.bracketedExpressionRuleName = prefix + "BracketedExpressionRule";
        this.expressionRuleName = prefix + "ExpressionRule";
        this.assignableExpressionTypeName = prefix + "AssignableExpression";
        this.assignableExpressionRuleName = prefix + "AssignableExpressionRule";
    }
}

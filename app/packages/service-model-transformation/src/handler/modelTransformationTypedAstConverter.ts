import type { AstReflection } from "@mdeo/language-common";
import { ExpressionTypedAstConverter, type TypedExpression } from "@mdeo/language-expression";
import {
    expressionTypes,
    type ModelTransformationTypirServices,
    type ModelTransformationType,
    type TypedAst,
    type TypedTransformationStatement,
    statementTypes,
    type MatchStatementType,
    type TypedMatchStatement,
    type IfMatchStatementType,
    type TypedIfMatchStatement,
    type WhileMatchStatementType,
    type TypedWhileMatchStatement,
    type UntilMatchStatementType,
    type TypedUntilMatchStatement,
    type ForMatchStatementType,
    type TypedForMatchStatement,
    type IfExpressionStatementType,
    type TypedIfExpressionStatement,
    type ElseIfBranchType,
    type TypedElseIfBranch,
    type WhileExpressionStatementType,
    type TypedWhileExpressionStatement,
    type StopStatementType,
    type TypedStopStatement,
    type PatternType,
    type TypedPattern,
    type TypedPatternVariableElement,
    type TypedPatternObjectInstanceElement,
    type TypedPatternLinkElement,
    type TypedPatternWhereClauseElement,
    type PatternVariableType,
    type TypedPatternVariable,
    type PatternObjectInstanceType,
    type TypedPatternObjectInstance,
    type PatternLinkType,
    type TypedPatternLink,
    type WhereClauseType,
    type TypedWhereClause,
    LambdaExpression,
    type LambdaExpressionType,
    type TypedLambdaExpression
} from "@mdeo/language-model-transformation";

import type { AstNode } from "langium";

/**
 * Model Transformation-specific extension of ExpressionTypedAstConverter.
 * Handles transformation-specific features like patterns, match statements, and lambda expressions.
 * Does not use the base statement types as transformation statements are conceptually different.
 */
export class ModelTransformationTypedAstConverter extends ExpressionTypedAstConverter {
    /**
     * Expression type identifiers for checking instance types.
     */
    protected expressionTypes = expressionTypes;

    /**
     * Creates a new ModelTransformationTypedAstConverter.
     *
     * @param typir The Typir services for type inference
     * @param reflection The AST reflection for type checking
     */
    constructor(
        protected override readonly typir: ModelTransformationTypirServices,
        reflection: AstReflection
    ) {
        super(typir, reflection);
    }

    /**
     * Converts a ModelTransformation AST node to a TypedAst.
     *
     * @param transformation The ModelTransformation AST node
     * @returns The TypedAst representation
     */
    convertModelTransformation(transformation: ModelTransformationType): TypedAst {
        const statements: TypedTransformationStatement[] = transformation.statements.map((stmt) =>
            this.convertTransformationStatement(stmt)
        );

        return {
            types: this.types,
            metamodelUri: transformation.import.file,
            statements
        };
    }

    /**
     * Converts a transformation statement.
     *
     * @param stmt The statement AST node from the parsed grammar
     * @returns The TypedTransformationStatement representation
     * @throws Error if statement type is unknown or not supported
     */
    private convertTransformationStatement(stmt: AstNode): TypedTransformationStatement {
        if (this.reflection.isInstance(stmt, statementTypes.matchStatementType)) {
            return this.convertMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.ifMatchStatementType)) {
            return this.convertIfMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.whileMatchStatementType)) {
            return this.convertWhileMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.untilMatchStatementType)) {
            return this.convertUntilMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.forMatchStatementType)) {
            return this.convertForMatchStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.ifExpressionStatementType)) {
            return this.convertIfExpressionStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.whileExpressionStatementType)) {
            return this.convertWhileExpressionStatement(stmt);
        }
        if (this.reflection.isInstance(stmt, statementTypes.stopStatementType)) {
            return this.convertStopStatement(stmt);
        }
        throw new Error(`Unknown transformation statement type: ${stmt.$type}`);
    }

    /**
     * Converts a match statement.
     *
     * @param stmt The MatchStatement AST node
     * @returns The TypedMatchStatement representation
     */
    private convertMatchStatement(stmt: MatchStatementType): TypedMatchStatement {
        return {
            kind: "match",
            pattern: this.convertPattern(stmt.pattern)
        };
    }

    /**
     * Converts an if-match statement.
     *
     * @param stmt The IfMatchStatement AST node
     * @returns The TypedIfMatchStatement representation
     */
    private convertIfMatchStatement(stmt: IfMatchStatementType): TypedIfMatchStatement {
        return {
            kind: "ifMatch",
            pattern: this.convertPattern(stmt.pattern),
            thenBlock: stmt.thenBlock.statements.map((s) => this.convertTransformationStatement(s)),
            elseBlock: stmt.elseBlock
                ? stmt.elseBlock.statements.map((s) => this.convertTransformationStatement(s))
                : undefined
        };
    }

    /**
     * Converts a while-match statement.
     *
     * @param stmt The WhileMatchStatement AST node
     * @returns The TypedWhileMatchStatement representation
     */
    private convertWhileMatchStatement(stmt: WhileMatchStatementType): TypedWhileMatchStatement {
        return {
            kind: "whileMatch",
            pattern: this.convertPattern(stmt.pattern),
            doBlock: stmt.doBlock.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts an until-match statement.
     *
     * @param stmt The UntilMatchStatement AST node
     * @returns The TypedUntilMatchStatement representation
     */
    private convertUntilMatchStatement(stmt: UntilMatchStatementType): TypedUntilMatchStatement {
        return {
            kind: "untilMatch",
            pattern: this.convertPattern(stmt.pattern),
            doBlock: stmt.doBlock.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts a for-match statement.
     *
     * @param stmt The ForMatchStatement AST node
     * @returns The TypedForMatchStatement representation
     */
    private convertForMatchStatement(stmt: ForMatchStatementType): TypedForMatchStatement {
        return {
            kind: "forMatch",
            pattern: this.convertPattern(stmt.pattern),
            doBlock: stmt.doBlock.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts an if-expression statement.
     *
     * @param stmt The IfExpressionStatement AST node
     * @returns The TypedIfExpressionStatement representation
     */
    private convertIfExpressionStatement(stmt: IfExpressionStatementType): TypedIfExpressionStatement {
        return {
            kind: "ifExpression",
            condition: this.convertExpression(stmt.condition),
            thenBlock: stmt.thenBlock.statements.map((s) => this.convertTransformationStatement(s)),
            elseIfBranches: stmt.elseIfBranches.map((branch) => this.convertElseIfBranch(branch)),
            elseBlock: stmt.elseBlock
                ? stmt.elseBlock.statements.map((s) => this.convertTransformationStatement(s))
                : undefined
        };
    }

    /**
     * Converts an else-if branch.
     *
     * @param branch The ElseIfBranch AST node
     * @returns The TypedElseIfBranch representation
     */
    private convertElseIfBranch(branch: ElseIfBranchType): TypedElseIfBranch {
        return {
            condition: this.convertExpression(branch.condition),
            block: branch.block.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts a while-expression statement.
     *
     * @param stmt The WhileExpressionStatement AST node
     * @returns The TypedWhileExpressionStatement representation
     */
    private convertWhileExpressionStatement(stmt: WhileExpressionStatementType): TypedWhileExpressionStatement {
        return {
            kind: "whileExpression",
            condition: this.convertExpression(stmt.condition),
            block: stmt.block.statements.map((s) => this.convertTransformationStatement(s))
        };
    }

    /**
     * Converts a stop statement.
     *
     * @param stmt The StopStatement AST node
     * @returns The TypedStopStatement representation
     */
    private convertStopStatement(stmt: StopStatementType): TypedStopStatement {
        return {
            kind: "stop",
            keyword: stmt.keyword
        };
    }

    /**
     * Converts a pattern.
     *
     * @param pattern The Pattern AST node
     * @returns The TypedPattern representation
     */
    private convertPattern(pattern: PatternType): TypedPattern {
        const elements: (
            | TypedPatternVariableElement
            | TypedPatternObjectInstanceElement
            | TypedPatternLinkElement
            | TypedPatternWhereClauseElement
        )[] = [];

        for (const element of pattern.elements) {
            if (this.reflection.isInstance(element, statementTypes.patternVariableType)) {
                elements.push({
                    kind: "variable",
                    variable: this.convertPatternVariable(element)
                });
            } else if (this.reflection.isInstance(element, statementTypes.patternObjectInstanceType)) {
                elements.push({
                    kind: "objectInstance",
                    objectInstance: this.convertPatternObjectInstance(element)
                });
            } else if (this.reflection.isInstance(element, statementTypes.patternLinkType)) {
                elements.push({
                    kind: "link",
                    link: this.convertPatternLink(element)
                });
            } else if (this.reflection.isInstance(element, statementTypes.whereClauseType)) {
                elements.push({
                    kind: "whereClause",
                    whereClause: this.convertWhereClause(element)
                });
            }
        }

        return { elements };
    }

    /**
     * Converts a pattern variable.
     *
     * @param variable The PatternVariable AST node
     * @returns The TypedPatternVariable representation
     */
    private convertPatternVariable(variable: PatternVariableType): TypedPatternVariable {
        return {
            name: variable.name,
            type: this.getTypeIndex(variable.type)
        };
    }

    /**
     * Converts a pattern object instance.
     *
     * @param obj The PatternObjectInstance AST node
     * @returns The TypedPatternObjectInstance representation
     */
    private convertPatternObjectInstance(obj: PatternObjectInstanceType): TypedPatternObjectInstance {
        return {
            modifier: obj.modifier?.modifier,
            name: obj.name,
            className: obj.class?.$refText ?? "",
            properties: obj.properties.map((prop) => ({
                propertyName: prop.name?.$refText ?? "",
                operator: prop.operator,
                value: this.convertExpression(prop.value)
            }))
        };
    }

    /**
     * Converts a pattern link.
     *
     * @param link The PatternLink AST node
     * @returns The TypedPatternLink representation
     */
    private convertPatternLink(link: PatternLinkType): TypedPatternLink {
        return {
            modifier: link.modifier?.modifier,
            source: {
                objectName: link.source.object?.$refText ?? "",
                propertyName: link.source.property?.$refText
            },
            target: {
                objectName: link.target.object?.$refText ?? "",
                propertyName: link.target.property?.$refText
            }
        };
    }

    /**
     * Converts a where clause.
     *
     * @param clause The WhereClause AST node
     * @returns The TypedWhereClause representation
     */
    private convertWhereClause(clause: WhereClauseType): TypedWhereClause {
        return {
            expression: this.convertExpression(clause.expression)
        };
    }

    /**
     * Handles expression conversion, including lambda expressions.
     *
     * @param expr The expression AST node
     * @returns The TypedExpression representation
     * @throws Error if expression type is unsupported
     */
    protected override convertAdditionalExpression(expr: AstNode): TypedExpression {
        if (this.reflection.isInstance(expr, LambdaExpression)) {
            return this.convertLambdaExpression(expr);
        }
        return super.convertAdditionalExpression(expr);
    }

    /**
     * Converts a lambda expression.
     *
     * @param expr The lambda expression AST node
     * @returns The TypedLambdaExpression representation
     */
    private convertLambdaExpression(expr: LambdaExpressionType): TypedLambdaExpression {
        const parameters = expr.parameterList.parameters.map((param) => param.name);

        return {
            kind: "lambda",
            evalType: this.getTypeIndex(expr),
            parameters,
            body: this.convertExpression(expr.expression)
        };
    }
}

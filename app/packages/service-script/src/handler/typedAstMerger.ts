import type { ReturnType } from "@mdeo/language-expression";
import {
    TypedExpressionKind,
    TypedStatementKind,
    type TypedAssignmentStatement,
    type TypedBinaryExpression,
    type TypedBooleanLiteralExpression,
    type TypedBreakStatement,
    type TypedCallableBody,
    type TypedContinueStatement,
    type TypedDoubleLiteralExpression,
    type TypedElseIfClause,
    type TypedExpression,
    type TypedExpressionCallExpression,
    type TypedExpressionStatement,
    type TypedExtensionCallArgument,
    type TypedExtensionCallExpression,
    type TypedFloatLiteralExpression,
    type TypedForStatement,
    type TypedFunction,
    type TypedFunctionCallExpression,
    type TypedIdentifierExpression,
    type TypedIfStatement,
    type TypedIntLiteralExpression,
    type TypedLambdaExpression,
    type TypedLongLiteralExpression,
    type TypedMemberAccessExpression,
    type TypedMemberCallExpression,
    type TypedNullLiteralExpression,
    type TypedParameter,
    type TypedReturnStatement,
    type TypedStatement,
    type TypedStringLiteralExpression,
    type TypedTernaryExpression,
    type TypedUnaryExpression,
    type TypedVariableDeclarationStatement,
    type TypedWhileStatement
} from "@mdeo/language-script";

/**
 * Helper class for merging multiple typed ASTs with different type arrays
 * into a single global type array with remapped indices.
 */
export class TypedAstMerger {
    /**
     * The global merged types array.
     */
    private readonly globalTypes: ReturnType[] = [];

    /**
     * Map from serialized type to its index in the global types array.
     */
    private readonly typeToIndex = new Map<string, number>();

    /**
     * Map from type array reference to its index mapping (old index -> new index).
     */
    private readonly typesArrayMappings = new Map<ReturnType[], Map<number, number>>();

    /**
     * Gets the merged global types array.
     *
     * @returns The array of all unique types
     */
    getGlobalTypes(): ReturnType[] {
        return this.globalTypes;
    }

    /**
     * Indexes a type array and creates a mapping from old indices to new global indices.
     * Uses reference comparison to avoid re-indexing the same array.
     *
     * @param typesArray The type array to index
     * @returns A map from old index to new global index
     */
    indexTypesArray(typesArray: ReturnType[]): Map<number, number> {
        const existingMapping = this.typesArrayMappings.get(typesArray);
        if (existingMapping !== undefined) {
            return existingMapping;
        }

        const mapping = new Map<number, number>();
        for (let oldIndex = 0; oldIndex < typesArray.length; oldIndex++) {
            const type = typesArray[oldIndex];
            const newIndex = this.addTypeToGlobal(type);
            mapping.set(oldIndex, newIndex);
        }

        this.typesArrayMappings.set(typesArray, mapping);
        return mapping;
    }

    /**
     * Adds a type to the global types array if it doesn't already exist.
     *
     * @param type The type to add
     * @returns The index of the type in the global array
     */
    addTypeToGlobal(type: ReturnType): number {
        const typeKey = JSON.stringify(type);
        const existingIndex = this.typeToIndex.get(typeKey);
        if (existingIndex !== undefined) {
            return existingIndex;
        }

        const newIndex = this.globalTypes.length;
        this.globalTypes.push(type);
        this.typeToIndex.set(typeKey, newIndex);
        return newIndex;
    }

    /**
     * Remaps a function to use the global type indices.
     *
     * @param func The function to remap
     * @param typesArray The original types array used by this function
     * @returns A new function with remapped type indices
     */
    remapFunction(func: TypedFunction, typesArray: ReturnType[]): TypedFunction {
        const mapping = this.indexTypesArray(typesArray);

        return {
            name: func.name,
            parameters: func.parameters.map((p) => this.remapParameter(p, mapping)),
            returnType: mapping.get(func.returnType)!,
            body: this.remapCallableBody(func.body, mapping)
        };
    }

    /**
     * Remaps a parameter to use global type indices.
     *
     * @param param The parameter to remap
     * @param mapping The index mapping
     * @returns A new parameter with remapped type index
     */
    private remapParameter(param: TypedParameter, mapping: Map<number, number>): TypedParameter {
        return {
            name: param.name,
            type: mapping.get(param.type)!
        };
    }

    /**
     * Remaps a callable body to use global type indices.
     *
     * @param body The callable body to remap
     * @param mapping The index mapping
     * @returns A new callable body with remapped type indices
     */
    private remapCallableBody(body: TypedCallableBody, mapping: Map<number, number>): TypedCallableBody {
        return {
            body: body.body.map((stmt) => this.remapStatement(stmt, mapping))
        };
    }

    /**
     * Remaps a statement to use global type indices.
     *
     * @param stmt The statement to remap
     * @param mapping The index mapping
     * @returns A new statement with remapped type indices
     */
    private remapStatement(stmt: TypedStatement, mapping: Map<number, number>): TypedStatement {
        switch (stmt.kind) {
            case TypedStatementKind.If:
                return this.remapIfStatement(stmt as TypedIfStatement, mapping);
            case TypedStatementKind.While:
                return this.remapWhileStatement(stmt as TypedWhileStatement, mapping);
            case TypedStatementKind.For:
                return this.remapForStatement(stmt as TypedForStatement, mapping);
            case TypedStatementKind.VariableDeclaration:
                return this.remapVariableDeclarationStatement(stmt as TypedVariableDeclarationStatement, mapping);
            case TypedStatementKind.Assignment:
                return this.remapAssignmentStatement(stmt as TypedAssignmentStatement, mapping);
            case TypedStatementKind.Expression:
                return this.remapExpressionStatement(stmt as TypedExpressionStatement, mapping);
            case TypedStatementKind.Break:
                return this.remapBreakStatement(stmt as TypedBreakStatement);
            case TypedStatementKind.Continue:
                return this.remapContinueStatement(stmt as TypedContinueStatement);
            case TypedStatementKind.Return:
                return this.remapReturnStatement(stmt as TypedReturnStatement, mapping);
        }
        throw new Error(`Unhandled statement kind: ${stmt.kind}`);
    }

    /**
     * Remaps an if statement.
     *
     * @param stmt The if statement to remap
     * @param mapping The index mapping
     * @returns A new if statement with remapped type indices
     */
    private remapIfStatement(stmt: TypedIfStatement, mapping: Map<number, number>): TypedIfStatement {
        return {
            kind: TypedStatementKind.If,
            condition: this.remapExpression(stmt.condition, mapping),
            thenBlock: stmt.thenBlock.map((s) => this.remapStatement(s, mapping)),
            elseIfs: stmt.elseIfs.map((ei) => this.remapElseIfClause(ei, mapping)),
            elseBlock: stmt.elseBlock?.map((s) => this.remapStatement(s, mapping))
        };
    }

    /**
     * Remaps an else-if clause.
     *
     * @param clause The else-if clause to remap
     * @param mapping The index mapping
     * @returns A new else-if clause with remapped type indices
     */
    private remapElseIfClause(clause: TypedElseIfClause, mapping: Map<number, number>): TypedElseIfClause {
        return {
            condition: this.remapExpression(clause.condition, mapping),
            thenBlock: clause.thenBlock.map((s) => this.remapStatement(s, mapping))
        };
    }

    /**
     * Remaps a while statement.
     *
     * @param stmt The while statement to remap
     * @param mapping The index mapping
     * @returns A new while statement with remapped type indices
     */
    private remapWhileStatement(stmt: TypedWhileStatement, mapping: Map<number, number>): TypedWhileStatement {
        return {
            kind: TypedStatementKind.While,
            condition: this.remapExpression(stmt.condition, mapping),
            body: stmt.body.map((s) => this.remapStatement(s, mapping))
        };
    }

    /**
     * Remaps a for statement.
     *
     * @param stmt The for statement to remap
     * @param mapping The index mapping
     * @returns A new for statement with remapped type indices
     */
    private remapForStatement(stmt: TypedForStatement, mapping: Map<number, number>): TypedForStatement {
        return {
            kind: TypedStatementKind.For,
            variableName: stmt.variableName,
            variableType: mapping.get(stmt.variableType)!,
            iterable: this.remapExpression(stmt.iterable, mapping),
            body: stmt.body.map((s) => this.remapStatement(s, mapping))
        };
    }

    /**
     * Remaps a variable declaration statement.
     *
     * @param stmt The variable declaration statement to remap
     * @param mapping The index mapping
     * @returns A new variable declaration statement with remapped type indices
     */
    private remapVariableDeclarationStatement(
        stmt: TypedVariableDeclarationStatement,
        mapping: Map<number, number>
    ): TypedVariableDeclarationStatement {
        return {
            kind: TypedStatementKind.VariableDeclaration,
            name: stmt.name,
            type: mapping.get(stmt.type)!,
            initialValue: stmt.initialValue ? this.remapExpression(stmt.initialValue, mapping) : undefined
        };
    }

    /**
     * Remaps an assignment statement.
     *
     * @param stmt The assignment statement to remap
     * @param mapping The index mapping
     * @returns A new assignment statement with remapped type indices
     */
    private remapAssignmentStatement(
        stmt: TypedAssignmentStatement,
        mapping: Map<number, number>
    ): TypedAssignmentStatement {
        return {
            kind: TypedStatementKind.Assignment,
            left: this.remapExpression(stmt.left, mapping) as TypedIdentifierExpression | TypedMemberAccessExpression,
            right: this.remapExpression(stmt.right, mapping)
        };
    }

    /**
     * Remaps an expression statement.
     *
     * @param stmt The expression statement to remap
     * @param mapping The index mapping
     * @returns A new expression statement with remapped type indices
     */
    private remapExpressionStatement(
        stmt: TypedExpressionStatement,
        mapping: Map<number, number>
    ): TypedExpressionStatement {
        return {
            kind: TypedStatementKind.Expression,
            expression: this.remapExpression(stmt.expression, mapping)
        };
    }

    /**
     * Remaps a break statement.
     *
     * @param stmt The break statement to remap
     * @returns The break statement unchanged
     */
    private remapBreakStatement(stmt: TypedBreakStatement): TypedBreakStatement {
        return stmt;
    }

    /**
     * Remaps a continue statement.
     *
     * @param stmt The continue statement to remap
     * @returns The continue statement unchanged
     */
    private remapContinueStatement(stmt: TypedContinueStatement): TypedContinueStatement {
        return stmt;
    }

    /**
     * Remaps a return statement.
     *
     * @param stmt The return statement to remap
     * @param mapping The index mapping
     * @returns A new return statement with remapped type indices
     */
    private remapReturnStatement(stmt: TypedReturnStatement, mapping: Map<number, number>): TypedReturnStatement {
        return {
            kind: TypedStatementKind.Return,
            value: stmt.value ? this.remapExpression(stmt.value, mapping) : undefined
        };
    }

    /**
     * Remaps an expression to use global type indices.
     *
     * @param expr The expression to remap
     * @param mapping The index mapping
     * @returns A new expression with remapped type indices
     */
    private remapExpression(expr: TypedExpression, mapping: Map<number, number>): TypedExpression {
        switch (expr.kind) {
            case TypedExpressionKind.Unary:
                return this.remapUnaryExpression(expr as TypedUnaryExpression, mapping);
            case TypedExpressionKind.Binary:
                return this.remapBinaryExpression(expr as TypedBinaryExpression, mapping);
            case TypedExpressionKind.Ternary:
                return this.remapTernaryExpression(expr as TypedTernaryExpression, mapping);
            case TypedExpressionKind.ExpressionCall:
                return this.remapExpressionCallExpression(expr as TypedExpressionCallExpression, mapping);
            case TypedExpressionKind.FunctionCall:
                return this.remapFunctionCallExpression(expr as TypedFunctionCallExpression, mapping);
            case TypedExpressionKind.MemberCall:
                return this.remapMemberCallExpression(expr as TypedMemberCallExpression, mapping);
            case TypedExpressionKind.ExtensionCall:
                return this.remapExtensionCallExpression(expr as TypedExtensionCallExpression, mapping);
            case TypedExpressionKind.MemberAccess:
                return this.remapMemberAccessExpression(expr as TypedMemberAccessExpression, mapping);
            case TypedExpressionKind.Identifier:
                return this.remapIdentifierExpression(expr as TypedIdentifierExpression, mapping);
            case TypedExpressionKind.StringLiteral:
                return this.remapStringLiteralExpression(expr as TypedStringLiteralExpression, mapping);
            case TypedExpressionKind.IntLiteral:
                return this.remapIntLiteralExpression(expr as TypedIntLiteralExpression, mapping);
            case TypedExpressionKind.LongLiteral:
                return this.remapLongLiteralExpression(expr as TypedLongLiteralExpression, mapping);
            case TypedExpressionKind.FloatLiteral:
                return this.remapFloatLiteralExpression(expr as TypedFloatLiteralExpression, mapping);
            case TypedExpressionKind.DoubleLiteral:
                return this.remapDoubleLiteralExpression(expr as TypedDoubleLiteralExpression, mapping);
            case TypedExpressionKind.BooleanLiteral:
                return this.remapBooleanLiteralExpression(expr as TypedBooleanLiteralExpression, mapping);
            case TypedExpressionKind.NullLiteral:
                return this.remapNullLiteralExpression(expr as TypedNullLiteralExpression, mapping);
            case TypedExpressionKind.Lambda:
                return this.remapLambdaExpression(expr as TypedLambdaExpression, mapping);
        }
        throw new Error(`Unhandled expression kind: ${expr.kind}`);
    }

    /**
     * Remaps a unary expression.
     *
     * @param expr The unary expression to remap
     * @param mapping The index mapping
     * @returns A new unary expression with remapped type indices
     */
    private remapUnaryExpression(expr: TypedUnaryExpression, mapping: Map<number, number>): TypedUnaryExpression {
        return {
            kind: TypedExpressionKind.Unary,
            evalType: mapping.get(expr.evalType)!,
            operator: expr.operator,
            expression: this.remapExpression(expr.expression, mapping)
        };
    }

    /**
     * Remaps a binary expression.
     *
     * @param expr The binary expression to remap
     * @param mapping The index mapping
     * @returns A new binary expression with remapped type indices
     */
    private remapBinaryExpression(expr: TypedBinaryExpression, mapping: Map<number, number>): TypedBinaryExpression {
        return {
            kind: TypedExpressionKind.Binary,
            evalType: mapping.get(expr.evalType)!,
            operator: expr.operator,
            left: this.remapExpression(expr.left, mapping),
            right: this.remapExpression(expr.right, mapping)
        };
    }

    /**
     * Remaps a ternary expression.
     *
     * @param expr The ternary expression to remap
     * @param mapping The index mapping
     * @returns A new ternary expression with remapped type indices
     */
    private remapTernaryExpression(expr: TypedTernaryExpression, mapping: Map<number, number>): TypedTernaryExpression {
        return {
            kind: TypedExpressionKind.Ternary,
            evalType: mapping.get(expr.evalType)!,
            condition: this.remapExpression(expr.condition, mapping),
            trueExpression: this.remapExpression(expr.trueExpression, mapping),
            falseExpression: this.remapExpression(expr.falseExpression, mapping)
        };
    }

    /**
     * Remaps an expression call expression.
     *
     * @param expr The expression call expression to remap
     * @param mapping The index mapping
     * @returns A new expression call expression with remapped type indices
     */
    private remapExpressionCallExpression(
        expr: TypedExpressionCallExpression,
        mapping: Map<number, number>
    ): TypedExpressionCallExpression {
        return {
            kind: TypedExpressionKind.ExpressionCall,
            evalType: mapping.get(expr.evalType)!,
            expression: this.remapExpression(expr.expression, mapping),
            arguments: expr.arguments.map((arg) => this.remapExpression(arg, mapping))
        };
    }

    /**
     * Remaps a function call expression.
     *
     * @param expr The function call expression to remap
     * @param mapping The index mapping
     * @returns A new function call expression with remapped type indices
     */
    private remapFunctionCallExpression(
        expr: TypedFunctionCallExpression,
        mapping: Map<number, number>
    ): TypedFunctionCallExpression {
        return {
            kind: TypedExpressionKind.FunctionCall,
            evalType: mapping.get(expr.evalType)!,
            name: expr.name,
            overload: expr.overload,
            arguments: expr.arguments.map((arg) => this.remapExpression(arg, mapping))
        };
    }

    /**
     * Remaps a member call expression.
     *
     * @param expr The member call expression to remap
     * @param mapping The index mapping
     * @returns A new member call expression with remapped type indices
     */
    private remapMemberCallExpression(
        expr: TypedMemberCallExpression,
        mapping: Map<number, number>
    ): TypedMemberCallExpression {
        return {
            kind: TypedExpressionKind.MemberCall,
            evalType: mapping.get(expr.evalType)!,
            expression: this.remapExpression(expr.expression, mapping),
            member: expr.member,
            isNullChaining: expr.isNullChaining,
            overload: expr.overload,
            arguments: expr.arguments.map((arg) => this.remapExpression(arg, mapping))
        };
    }

    /**
     * Remaps an extension call expression.
     *
     * @param expr The extension call expression to remap
     * @param mapping The index mapping
     * @returns A new extension call expression with remapped type indices
     */
    private remapExtensionCallExpression(
        expr: TypedExtensionCallExpression,
        mapping: Map<number, number>
    ): TypedExtensionCallExpression {
        return {
            kind: TypedExpressionKind.ExtensionCall,
            evalType: mapping.get(expr.evalType)!,
            name: expr.name,
            overload: expr.overload,
            arguments: expr.arguments.map((arg) => this.remapExtensionCallArgument(arg, mapping))
        };
    }

    /**
     * Remaps an extension call argument.
     *
     * @param arg The extension call argument to remap
     * @param mapping The index mapping
     * @returns A new extension call argument with remapped type indices
     */
    private remapExtensionCallArgument(
        arg: TypedExtensionCallArgument,
        mapping: Map<number, number>
    ): TypedExtensionCallArgument {
        return {
            name: arg.name,
            value: this.remapExpression(arg.value, mapping)
        };
    }

    /**
     * Remaps a member access expression.
     *
     * @param expr The member access expression to remap
     * @param mapping The index mapping
     * @returns A new member access expression with remapped type indices
     */
    private remapMemberAccessExpression(
        expr: TypedMemberAccessExpression,
        mapping: Map<number, number>
    ): TypedMemberAccessExpression {
        return {
            kind: TypedExpressionKind.MemberAccess,
            evalType: mapping.get(expr.evalType)!,
            expression: this.remapExpression(expr.expression, mapping),
            member: expr.member,
            isNullChaining: expr.isNullChaining
        };
    }

    /**
     * Remaps an identifier expression.
     *
     * @param expr The identifier expression to remap
     * @param mapping The index mapping
     * @returns A new identifier expression with remapped type indices
     */
    private remapIdentifierExpression(
        expr: TypedIdentifierExpression,
        mapping: Map<number, number>
    ): TypedIdentifierExpression {
        return {
            kind: TypedExpressionKind.Identifier,
            evalType: mapping.get(expr.evalType)!,
            name: expr.name,
            scope: expr.scope
        };
    }

    /**
     * Remaps a string literal expression.
     *
     * @param expr The string literal expression to remap
     * @param mapping The index mapping
     * @returns A new string literal expression with remapped type indices
     */
    private remapStringLiteralExpression(
        expr: TypedStringLiteralExpression,
        mapping: Map<number, number>
    ): TypedStringLiteralExpression {
        return {
            kind: TypedExpressionKind.StringLiteral,
            evalType: mapping.get(expr.evalType)!,
            value: expr.value
        };
    }

    /**
     * Remaps an int literal expression.
     *
     * @param expr The int literal expression to remap
     * @param mapping The index mapping
     * @returns A new int literal expression with remapped type indices
     */
    private remapIntLiteralExpression(
        expr: TypedIntLiteralExpression,
        mapping: Map<number, number>
    ): TypedIntLiteralExpression {
        return {
            kind: TypedExpressionKind.IntLiteral,
            evalType: mapping.get(expr.evalType)!,
            value: expr.value
        };
    }

    /**
     * Remaps a long literal expression.
     *
     * @param expr The long literal expression to remap
     * @param mapping The index mapping
     * @returns A new long literal expression with remapped type indices
     */
    private remapLongLiteralExpression(
        expr: TypedLongLiteralExpression,
        mapping: Map<number, number>
    ): TypedLongLiteralExpression {
        return {
            kind: TypedExpressionKind.LongLiteral,
            evalType: mapping.get(expr.evalType)!,
            value: expr.value
        };
    }

    /**
     * Remaps a float literal expression.
     *
     * @param expr The float literal expression to remap
     * @param mapping The index mapping
     * @returns A new float literal expression with remapped type indices
     */
    private remapFloatLiteralExpression(
        expr: TypedFloatLiteralExpression,
        mapping: Map<number, number>
    ): TypedFloatLiteralExpression {
        return {
            kind: TypedExpressionKind.FloatLiteral,
            evalType: mapping.get(expr.evalType)!,
            value: expr.value
        };
    }

    /**
     * Remaps a double literal expression.
     *
     * @param expr The double literal expression to remap
     * @param mapping The index mapping
     * @returns A new double literal expression with remapped type indices
     */
    private remapDoubleLiteralExpression(
        expr: TypedDoubleLiteralExpression,
        mapping: Map<number, number>
    ): TypedDoubleLiteralExpression {
        return {
            kind: TypedExpressionKind.DoubleLiteral,
            evalType: mapping.get(expr.evalType)!,
            value: expr.value
        };
    }

    /**
     * Remaps a boolean literal expression.
     *
     * @param expr The boolean literal expression to remap
     * @param mapping The index mapping
     * @returns A new boolean literal expression with remapped type indices
     */
    private remapBooleanLiteralExpression(
        expr: TypedBooleanLiteralExpression,
        mapping: Map<number, number>
    ): TypedBooleanLiteralExpression {
        return {
            kind: TypedExpressionKind.BooleanLiteral,
            evalType: mapping.get(expr.evalType)!,
            value: expr.value
        };
    }

    /**
     * Remaps a null literal expression.
     *
     * @param expr The null literal expression to remap
     * @param mapping The index mapping
     * @returns A new null literal expression with remapped type indices
     */
    private remapNullLiteralExpression(
        expr: TypedNullLiteralExpression,
        mapping: Map<number, number>
    ): TypedNullLiteralExpression {
        return {
            kind: TypedExpressionKind.NullLiteral,
            evalType: mapping.get(expr.evalType)!
        };
    }

    /**
     * Remaps a lambda expression.
     *
     * @param expr The lambda expression to remap
     * @param mapping The index mapping
     * @returns A new lambda expression with remapped type indices
     */
    private remapLambdaExpression(expr: TypedLambdaExpression, mapping: Map<number, number>): TypedLambdaExpression {
        return {
            kind: TypedExpressionKind.Lambda,
            evalType: mapping.get(expr.evalType)!,
            parameters: expr.parameters,
            body: this.remapCallableBody(expr.body, mapping)
        };
    }
}

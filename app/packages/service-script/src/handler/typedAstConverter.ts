import type {
    ScriptTypirSpecifics,
    TypedExpressionCallExpression,
    TypedFunctionCallExpression,
    TypedMemberCallExpression
} from "@mdeo/language-script";
import type {
    TypedAst,
    TypedFunction,
    TypedStatement,
    TypedExpression,
    TypedParameter,
    TypedCallableBody
} from "@mdeo/language-script";
import {
    TypedStatementKind,
    TypedExpressionKind,
    type TypedIfStatement,
    type TypedWhileStatement,
    type TypedForStatement,
    type TypedVariableDeclarationStatement,
    type TypedAssignmentStatement,
    type TypedExpressionStatement,
    type TypedBreakStatement,
    type TypedContinueStatement,
    type TypedReturnStatement,
    type TypedUnaryExpression,
    type TypedBinaryExpression,
    type TypedTernaryExpression,
    type TypedMemberAccessExpression,
    type TypedIdentifierExpression,
    type TypedStringLiteralExpression,
    type TypedIntLiteralExpression,
    type TypedLongLiteralExpression,
    type TypedFloatLiteralExpression,
    type TypedDoubleLiteralExpression,
    type TypedBooleanLiteralExpression,
    type TypedNullLiteralExpression,
    type TypedLambdaExpression,
    type TypedImport
} from "@mdeo/language-script";
import type { ScriptType, FunctionType, LambdaExpressionType, FunctionParametersType } from "@mdeo/language-script";
import { LambdaExpression, ReturnStatement, statementTypes, expressionTypes } from "@mdeo/language-script";
import type {
    ExpressionTypirServices,
    IfStatementType,
    ElseIfClauseType,
    WhileStatementType,
    ForStatementType,
    VariableDeclarationStatementType,
    AssignmentStatementType,
    UnaryExpressionType,
    BinaryExpressionType,
    TernaryExpressionType,
    CallExpressionType,
    MemberAccessExpressionType,
    IdentifierExpressionType,
    StringLiteralExpressionType,
    IntLiteralExpressionType,
    LongLiteralExpressionType,
    FloatLiteralExpressionType,
    DoubleLiteralExpressionType,
    BooleanLiteralExpressionType,
    NullLiteralExpressionType,
    ReturnType,
    ValueType,
    ClassTypeRef
} from "@mdeo/language-expression";
import {
    getCallOverload,
    isCustomVoidType,
    isCustomClassType,
    isCustomLambdaType,
    isCustomFunctionType
} from "@mdeo/language-expression";
import type { AstNode } from "langium";
import type { AstReflection } from "@mdeo/language-common";
import type { Type } from "typir";

/**
 * Converts a Script AST to a TypedRoot.
 * Handles type inference, scope resolution, and AST transformation.
 */
export class TypedAstConverter {
    /**
     * Lookup map to deduplicate types in the types array.
     */
    private readonly typeLookup = new Map<Type, number>();

    /**
     * Array of all types used in the program.
     */
    private readonly types: ReturnType[] = [];

    /**
     * The Any? type used for generic type parameter replacement.
     */
    private readonly anyNullableType: ClassTypeRef = {
        type: "Any",
        isNullable: true
    };

    /**
     * Creates a new TypedAstConverter.
     *
     * @param typir The Typir services for type inference
     * @param reflection The AST reflection for type checking
     */
    constructor(
        private readonly typir: ExpressionTypirServices<ScriptTypirSpecifics>,
        private readonly reflection: AstReflection
    ) {
        const customVoid = typir.factory.CustomVoid.getOrCreate();
        this.types.push({ kind: "void" });
        this.typeLookup.set(customVoid, 0);
    }

    /**
     * Converts a Script AST node to a TypedRoot.
     *
     * @param script The Script AST node
     * @returns The TypedRoot representation
     */
    convertScript(script: ScriptType): TypedAst {
        const imports: TypedImport[] = script.imports.map((imp: any) => ({
            name: imp.name,
            ref: imp.ref?.$refText ?? "",
            uri: imp.uri
        }));

        const functions: TypedFunction[] = script.functions.map((func: FunctionType) => this.convertFunction(func));

        return {
            types: this.types,
            imports,
            functions
        };
    }

    /**
     * Converts a Function AST node to a TypedFunction.
     *
     * @param func The Function AST node
     * @returns The TypedFunction representation
     */
    private convertFunction(func: FunctionType): TypedFunction {
        const parameters = this.convertParameters(func.parameterList);
        const returnTypeIndex = func.returnType != undefined ? this.getTypeIndex(func.returnType) : 0;
        const body = this.convertCallableBody(func.body);

        return {
            name: func.name,
            parameters,
            returnType: returnTypeIndex,
            body
        };
    }

    /**
     * Converts function parameters to typed parameters.
     *
     * @param paramList The function parameters list
     * @returns Array of typed parameters
     */
    private convertParameters(paramList: FunctionParametersType): TypedParameter[] {
        return paramList.parameters.map((param: any) => ({
            name: param.name,
            type: this.getTypeIndex(param)
        }));
    }

    /**
     * Converts a callable body (function or lambda body) to a TypedCallableBody.
     *
     * @param body The statements scope node
     * @returns The TypedCallableBody representation
     */
    private convertCallableBody(body: AstNode): TypedCallableBody {
        const statements = this.getStatementsFromBody(body);
        return {
            body: statements.map((statement) => this.convertStatement(statement))
        };
    }

    /**
     * Extracts statements from a callable body node.
     *
     * @param body The body node
     * @returns Array of statement AST nodes
     */
    private getStatementsFromBody(body: AstNode): AstNode[] {
        if (this.reflection.isInstance(body, statementTypes.statementsScopeType)) {
            return body.statements ?? [];
        }
        return [];
    }

    /**
     * Converts a statement AST node to a TypedStatement.
     *
     * @param statement The statement AST node
     * @returns The TypedStatement representation
     */
    private convertStatement(statement: AstNode): TypedStatement {
        if (this.reflection.isInstance(statement, statementTypes.ifStatementType)) {
            return this.convertIfStatement(statement);
        } else if (this.reflection.isInstance(statement, statementTypes.whileStatementType)) {
            return this.convertWhileStatement(statement);
        } else if (this.reflection.isInstance(statement, statementTypes.forStatementType)) {
            return this.convertForStatement(statement);
        } else if (this.reflection.isInstance(statement, statementTypes.variableDeclarationStatementType)) {
            return this.convertVariableDeclarationStatement(statement);
        } else if (this.reflection.isInstance(statement, statementTypes.assignmentStatementType)) {
            return this.convertAssignmentStatement(statement);
        } else if (this.reflection.isInstance(statement, statementTypes.expressionStatementType)) {
            return this.convertExpressionStatement(statement);
        } else if (this.reflection.isInstance(statement, statementTypes.breakStatementType)) {
            return this.convertBreakStatement();
        } else if (this.reflection.isInstance(statement, statementTypes.continueStatementType)) {
            return this.convertContinueStatement();
        } else if (this.reflection.isInstance(statement, ReturnStatement)) {
            return this.convertReturnStatement(statement);
        } else {
            throw new Error(`Unsupported statement type: ${statement}`);
        }
    }

    /**
     * Converts an if statement.
     *
     * @param statement The if statement AST node
     * @returns The TypedIfStatement representation
     */
    private convertIfStatement(statement: IfStatementType): TypedIfStatement {
        return {
            kind: TypedStatementKind.If,
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
    private convertWhileStatement(statement: WhileStatementType): TypedWhileStatement {
        return {
            kind: TypedStatementKind.While,
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
    private convertForStatement(statement: ForStatementType): TypedForStatement {
        return {
            kind: TypedStatementKind.For,
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
    private convertVariableDeclarationStatement(
        statement: VariableDeclarationStatementType
    ): TypedVariableDeclarationStatement {
        return {
            kind: TypedStatementKind.VariableDeclaration,
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
    private convertAssignmentStatement(statement: AssignmentStatementType): TypedAssignmentStatement {
        const left = this.convertExpression(statement.left);
        const right = this.convertExpression(statement.right);

        if (left.kind !== TypedExpressionKind.Identifier && left.kind !== TypedExpressionKind.MemberAccess) {
            throw new Error("Assignment left side must be an identifier or member access");
        }

        return {
            kind: TypedStatementKind.Assignment,
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
    private convertExpressionStatement(statement: any): TypedExpressionStatement {
        return {
            kind: TypedStatementKind.Expression,
            expression: this.convertExpression(statement.expression)
        };
    }

    /**
     * Converts a break statement.
     *
     * @returns The TypedBreakStatement representation
     */
    private convertBreakStatement(): TypedBreakStatement {
        return {
            kind: TypedStatementKind.Break
        };
    }

    /**
     * Converts a continue statement.
     *
     * @returns The TypedContinueStatement representation
     */
    private convertContinueStatement(): TypedContinueStatement {
        return {
            kind: TypedStatementKind.Continue
        };
    }

    /**
     * Converts a return statement.
     *
     * @param statement The return statement AST node
     * @returns The TypedReturnStatement representation
     */
    private convertReturnStatement(statement: any): TypedReturnStatement {
        return {
            kind: TypedStatementKind.Return,
            value: statement.value ? this.convertExpression(statement.value) : undefined
        };
    }

    /**
     * Converts an expression AST node to a TypedExpression.
     *
     * @param expr The expression AST node
     * @returns The TypedExpression representation
     */
    private convertExpression(expr: AstNode): TypedExpression {
        if (this.reflection.isInstance(expr, expressionTypes.unaryExpressionType)) {
            return this.convertUnaryExpression(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.binaryExpressionType)) {
            return this.convertBinaryExpression(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.ternaryExpressionType)) {
            return this.convertTernaryExpression(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.callExpressionType)) {
            return this.convertCallExpression(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.memberAccessExpressionType)) {
            return this.convertMemberAccessExpression(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.identifierExpressionType)) {
            return this.convertIdentifierExpression(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.stringLiteralExpressionType)) {
            return this.convertStringLiteral(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.intLiteralExpressionType)) {
            return this.convertIntLiteral(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.longLiteralExpressionType)) {
            return this.convertLongLiteral(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.floatLiteralExpressionType)) {
            return this.convertFloatLiteral(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.doubleLiteralExpressionType)) {
            return this.convertDoubleLiteral(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.booleanLiteralExpressionType)) {
            return this.convertBooleanLiteral(expr);
        } else if (this.reflection.isInstance(expr, expressionTypes.nullLiteralExpressionType)) {
            return this.convertNullLiteral(expr);
        } else if (this.reflection.isInstance(expr, LambdaExpression)) {
            return this.convertLambdaExpression(expr as LambdaExpressionType);
        }
        throw new Error(`Unsupported expression type: ${expr}`);
    }

    /**
     * Converts a unary expression.
     *
     * @param expr The unary expression AST node
     * @returns The TypedUnaryExpression representation
     */
    private convertUnaryExpression(expr: UnaryExpressionType): TypedUnaryExpression {
        return {
            kind: TypedExpressionKind.Unary,
            evalType: this.getTypeIndex(expr),
            operator: expr.operator as "-" | "!",
            expression: this.convertExpression(expr.expression)
        };
    }

    /**
     * Converts a binary expression.
     *
     * @param expr The binary expression AST node
     * @returns The TypedBinaryExpression representation
     */
    private convertBinaryExpression(expr: BinaryExpressionType): TypedBinaryExpression {
        return {
            kind: TypedExpressionKind.Binary,
            evalType: this.getTypeIndex(expr),
            operator: expr.operator as TypedBinaryExpression["operator"],
            left: this.convertExpression(expr.left),
            right: this.convertExpression(expr.right)
        };
    }

    /**
     * Converts a ternary expression.
     *
     * @param expr The ternary expression AST node
     * @returns The TypedTernaryExpression representation
     */
    private convertTernaryExpression(expr: TernaryExpressionType): TypedTernaryExpression {
        return {
            kind: TypedExpressionKind.Ternary,
            evalType: this.getTypeIndex(expr),
            condition: this.convertExpression(expr.condition),
            trueExpression: this.convertExpression(expr.trueExpression),
            falseExpression: this.convertExpression(expr.falseExpression)
        };
    }

    /**
     * Converts a call expression.
     *
     * @param expr The call expression AST node
     * @returns The TypedCallExpression representation
     */
    private convertCallExpression(
        expr: CallExpressionType
    ): TypedExpressionCallExpression | TypedFunctionCallExpression | TypedMemberCallExpression {
        const expression = expr.expression;
        const expressionType = this.typir.Inference.inferType(expression);
        if (Array.isArray(expressionType)) {
            throw new Error("Cannot infer type for call expression");
        }
        const args = expr.arguments.map((arg) => this.convertExpression(arg));
        const evalType = this.getTypeIndex(expr);
        if (isCustomFunctionType(expressionType)) {
            return this.convertFunctionCallExpression(expr, args, evalType);
        } else {
            return {
                kind: TypedExpressionKind.ExpressionCall,
                evalType,
                expression: this.convertExpression(expr.expression),
                arguments: args
            };
        }
    }

    /**
     * Converts a function call expression.
     * Only handles calls to named functions and member functions.
     *
     * @param expr The call expression AST node
     * @param args The converted argument expressions
     * @param evalType The evaluated type index of the call expression
     * @returns The TypedFunctionCallExpression or TypedMemberCallExpression representation
     */
    private convertFunctionCallExpression(
        expr: CallExpressionType,
        args: TypedExpression[],
        evalType: number
    ): TypedFunctionCallExpression | TypedMemberCallExpression {
        const expression = expr.expression;
        const overload = getCallOverload(
            expr,
            expr.expression,
            expr.genericArgs?.typeArguments ?? [],
            expr.arguments,
            this.typir
        );
        if (overload == undefined) {
            throw new Error("Could not determine overload for function call");
        }
        if (this.reflection.isInstance(expression, expressionTypes.memberAccessExpressionType)) {
            return {
                kind: TypedExpressionKind.MemberCall,
                evalType,
                expression: this.convertExpression(expression.expression),
                member: expression.member,
                isNullChaining: expression.isNullChaining ?? false,
                arguments: args,
                overload
            };
        } else if (this.reflection.isInstance(expression, expressionTypes.identifierExpressionType)) {
            return {
                kind: TypedExpressionKind.FunctionCall,
                evalType,
                name: (expression as IdentifierExpressionType).name,
                arguments: args,
                overload
            };
        } else {
            throw new Error("Unsupported function call expression type");
        }
    }

    /**
     * Converts a member access expression.
     *
     * @param expr The member access expression AST node
     * @returns The TypedMemberAccessExpression representation
     */
    private convertMemberAccessExpression(expr: MemberAccessExpressionType): TypedMemberAccessExpression {
        return {
            kind: TypedExpressionKind.MemberAccess,
            evalType: this.getTypeIndex(expr),
            expression: this.convertExpression(expr.expression),
            member: expr.member,
            isNullChaining: expr.isNullChaining ?? false
        };
    }

    /**
     * Converts an identifier expression.
     *
     * @param expr The identifier expression AST node
     * @returns The TypedIdentifierExpression representation
     */
    private convertIdentifierExpression(expr: IdentifierExpressionType): TypedIdentifierExpression {
        const scope = this.typir.ScopeProvider.getScope(expr);
        const entry = scope.getEntry(expr.name);

        if (entry == undefined) {
            throw new Error(`Identifier '${expr.name}' not found in scope`);
        }

        return {
            kind: TypedExpressionKind.Identifier,
            evalType: this.getTypeIndex(expr),
            name: expr.name,
            scope: entry?.definingScope.level
        };
    }

    /**
     * Converts a string literal expression.
     *
     * @param expr The string literal AST node
     * @returns The TypedStringLiteralExpression representation
     */
    private convertStringLiteral(expr: StringLiteralExpressionType): TypedStringLiteralExpression {
        return {
            kind: TypedExpressionKind.StringLiteral,
            evalType: this.getTypeIndex(expr),
            value: expr.value
        };
    }

    /**
     * Converts an int literal expression.
     *
     * @param expr The int literal AST node
     * @returns The TypedIntLiteralExpression representation
     */
    private convertIntLiteral(expr: IntLiteralExpressionType): TypedIntLiteralExpression {
        return {
            kind: TypedExpressionKind.IntLiteral,
            evalType: this.getTypeIndex(expr),
            value: expr.value
        };
    }

    /**
     * Converts a long literal expression.
     *
     * @param expr The long literal AST node
     * @returns The TypedLongLiteralExpression representation
     */
    private convertLongLiteral(expr: LongLiteralExpressionType): TypedLongLiteralExpression {
        return {
            kind: TypedExpressionKind.LongLiteral,
            evalType: this.getTypeIndex(expr),
            value: expr.value
        };
    }

    /**
     * Converts a float literal expression.
     *
     * @param expr The float literal AST node
     * @returns The TypedFloatLiteralExpression representation
     */
    private convertFloatLiteral(expr: FloatLiteralExpressionType): TypedFloatLiteralExpression {
        return {
            kind: TypedExpressionKind.FloatLiteral,
            evalType: this.getTypeIndex(expr),
            value: expr.value
        };
    }

    /**
     * Converts a double literal expression.
     *
     * @param expr The double literal AST node
     * @returns The TypedDoubleLiteralExpression representation
     */
    private convertDoubleLiteral(expr: DoubleLiteralExpressionType): TypedDoubleLiteralExpression {
        return {
            kind: TypedExpressionKind.DoubleLiteral,
            evalType: this.getTypeIndex(expr),
            value: expr.value
        };
    }

    /**
     * Converts a boolean literal expression.
     *
     * @param expr The boolean literal AST node
     * @returns The TypedBooleanLiteralExpression representation
     */
    private convertBooleanLiteral(expr: BooleanLiteralExpressionType): TypedBooleanLiteralExpression {
        return {
            kind: TypedExpressionKind.BooleanLiteral,
            evalType: this.getTypeIndex(expr),
            value: expr.value
        };
    }

    /**
     * Converts a null literal expression.
     *
     * @param expr The null literal AST node
     * @returns The TypedNullLiteralExpression representation
     */
    private convertNullLiteral(expr: NullLiteralExpressionType): TypedNullLiteralExpression {
        return {
            kind: TypedExpressionKind.NullLiteral,
            evalType: this.getTypeIndex(expr)
        };
    }

    /**
     * Converts a lambda expression.
     *
     * @param expr The lambda expression AST node
     * @returns The TypedLambdaExpression representation
     */
    private convertLambdaExpression(expr: LambdaExpressionType): TypedLambdaExpression {
        const parameters = expr.parameterList.parameters.map((param: any) => param.name);

        let body: TypedCallableBody;
        if (expr.expression != undefined) {
            const returnStatement: TypedReturnStatement = {
                kind: TypedStatementKind.Return,
                value: this.convertExpression(expr.expression)
            };
            body = {
                body: [returnStatement]
            };
        } else if (expr.body != undefined) {
            body = this.convertCallableBody(expr.body);
        } else {
            body = { body: [] };
        }

        return {
            kind: TypedExpressionKind.Lambda,
            evalType: this.getTypeIndex(expr),
            parameters,
            body
        };
    }

    /**
     * Gets the index of a type in the types array for the given AST node.
     * Infers the type, erases generic parameters, and adds to the types array if not already present.
     *
     * @param node The AST node to get the type for
     * @returns The index into the types array
     */
    private getTypeIndex(node: AstNode): number {
        const inferredType = this.typir.Inference.inferType(node);
        if (Array.isArray(inferredType)) {
            throw new Error("Cannot infer type");
        }
        if (this.typeLookup.has(inferredType)) {
            return this.typeLookup.get(inferredType)!;
        }

        let type: ReturnType;
        if (isCustomVoidType(inferredType)) {
            return 0;
        } else if (isCustomClassType(inferredType) || isCustomLambdaType(inferredType)) {
            type = inferredType.definition;
        } else {
            throw new Error("Unsupported type for indexing");
        }

        const erasedType = this.eraseGenericTypeParameters(type);
        const newIndex = this.types.length;
        this.types.push(erasedType);
        this.typeLookup.set(inferredType, newIndex);
        return newIndex;
    }

    /**
     * Erases generic type parameters from a type, replacing them with Any?.
     *
     * @param type The type to erase generics from
     * @returns The type with generics erased
     */
    private eraseGenericTypeParameters(type: ReturnType): ReturnType {
        if ("kind" in type) {
            return type;
        } else if ("generic" in type) {
            return this.anyNullableType;
        } else if ("type" in type) {
            return {
                type: type.type,
                isNullable: type.isNullable,
                typeArgs:
                    type.typeArgs != undefined
                        ? Object.fromEntries(
                              Object.entries(type.typeArgs).map(([argName, argType]) => [
                                  argName,
                                  this.eraseGenericTypeParameters(argType) as ValueType
                              ])
                          )
                        : undefined
            };
        } else {
            return {
                isNullable: type.isNullable,
                parameters: type.parameters.map((parameter) => ({
                    name: parameter.name,
                    type: this.eraseGenericTypeParameters(parameter.type) as ValueType
                })),
                returnType: this.eraseGenericTypeParameters(type.returnType) as ValueType
            };
        }
    }
}

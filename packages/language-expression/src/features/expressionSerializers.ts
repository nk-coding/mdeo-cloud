import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext, PluginContext, Builders } from "@mdeo/language-common";
import { ID } from "@mdeo/language-common";
import type {
    ExpressionTypes,
    UnaryExpressionType,
    BinaryExpressionType,
    TernaryExpressionType,
    CallExpressionGenericArgsType,
    CallExpressionType,
    MemberAccessExpressionType,
    IdentifierExpressionType,
    StringLiteralExpressionType,
    IntLiteralExpressionType,
    LongLiteralExpressionType,
    FloatLiteralExpressionType,
    DoubleLiteralExpressionType,
    BooleanLiteralExpressionType
} from "../grammar/expressionTypes.js";

/**
 * Registers all expression serializers for pretty-printing expression AST nodes.
 *
 * @param context The plugin context
 * @param services The Langium core services with AST serializer
 * @param types The generated expression types
 */
export function registerExpressionSerializers(
    { prettier }: PluginContext,
    services: LangiumCoreServices & AstSerializerAdditionalServices,
    types: ExpressionTypes
): void {
    const { AstSerializer } = services;
    const builders = prettier.doc.builders;

    AstSerializer.registerNodeSerializer(types.unaryExpressionType, (ctx) => printUnaryExpression(ctx));
    AstSerializer.registerNodeSerializer(types.binaryExpressionType, (ctx) => printBinaryExpression(ctx, builders));
    AstSerializer.registerNodeSerializer(types.ternaryExpressionType, (ctx) => printTernaryExpression(ctx, builders));
    AstSerializer.registerNodeSerializer(types.callExpressionGenericArgsType, (ctx) =>
        printCallExpressionGenericArgs(ctx, builders)
    );
    AstSerializer.registerNodeSerializer(types.callExpressionType, (ctx) => printCallExpression(ctx, builders));
    AstSerializer.registerNodeSerializer(types.memberAccessExpressionType, (ctx) => printMemberAccessExpression(ctx));
    AstSerializer.registerNodeSerializer(types.identifierExpressionType, (ctx) => printIdentifierExpression(ctx));
    AstSerializer.registerNodeSerializer(types.stringLiteralExpressionType, (ctx) => printStringLiteralExpression(ctx));
    AstSerializer.registerNodeSerializer(types.intLiteralExpressionType, (ctx) => printIntLiteralExpression(ctx));
    AstSerializer.registerNodeSerializer(types.longLiteralExpressionType, (ctx) => printLongLiteralExpression(ctx));
    AstSerializer.registerNodeSerializer(types.floatLiteralExpressionType, (ctx) => printFloatLiteralExpression(ctx));
    AstSerializer.registerNodeSerializer(types.doubleLiteralExpressionType, (ctx) => printDoubleLiteralExpression(ctx));
    AstSerializer.registerNodeSerializer(types.booleanLiteralExpressionType, (ctx) =>
        printBooleanLiteralExpression(ctx)
    );
    AstSerializer.registerNodeSerializer(types.nullLiteralExpressionType, () => printNullLiteralExpression());
}

/**
 * Prints a unary expression node.
 *
 * @param context The print context
 * @returns The formatted unary expression
 */
function printUnaryExpression(context: PrintContext<UnaryExpressionType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const operator = printPrimitive(getPrimitive(ctx, "operator"), ID);
    const expression = path.call(print, "expression");
    return [operator, expression];
}

/**
 * Prints a binary expression node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted binary expression
 */
function printBinaryExpression(context: PrintContext<BinaryExpressionType>, builders: Builders): Doc {
    const { ctx, path, print } = context;
    const { group } = builders;
    const left = path.call(print, "left");
    const operator = ctx.operator;
    const right = path.call(print, "right");
    return group([left, " ", operator, " ", right]);
}

/**
 * Prints a ternary expression node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted ternary expression
 */
function printTernaryExpression(context: PrintContext<TernaryExpressionType>, builders: Builders): Doc {
    const { path, print } = context;
    const { group } = builders;
    const condition = path.call(print, "condition");
    const trueExpression = path.call(print, "trueExpression");
    const falseExpression = path.call(print, "falseExpression");
    return group([condition, " ? ", trueExpression, " : ", falseExpression]);
}

/**
 * Prints call expression generic arguments node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted generic arguments
 */
function printCallExpressionGenericArgs(context: PrintContext<CallExpressionGenericArgsType>, builders: Builders): Doc {
    const { ctx, path, print } = context;
    const { join } = builders;
    const docs: Doc[] = [];

    if (ctx.typeArguments && ctx.typeArguments.length > 0) {
        docs.push("<");
        const typeArguments = path.map(print, "typeArguments");
        docs.push(join(", ", typeArguments));
        docs.push(">");
    }

    return docs;
}

/**
 * Prints a call expression node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted call expression
 */
function printCallExpression(context: PrintContext<CallExpressionType>, builders: Builders): Doc {
    const { ctx, path, print } = context;
    const { join, group } = builders;
    const docs: Doc[] = [path.call(print, "expression")];

    if (ctx.genericArgs != undefined) {
        docs.push(path.call(print, "genericArgs"));
    }

    docs.push("(");
    if (ctx.arguments && ctx.arguments.length > 0) {
        const args = path.map(print, "arguments");
        docs.push(join(", ", args));
    }
    docs.push(")");

    return group(docs);
}

/**
 * Prints a member access expression node.
 *
 * @param context The print context
 * @returns The formatted member access expression
 */
function printMemberAccessExpression(context: PrintContext<MemberAccessExpressionType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const expression = path.call(print, "expression");
    const operator = ctx.isNullChaining ? "?." : ".";
    const member = printPrimitive(getPrimitive(ctx, "member"), ID);
    return [expression, operator, member];
}

/**
 * Prints an identifier expression node.
 *
 * @param context The print context
 * @returns The formatted identifier expression
 */
function printIdentifierExpression(context: PrintContext<IdentifierExpressionType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return printPrimitive(getPrimitive(ctx, "name"), ID);
}

/**
 * Prints a string literal expression node.
 *
 * @param context The print context
 * @returns The formatted string literal expression
 */
function printStringLiteralExpression(context: PrintContext<StringLiteralExpressionType>): Doc {
    const { ctx } = context;
    return `"${ctx.value}"`;
}

/**
 * Prints an int literal expression node.
 *
 * @param context The print context
 * @returns The formatted int literal expression
 */
function printIntLiteralExpression(context: PrintContext<IntLiteralExpressionType>): Doc {
    const { ctx } = context;
    return ctx.value;
}

/**
 * Prints a long literal expression node.
 *
 * @param context The print context
 * @returns The formatted long literal expression
 */
function printLongLiteralExpression(context: PrintContext<LongLiteralExpressionType>): Doc {
    const { ctx } = context;
    return ctx.value;
}

/**
 * Prints a float literal expression node.
 *
 * @param context The print context
 * @returns The formatted float literal expression
 */
function printFloatLiteralExpression(context: PrintContext<FloatLiteralExpressionType>): Doc {
    const { ctx } = context;
    return ctx.value;
}

/**
 * Prints a double literal expression node.
 *
 * @param context The print context
 * @returns The formatted double literal expression
 */
function printDoubleLiteralExpression(context: PrintContext<DoubleLiteralExpressionType>): Doc {
    const { ctx } = context;
    return ctx.value;
}

/**
 * Prints a boolean literal expression node.
 *
 * @param context The print context
 * @returns The formatted boolean literal expression
 */
function printBooleanLiteralExpression(context: PrintContext<BooleanLiteralExpressionType>): Doc {
    const { ctx } = context;
    return ctx.value ? "true" : "false";
}

/**
 * Prints a null literal expression node.
 *
 * @returns The formatted null literal expression
 */
function printNullLiteralExpression(): Doc {
    return "null";
}

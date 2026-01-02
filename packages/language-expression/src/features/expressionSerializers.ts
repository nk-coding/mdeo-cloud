import type { AstPath, Doc } from "prettier";
import type { CstNode, CstUtils, LangiumCoreServices, isCompositeCstNode, isLeafCstNode } from "langium";
import type {
    AstSerializerAdditionalServices,
    PrintContext,
    PluginContext,
    Builders,
    Print
} from "@mdeo/language-common";
import { ID } from "@mdeo/language-common";
import { getExpressionPrecedence, Precedence } from "./precedenceHelper.js";
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
    BooleanLiteralExpressionType,
    BaseExpressionType
} from "../grammar/expressionTypes.js";

/**
 * Registers all expression serializers for pretty-printing expression AST nodes.
 *
 * @param context The plugin context
 * @param services The Langium core services with AST serializer
 * @param types The generated expression types
 */
export function registerExpressionSerializers(
    { prettier, langium }: PluginContext,
    services: LangiumCoreServices & AstSerializerAdditionalServices,
    types: ExpressionTypes
): void {
    const { AstSerializer } = services;
    const builders = prettier.doc.builders;
    const precedenceContext: PrecedenceContext = {
        types,
        builders,
        isLeafCstNode: langium.isLeafCstNode,
        isCompositeCstNode: langium.isCompositeCstNode
    };

    AstSerializer.registerNodeSerializer(types.unaryExpressionType, (ctx) =>
        printUnaryExpression(ctx, precedenceContext)
    );
    AstSerializer.registerNodeSerializer(types.binaryExpressionType, (ctx) =>
        printBinaryExpression(ctx, precedenceContext)
    );
    AstSerializer.registerNodeSerializer(types.ternaryExpressionType, (ctx) =>
        printTernaryExpression(ctx, precedenceContext)
    );
    AstSerializer.registerNodeSerializer(types.callExpressionGenericArgsType, (ctx) =>
        printCallExpressionGenericArgs(ctx, builders)
    );
    AstSerializer.registerNodeSerializer(types.callExpressionType, (ctx) =>
        printCallExpression(ctx, precedenceContext)
    );
    AstSerializer.registerNodeSerializer(types.memberAccessExpressionType, (ctx) =>
        printMemberAccessExpression(ctx, precedenceContext)
    );
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
 * Context object containing dependencies needed for precedence-aware expression printing.
 */
interface PrecedenceContext {
    /**
     * The generated expression types
     */
    types: ExpressionTypes;
    /**
     * Prettier doc builders for formatting
     */
    builders: Builders;
    /**
     * Function to check if a CST node is a leaf node
     */
    isLeafCstNode: typeof isLeafCstNode;
    /**
     * Function to check if a CST node is a composite node
     */
    isCompositeCstNode: typeof isCompositeCstNode;
}

/**
 * Helper function to print an expression with precedence awareness.
 * Wraps the expression in parentheses if needed based on precedence comparison.
 *
 * @param path The AST path to print
 * @param parentPrecedence The precedence of the parent expression
 * @param print The print function
 * @param context The precedence context containing types, builders, and CST utilities
 * @returns The formatted expression, potentially wrapped in parentheses
 */
function printWithPrec<T extends BaseExpressionType>(
    path: AstPath<T>,
    parentPrecedence: Precedence,
    print: Print,
    context: PrecedenceContext
): Doc {
    const { types, builders } = context;
    const { group, indent, softline } = builders;
    const childPrecedence = getExpressionPrecedence(path.node, types);
    const cstNode = path.node.$cstNode;

    const needsParens =
        childPrecedence > parentPrecedence ||
        (childPrecedence > Precedence.PRIMARY && cstNode != undefined && isBracketedExpression(cstNode, context));

    const printed = print(path);
    return needsParens ? group(["(", indent([softline, printed]), softline, ")"]) : printed;
}

function isBracketedExpression(node: CstNode, { isLeafCstNode, isCompositeCstNode }: PrecedenceContext): boolean {
    if (!isCompositeCstNode(node)) {
        return false;
    }
    let content = node.content;
    while (true) {
        const visibleContent = content.filter((node) => !node.hidden);
        if (visibleContent.length === 1 && isCompositeCstNode(visibleContent[0])) {
            content = visibleContent[0].content;
        } else if (visibleContent.length === 3) {
            const [first, middle, last] = visibleContent;
            return (
                isLeafCstNode(first) &&
                first.text === "(" &&
                isLeafCstNode(last) &&
                last.text === ")" &&
                isCompositeCstNode(middle)
            );
        } else {
            return false;
        }
    }
}

/**
 * Prints a unary expression node.
 *
 * @param context The print context
 * @param precedenceContext The precedence context
 * @returns The formatted unary expression
 */
function printUnaryExpression(context: PrintContext<UnaryExpressionType>, precedenceContext: PrecedenceContext): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const operator = printPrimitive(getPrimitive(ctx, "operator"), ID);
    const expression = path.call((p) => printWithPrec(p, Precedence.UNARY, print, precedenceContext), "expression");
    return [operator, expression];
}

/**
 * Prints a binary expression node.
 *
 * @param context The print context
 * @param precedenceContext The precedence context
 * @returns The formatted binary expression
 */
function printBinaryExpression(context: PrintContext<BinaryExpressionType>, precedenceContext: PrecedenceContext): Doc {
    const { ctx, path, print } = context;
    const { group } = precedenceContext.builders;

    const precedence = getExpressionPrecedence(ctx, precedenceContext.types);

    const left = path.call((p) => printWithPrec(p, precedence, print, precedenceContext), "left");
    const operator = ctx.operator;
    const right = path.call((p) => printWithPrec(p, precedence - 1, print, precedenceContext), "right");

    return group([left, " ", operator, " ", right]);
}

/**
 * Prints a ternary expression node.
 *
 * @param context The print context
 * @param precedenceContext The precedence context
 * @returns The formatted ternary expression
 */
function printTernaryExpression(
    context: PrintContext<TernaryExpressionType>,
    precedenceContext: PrecedenceContext
): Doc {
    const { path, print } = context;
    const { group } = precedenceContext.builders;

    const condition = path.call((p) => printWithPrec(p, Precedence.TERNARY, print, precedenceContext), "condition");
    const trueExpression = path.call(
        (p) => printWithPrec(p, Precedence.TERNARY, print, precedenceContext),
        "trueExpression"
    );
    const falseExpression = path.call(
        (p) => printWithPrec(p, Precedence.TERNARY, print, precedenceContext),
        "falseExpression"
    );

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
 * @param precedenceContext The precedence context
 * @returns The formatted call expression
 */
function printCallExpression(context: PrintContext<CallExpressionType>, precedenceContext: PrecedenceContext): Doc {
    const { ctx, path, print } = context;
    const { join, group, line, softline, indent } = precedenceContext.builders;

    const docs: Doc[] = [
        path.call((p) => printWithPrec(p, Precedence.MEMBER_CALL, print, precedenceContext), "expression")
    ];

    if (ctx.genericArgs != undefined) {
        docs.push(path.call(print, "genericArgs"));
    }

    docs.push(group(["(", indent([softline, join([",", line], path.map(print, "arguments"))]), softline, ")"]));

    return group(docs);
}

/**
 * Prints a member access expression node.
 *
 * @param context The print context
 * @param precedenceContext The precedence context
 * @returns The formatted member access expression
 */
function printMemberAccessExpression(
    context: PrintContext<MemberAccessExpressionType>,
    precedenceContext: PrecedenceContext
): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;

    const expression = path.call(
        (p) => printWithPrec(p, Precedence.MEMBER_CALL, print, precedenceContext),
        "expression"
    );
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

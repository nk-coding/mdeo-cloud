import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-common";
import type { Doc } from "prettier";
import { ID, STRING } from "@mdeo/language-common";
import { serializeNewlineSep, sharedImport } from "@mdeo/language-shared";
import {
    PatternModifier,
    StopStatement,
    PatternVariable,
    PatternPropertyAssignment,
    PatternObjectInstance,
    PatternLinkEnd,
    PatternLink,
    WhereClause,
    Pattern,
    StatementsScope,
    MatchStatement,
    IfMatchStatement,
    WhileMatchStatement,
    UntilMatchStatement,
    ForMatchStatement,
    ElseIfBranch,
    IfExpressionStatement,
    WhileExpressionStatement,
    MetamodelFileImport,
    ModelTransformation,
    LambdaParameter,
    LambdaParameters,
    LambdaExpression,
    type PatternModifierType,
    type StopStatementType,
    type PatternVariableType,
    type PatternPropertyAssignmentType,
    type PatternObjectInstanceType,
    type PatternLinkEndType,
    type PatternLinkType,
    type WhereClauseType,
    type PatternType,
    type MatchStatementType,
    type IfMatchStatementType,
    type WhileMatchStatementType,
    type UntilMatchStatementType,
    type ForMatchStatementType,
    type ElseIfBranchType,
    type IfExpressionStatementType,
    type WhileExpressionStatementType,
    type MetamodelFileImportType,
    type ModelTransformationType,
    type LambdaParameterType,
    type LambdaParametersType,
    type LambdaExpressionType,
    type StatementsScopeType
} from "../grammar/modelTransformationTypes.js";

const { doc } = sharedImport("prettier");
const { indent, hardline, line, group, softline, join } = doc.builders;

/**
 * Language services type combining core and serializer services.
 */
type ModelTransformationLanguageServices = LangiumCoreServices & AstSerializerAdditionalServices;

/**
 * Prints a pattern modifier node.
 *
 * @param context The print context.
 * @returns The formatted modifier keyword.
 */
function printPatternModifier(context: PrintContext<PatternModifierType>): Doc {
    return context.ctx.modifier ?? "";
}

/**
 * Prints a stop statement node.
 *
 * @param context The print context.
 * @returns The formatted stop/kill keyword.
 */
function printStopStatement(context: PrintContext<StopStatementType>): Doc {
    return context.ctx.keyword ?? "stop";
}

/**
 * Registers node serializers for the Model Transformation language.
 * These serializers control how AST nodes are converted back to text.
 *
 * @param services The language services to register serializers with.
 */
export function registerModelTransformationSerializers(services: ModelTransformationLanguageServices): void {
    const { AstSerializer } = services;

    // Pattern elements
    AstSerializer.registerNodeSerializer(PatternModifier, (ctx) => printPatternModifier(ctx));
    AstSerializer.registerNodeSerializer(PatternVariable, (ctx) => printPatternVariable(ctx));
    AstSerializer.registerNodeSerializer(PatternPropertyAssignment, (ctx) => printPatternPropertyAssignment(ctx));
    AstSerializer.registerNodeSerializer(PatternObjectInstance, (ctx) => printPatternObjectInstance(ctx));
    AstSerializer.registerNodeSerializer(PatternLinkEnd, (ctx) => printPatternLinkEnd(ctx));
    AstSerializer.registerNodeSerializer(PatternLink, (ctx) => printPatternLink(ctx));
    AstSerializer.registerNodeSerializer(WhereClause, (ctx) => printWhereClause(ctx));
    AstSerializer.registerNodeSerializer(Pattern, (ctx) => printPattern(ctx));

    // Statements
    AstSerializer.registerNodeSerializer(StatementsScope, (ctx) => printStatementsScope(ctx));
    AstSerializer.registerNodeSerializer(MatchStatement, (ctx) => printMatchStatement(ctx));
    AstSerializer.registerNodeSerializer(IfMatchStatement, (ctx) => printIfMatchStatement(ctx));
    AstSerializer.registerNodeSerializer(WhileMatchStatement, (ctx) => printWhileMatchStatement(ctx));
    AstSerializer.registerNodeSerializer(UntilMatchStatement, (ctx) => printUntilMatchStatement(ctx));
    AstSerializer.registerNodeSerializer(ForMatchStatement, (ctx) => printForMatchStatement(ctx));
    AstSerializer.registerNodeSerializer(ElseIfBranch, (ctx) => printElseIfBranch(ctx));
    AstSerializer.registerNodeSerializer(IfExpressionStatement, (ctx) => printIfExpressionStatement(ctx));
    AstSerializer.registerNodeSerializer(WhileExpressionStatement, (ctx) => printWhileExpressionStatement(ctx));
    AstSerializer.registerNodeSerializer(StopStatement, (ctx) => printStopStatement(ctx));

    // Lambda expressions
    AstSerializer.registerNodeSerializer(LambdaParameter, (ctx) => printLambdaParameter(ctx));
    AstSerializer.registerNodeSerializer(LambdaParameters, (ctx) => printLambdaParameters(ctx));
    AstSerializer.registerNodeSerializer(LambdaExpression, (ctx) => printLambdaExpression(ctx));

    // Root
    AstSerializer.registerNodeSerializer(MetamodelFileImport, (ctx) => printMetamodelFileImport(ctx));
    AstSerializer.registerNodeSerializer(ModelTransformation, (ctx) => printModelTransformation(ctx));
}

/**
 * Prints a pattern variable node.
 *
 * @param context The print context.
 * @returns The formatted variable declaration.
 */
function printPatternVariable(context: PrintContext<PatternVariableType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    return ["var ", printPrimitive(getPrimitive(ctx, "name"), ID), ": ", path.call(print, "type")];
}

/**
 * Prints a pattern property assignment node.
 *
 * @param context The print context.
 * @returns The formatted property assignment.
 */
function printPatternPropertyAssignment(context: PrintContext<PatternPropertyAssignmentType>): Doc {
    const { ctx, path, print, printReference } = context;
    const name = path.call((ref) => printReference(ref, ID), "name");
    const operator = ctx.operator ?? "=";
    const value = path.call(print, "value");
    return [name, " ", operator, " ", value];
}

/**
 * Prints a pattern object instance node.
 *
 * @param context The print context.
 * @returns The formatted object instance.
 */
function printPatternObjectInstance(context: PrintContext<PatternObjectInstanceType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print, printReference } = context;
    const docs: Doc[] = [];

    if (ctx.modifier != undefined) {
        docs.push(path.call(print, "modifier"), " ");
    }

    docs.push(printPrimitive(getPrimitive(ctx, "name"), ID));
    docs.push(": ");
    docs.push(path.call((ref) => printReference(ref, ID), "class"));
    docs.push(" {");

    const content = serializeNewlineSep(context, ["properties"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]), hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints a pattern link end node.
 *
 * @param context The print context.
 * @returns The formatted link end.
 */
function printPatternLinkEnd(context: PrintContext<PatternLinkEndType>): Doc {
    const { ctx, path, printReference } = context;
    const docs: Doc[] = [];

    docs.push(path.call((ref) => printReference(ref, ID), "object"));

    if (ctx.property != undefined && ctx.property.$refText) {
        docs.push(".");
        docs.push(ctx.property.$refText);
    }

    return docs;
}

/**
 * Prints a pattern link node.
 *
 * @param context The print context.
 * @returns The formatted link.
 */
function printPatternLink(context: PrintContext<PatternLinkType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    if (ctx.modifier != undefined) {
        docs.push(path.call(print, "modifier"), " ");
    }

    const source = path.call(print, "source");
    const target = path.call(print, "target");
    docs.push(source, " -- ", target);

    return docs;
}

/**
 * Prints a where clause node.
 *
 * @param context The print context.
 * @returns The formatted where clause.
 */
function printWhereClause(context: PrintContext<WhereClauseType>): Doc {
    const { path, print } = context;
    return ["where ", path.call(print, "expression")];
}

/**
 * Prints a pattern node.
 *
 * @param context The print context.
 * @returns The formatted pattern.
 */
function printPattern(context: PrintContext<PatternType>): Doc {
    const docs: Doc[] = ["{"];

    const content = serializeNewlineSep(context, ["elements"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]), hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints a statements scope node.
 *
 * @param context The print context.
 * @returns The formatted statements scope.
 */
function printStatementsScope(context: PrintContext<StatementsScopeType>): Doc {
    const docs: Doc[] = ["{"];

    const content = serializeNewlineSep(context, ["statements"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]), hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints a match statement node.
 *
 * @param context The print context.
 * @returns The formatted match statement.
 */
function printMatchStatement(context: PrintContext<MatchStatementType>): Doc {
    const { path, print } = context;
    return ["match ", path.call(print, "pattern")];
}

/**
 * Prints an if-match statement node.
 *
 * @param context The print context.
 * @returns The formatted if-match statement.
 */
function printIfMatchStatement(context: PrintContext<IfMatchStatementType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    docs.push("if match ", path.call(print, "pattern"), " then ", path.call(print, "thenBlock"));

    if (ctx.elseBlock != undefined) {
        docs.push(" else ", path.call(print, "elseBlock"));
    }

    return docs;
}

/**
 * Prints a while-match statement node.
 *
 * @param context The print context.
 * @returns The formatted while-match statement.
 */
function printWhileMatchStatement(context: PrintContext<WhileMatchStatementType>): Doc {
    const { path, print } = context;
    return ["while match ", path.call(print, "pattern"), " do ", path.call(print, "doBlock")];
}

/**
 * Prints an until-match statement node.
 *
 * @param context The print context.
 * @returns The formatted until-match statement.
 */
function printUntilMatchStatement(context: PrintContext<UntilMatchStatementType>): Doc {
    const { path, print } = context;
    return ["until match ", path.call(print, "pattern"), " do ", path.call(print, "doBlock")];
}

/**
 * Prints a for-match statement node.
 *
 * @param context The print context.
 * @returns The formatted for-match statement.
 */
function printForMatchStatement(context: PrintContext<ForMatchStatementType>): Doc {
    const { path, print } = context;
    return ["for match ", path.call(print, "pattern"), " do ", path.call(print, "doBlock")];
}

/**
 * Prints an else-if branch node.
 *
 * @param context The print context.
 * @returns The formatted else-if branch.
 */
function printElseIfBranch(context: PrintContext<ElseIfBranchType>): Doc {
    const { path, print } = context;
    return ["else if (", path.call(print, "condition"), ") ", path.call(print, "block")];
}

/**
 * Prints an if-expression statement node.
 *
 * @param context The print context.
 * @returns The formatted if-expression statement.
 */
function printIfExpressionStatement(context: PrintContext<IfExpressionStatementType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    docs.push("if (", path.call(print, "condition"), ") ", path.call(print, "thenBlock"));

    if (ctx.elseIfBranches.length > 0) {
        docs.push(" ", join(" ", path.map(print, "elseIfBranches")));
    }

    if (ctx.elseBlock != undefined) {
        docs.push(" else ", path.call(print, "elseBlock"));
    }

    return docs;
}

/**
 * Prints a while-expression statement node.
 *
 * @param context The print context.
 * @returns The formatted while-expression statement.
 */
function printWhileExpressionStatement(context: PrintContext<WhileExpressionStatementType>): Doc {
    const { path, print } = context;
    return ["while (", path.call(print, "condition"), ") ", path.call(print, "block")];
}

/**
 * Prints a lambda parameter node.
 *
 * @param context The print context.
 * @returns The formatted lambda parameter.
 */
function printLambdaParameter(context: PrintContext<LambdaParameterType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return printPrimitive(getPrimitive(ctx, "name"), ID);
}

/**
 * Prints lambda parameters node (with round brackets).
 *
 * @param context The print context.
 * @returns The formatted lambda parameters.
 */
function printLambdaParameters(context: PrintContext<LambdaParametersType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    if (ctx.parameters.length > 0) {
        docs.push(group(["(", indent([softline, join([",", line], path.map(print, "parameters"))]), softline, ")"]));
    } else {
        docs.push("()");
    }

    return docs;
}

/**
 * Prints a lambda expression node.
 *
 * @param context The print context.
 * @returns The formatted lambda expression.
 */
function printLambdaExpression(context: PrintContext<LambdaExpressionType>): Doc {
    const { path, print } = context;
    return [path.call(print, "parameterList"), " => ", path.call(print, "expression")];
}

/**
 * Prints a metamodel file import node.
 *
 * @param context The print context.
 * @returns The formatted import.
 */
function printMetamodelFileImport(context: PrintContext<MetamodelFileImportType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return ["using ", printPrimitive(getPrimitive(ctx, "file"), STRING)];
}

/**
 * Prints the root model transformation node.
 *
 * @param context The print context.
 * @returns The formatted model transformation.
 */
function printModelTransformation(context: PrintContext<ModelTransformationType>): Doc {
    const { path, print } = context;
    const docs: Doc[] = [];

    docs.push(path.call(print, "import"));
    docs.push(hardline);

    const content = serializeNewlineSep(context, ["statements"], doc.builders);
    if (content.length > 0) {
        docs.push(hardline);
        docs.push(content);
    }

    return docs;
}

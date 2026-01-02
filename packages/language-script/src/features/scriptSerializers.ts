import type { Doc, doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext, PluginContext } from "@mdeo/language-common";
import { ID, serializeNewlineSep, printDanglingComments } from "@mdeo/language-common";
import type {
    ReturnStatementType,
    LambdaExpressionType,
    LambdaParameterType,
    LambdaParametersType,
    FunctionParametersType,
    FunctionType,
    ScriptType
} from "../grammar/types.js";
import {
    ReturnStatement,
    LambdaExpression,
    LambdaParameter,
    LambdaParameters,
    FunctionParameter,
    FunctionParameters,
    Function,
    FunctionImport,
    FunctionFileImport,
    Script
} from "../grammar/types.js";

/**
 * Registers all script serializers for pretty-printing script AST nodes.
 *
 * @param context The plugin context
 * @param services The Langium core services with AST serializer
 */
export function registerScriptSerializers(
    { prettier }: PluginContext,
    services: LangiumCoreServices & AstSerializerAdditionalServices
): void {
    const { AstSerializer } = services;
    const builders = prettier.doc.builders;

    AstSerializer.registerNodeSerializer(ReturnStatement, (ctx) => printReturnStatement(ctx));
    AstSerializer.registerNodeSerializer(LambdaParameter, (ctx) => printLambdaParameter(ctx));
    AstSerializer.registerNodeSerializer(LambdaParameters, (ctx) => printLambdaParameters(ctx, builders));
    AstSerializer.registerNodeSerializer(LambdaExpression, (ctx) => printLambdaExpression(ctx));
    AstSerializer.registerNodeSerializer(FunctionParameter, (ctx) => printFunctionParameter(ctx));
    AstSerializer.registerNodeSerializer(FunctionParameters, (ctx) => printFunctionParameters(ctx, builders));
    AstSerializer.registerNodeSerializer(Function, (ctx) => printFunction(ctx, builders));
    AstSerializer.registerNodeSerializer(FunctionImport, (ctx) => printFunctionImport(ctx));
    AstSerializer.registerNodeSerializer(FunctionFileImport, (ctx) => printFunctionFileImport(ctx, builders));
    AstSerializer.registerNodeSerializer(Script, (ctx) => printScript(ctx, builders));
}

/**
 * Prints a return statement node.
 *
 * @param context The print context
 * @returns The formatted return statement
 */
function printReturnStatement(context: PrintContext<ReturnStatementType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = ["return"];

    if (ctx.value != undefined) {
        docs.push(" ", path.call(print, "value"));
    }

    return docs;
}

/**
 * Prints a lambda parameter node.
 *
 * @param context The print context
 * @returns The formatted lambda parameter
 */
function printLambdaParameter(context: PrintContext<LambdaParameterType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return printPrimitive(getPrimitive(ctx, "name"), ID);
}

/**
 * Prints lambda parameters node (with round brackets).
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted lambda parameters
 */
function printLambdaParameters(context: PrintContext<LambdaParametersType>, builders: typeof doc.builders): Doc {
    const { ctx, path, print } = context;
    const { join, line, group, softline, indent } = builders;
    const docs: Doc[] = [];

    if (ctx.parameters.length > 0) {
        docs.push(group(["(", indent([softline, join([",", line], path.map(print, "parameters"))]), softline, ")"]));
    } else {
        docs.push("(");
        const danglingComments = printDanglingComments(context, builders);
        if (danglingComments.length > 0) {
            docs.push(line, danglingComments, line);
        }
        docs.push(")");
    }
    return docs;
}

/**
 * Prints a lambda expression node.
 *
 * @param context The print context
 * @returns The formatted lambda expression
 */
function printLambdaExpression(context: PrintContext<LambdaExpressionType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [path.call(print, "parameterList"), " => "];

    if (ctx.expression != undefined) {
        docs.push(path.call(print, "expression"));
    } else if (ctx.body != undefined) {
        docs.push(path.call(print, "body"));
    }

    return docs;
}

/**
 * Prints a function parameter node.
 *
 * @param context The print context
 * @returns The formatted function parameter
 */
function printFunctionParameter(context: PrintContext<any>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const docs: Doc[] = [printPrimitive(getPrimitive(ctx, "name"), ID)];

    if (ctx.type != undefined) {
        docs.push(": ", path.call(print, "type"));
    }

    return docs;
}

/**
 * Prints function parameters node (with round brackets).
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted function parameters
 */
function printFunctionParameters(context: PrintContext<FunctionParametersType>, builders: typeof doc.builders): Doc {
    const { ctx, path, print } = context;
    const { join, line, softline, indent, group } = builders;
    const docs: Doc[] = [];

    if (ctx.parameters && ctx.parameters.length > 0) {
        docs.push(group(["(", indent([softline, join([",", line], path.map(print, "parameters"))]), softline, ")"]));
    } else {
        docs.push("(");
        const danglingComments = printDanglingComments(context, builders);
        if (danglingComments.length > 0) {
            docs.push(line, danglingComments, line);
        }
        docs.push(")");
    }

    return docs;
}

/**
 * Prints a function node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted function
 */
function printFunction(context: PrintContext<FunctionType>, builders: typeof doc.builders): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const { group } = builders;
    const docs: Doc[] = ["fun ", printPrimitive(getPrimitive(ctx, "name"), ID), path.call(print, "parameterList")];

    if (ctx.returnType != undefined) {
        docs.push(": ", path.call(print, "returnType"));
    }

    docs.push(" ", path.call(print, "body"));

    return group(docs);
}

/**
 * Prints a function import node.
 *
 * @param context The print context
 * @returns The formatted function import
 */
function printFunctionImport(context: PrintContext<any>): Doc {
    const { path, print } = context;
    return path.call(print, "function", "ref");
}

/**
 * Prints a function file import node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted function file import
 */
function printFunctionFileImport(context: PrintContext<any>, builders: typeof doc.builders): Doc {
    const { ctx, path, print, printPrimitive, getPrimitive } = context;
    const { join } = builders;
    const docs: Doc[] = ["import "];

    if (ctx.imports && ctx.imports.length > 0) {
        docs.push("{");
        const imports = path.map(print, "imports");
        docs.push(join(", ", imports));
        docs.push("} from ");
    } else {
        docs.push("* from ");
    }

    docs.push(printPrimitive(getPrimitive(ctx, "uri"), ID));

    return docs;
}

/**
 * Prints the root script node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted script
 */
function printScript(context: PrintContext<ScriptType>, builders: typeof doc.builders): Doc {
    return serializeNewlineSep(context, ["imports", "functions"], builders);
}

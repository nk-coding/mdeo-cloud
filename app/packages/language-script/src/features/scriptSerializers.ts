import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-common";
import { ID, STRING } from "@mdeo/language-common";
import {
    serializeNewlineSep,
    printDanglingComments,
    sharedImport,
    registerImportSerializers
} from "@mdeo/language-shared";
import type {
    ReturnStatementType,
    LambdaExpressionType,
    LambdaParameterType,
    LambdaParametersType,
    FunctionParametersType,
    FunctionType,
    ScriptType,
    FunctionParameterType,
    ExtensionExpressionType,
    MetamodelFileImportType
} from "../grammar/scriptTypes.js";
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
    Script,
    ExtensionExpression,
    MetamodelFileImport
} from "../grammar/scriptTypes.js";

const { doc } = sharedImport("prettier");
const { join, line, group, softline, indent } = doc.builders;

/**
 * Registers all script serializers for pretty-printing script AST nodes.
 *
 * @param services The Langium core services with AST serializer
 */
export function registerScriptSerializers(services: LangiumCoreServices & AstSerializerAdditionalServices): void {
    const { AstSerializer } = services;

    AstSerializer.registerNodeSerializer(ReturnStatement, (ctx) => printReturnStatement(ctx));
    AstSerializer.registerNodeSerializer(LambdaParameter, (ctx) => printLambdaParameter(ctx));
    AstSerializer.registerNodeSerializer(LambdaParameters, (ctx) => printLambdaParameters(ctx));
    AstSerializer.registerNodeSerializer(LambdaExpression, (ctx) => printLambdaExpression(ctx));
    AstSerializer.registerNodeSerializer(FunctionParameter, (ctx) => printFunctionParameter(ctx));
    AstSerializer.registerNodeSerializer(FunctionParameters, (ctx) => printFunctionParameters(ctx));
    AstSerializer.registerNodeSerializer(Function, (ctx) => printFunction(ctx));
    AstSerializer.registerNodeSerializer(MetamodelFileImport, (ctx) => printMetamodelFileImport(ctx));
    AstSerializer.registerNodeSerializer(Script, (ctx) => printScript(ctx));
    AstSerializer.registerNodeSerializer(ExtensionExpression, (ctx) => printExtensionExpression(ctx));
    registerImportSerializers(services, doc.builders, FunctionImport, FunctionFileImport);
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
 * @returns The formatted lambda parameters
 */
function printLambdaParameters(context: PrintContext<LambdaParametersType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    if (ctx.parameters.length > 0) {
        docs.push(group(["(", indent([softline, join([",", line], path.map(print, "parameters"))]), softline, ")"]));
    } else {
        docs.push("(");
        const danglingComments = printDanglingComments(context, doc.builders);
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
function printFunctionParameter(context: PrintContext<FunctionParameterType>): Doc {
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
 * @returns The formatted function parameters
 */
function printFunctionParameters(context: PrintContext<FunctionParametersType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    if (ctx.parameters && ctx.parameters.length > 0) {
        docs.push(group(["(", indent([softline, join([",", line], path.map(print, "parameters"))]), softline, ")"]));
    } else {
        docs.push("(");
        const danglingComments = printDanglingComments(context, doc.builders);
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
 * @returns The formatted function
 */
function printFunction(context: PrintContext<FunctionType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const docs: Doc[] = ["fun ", printPrimitive(getPrimitive(ctx, "name"), ID), path.call(print, "parameterList")];

    if (ctx.returnType != undefined) {
        docs.push(": ", path.call(print, "returnType"));
    }

    docs.push(" ", path.call(print, "body"));

    return group(docs);
}

/**
 * Prints a metamodel file import node.
 *
 * @param context The print context
 * @returns The formatted metamodel file import
 */
function printMetamodelFileImport(context: PrintContext<MetamodelFileImportType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return ["using ", printPrimitive(getPrimitive(ctx, "file"), STRING)];
}

/**
 * Prints the root script node.
 *
 * All import-related elements (metamodel import and function file imports) are written as a
 * single block at the top of the file. If there are any function definitions after the imports,
 * they are separated from the import block by one empty line.
 *
 * @param context The print context
 * @returns The formatted script
 */
function printScript(context: PrintContext<ScriptType>): Doc {
    const { ctx, path, print } = context;
    const { hardline } = doc.builders;

    const importDocs: Doc[] = [];
    if (ctx.metamodelImport != undefined) {
        importDocs.push(path.call(print, "metamodelImport"));
    }
    for (const fi of path.map(print, "imports")) {
        if (importDocs.length > 0) importDocs.push(hardline);
        importDocs.push(fi);
    }

    const functionDocs = serializeNewlineSep(context, ["functions"], doc.builders);

    if (importDocs.length === 0 && functionDocs.length === 0) {
        return printDanglingComments(context, doc.builders);
    }

    if (importDocs.length > 0 && functionDocs.length > 0) {
        return [...importDocs, hardline, hardline, ...functionDocs];
    }

    return [...importDocs, ...functionDocs];
}

/**
 * Prints an extension expression node.
 * For now just returns the text as-is.
 *
 * @param context The print context
 * @returns The formatted extension expression
 */
function printExtensionExpression(context: PrintContext<ExtensionExpressionType>): Doc {
    const cstNode = context.ctx.$cstNode;
    if (cstNode == undefined) {
        throw new Error("CST node is undefined for ExtensionExpression");
    }
    return cstNode.text;
}

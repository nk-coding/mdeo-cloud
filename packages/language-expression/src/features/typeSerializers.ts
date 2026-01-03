import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext, Builders } from "@mdeo/language-shared";
import { ID } from "@mdeo/language-common";
import { printDanglingComments, sharedImport } from "@mdeo/language-shared";
import type { TypeTypes, ClassTypeType, LambdaTypeType, LambdaTypeParametersType } from "../grammar/typeTypes.js";

const { doc: prettierDoc } = sharedImport("prettier");

/**
 * Registers all type serializers for pretty-printing type AST nodes.
 *
 * @param services The Langium core services with AST serializer
 * @param types The generated type types
 */
export function registerTypeSerializers(
    services: LangiumCoreServices & AstSerializerAdditionalServices,
    types: TypeTypes
): void {
    const { AstSerializer } = services;
    const builders = prettierDoc.builders;

    AstSerializer.registerNodeSerializer(types.classTypeType, (ctx) => printClassType(ctx, builders));
    AstSerializer.registerNodeSerializer(types.voidTypeType, () => printVoidType());
    AstSerializer.registerNodeSerializer(types.lambdaTypeParametersType, (ctx) =>
        printLambdaTypeParameters(ctx, builders)
    );
    AstSerializer.registerNodeSerializer(types.lambdaTypeType, (ctx) => printLambdaType(ctx));
}

/**
 * Prints a class type node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted class type
 */
function printClassType(context: PrintContext<ClassTypeType>, builders: Builders): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const { join } = builders;
    const docs: Doc[] = [printPrimitive(getPrimitive(ctx, "name"), ID)];

    if (ctx.typeArgs && ctx.typeArgs.length > 0) {
        docs.push("<");
        const typeArgs = path.map(print, "typeArgs");
        docs.push(join(", ", typeArgs));
        docs.push(">");
    }

    if (ctx.isNullable) {
        docs.push("?");
    }

    return docs;
}

/**
 * Prints a void type node.
 *
 * @returns The formatted void type
 */
function printVoidType(): Doc {
    return "void";
}

/**
 * Prints lambda type parameters node (with round brackets).
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted lambda type parameters
 */
function printLambdaTypeParameters(context: PrintContext<LambdaTypeParametersType>, builders: Builders): Doc {
    const { ctx, path, print } = context;
    const { join, line } = builders;
    const docs: Doc[] = ["("];

    if (ctx.parameters && ctx.parameters.length > 0) {
        const parameters = path.map(print, "parameters");
        docs.push(join(", ", parameters));
    } else {
        const danglingComments = printDanglingComments(context, builders);
        if (danglingComments.length > 0) {
            docs.push(line, danglingComments, line);
        }
    }

    docs.push(")");
    return docs;
}

/**
 * Prints a lambda type node.
 *
 * @param context The print context
 * @returns The formatted lambda type
 */
function printLambdaType(context: PrintContext<LambdaTypeType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    if (ctx.isNullable) {
        docs.push("(");
    }

    docs.push(path.call(print, "parameterList"), " => ", path.call(print, "returnType"));

    if (ctx.isNullable) {
        docs.push(")", "?");
    }

    return docs;
}

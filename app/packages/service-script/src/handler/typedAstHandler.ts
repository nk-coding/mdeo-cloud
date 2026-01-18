import { Script, type ScriptServices, type TypedAst } from "@mdeo/language-script";
import { hasErrors, type FileDataHandler } from "@mdeo/service-common";
import { TypedAstConverter } from "./typedAstConverter.js";

/**
 * Key for the typed AST handler.
 */
export const TYPED_AST_HANDLER_KEY = "typed-ast";

/**
 * Handler for computing the typed AST of a script file.
 * Converts the language AST into a typed representation suitable for code generation.
 *
 * @param context The file data context with path, content, and services
 * @returns Promise resolving to the file data result with typed AST
 */
export const typedAstHandler: FileDataHandler<TypedAst | null, ScriptServices> = async (context) => {
    const { uri, instance, version, serverApi } = context;

    if (version == undefined) {
        throw new Error("Typed AST handler does not support directory requests");
    }

    const document = await instance.buildDocument(uri);

    if (hasErrors(document)) {
        return {
            data: null,
            ...serverApi.getTrackedRequests()
        };
    }

    const script = document.parseResult.value;
    const reflection = instance.services.shared.AstReflection;
    if (!reflection.isInstance(script, Script)) {
        throw new Error("Document root is not a Script");
    }

    const converter = new TypedAstConverter(instance.services.typir, reflection);
    const typedRoot = converter.convertScript(script);

    return {
        data: typedRoot,
        ...serverApi.getTrackedRequests()
    };
};

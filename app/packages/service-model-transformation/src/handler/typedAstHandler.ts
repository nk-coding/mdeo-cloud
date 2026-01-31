import { type ModelTransformationServices, type TypedAst } from "@mdeo/language-model-transformation";
import { ModelTransformation } from "@mdeo/language-model-transformation";
import { hasErrors, type FileDataHandler } from "@mdeo/service-common";
import { ModelTransformationTypedAstConverter } from "./modelTransformationTypedAstConverter.js";

/**
 * Key for the typed AST handler.
 */
export const TYPED_AST_HANDLER_KEY = "typed-ast";

/**
 * Handler for computing the typed AST of a model transformation file.
 * Converts the language AST into a typed representation suitable for code generation.
 *
 * @param context The file data context with path, content, and services
 * @returns Promise resolving to the file data result with typed AST
 */
export const typedAstHandler: FileDataHandler<TypedAst | null, ModelTransformationServices> = async (context) => {
    const { instance, fileInfo, serverApi } = context;

    if (fileInfo == undefined) {
        return {
            data: null,
            ...serverApi.getTrackedRequests()
        };
    }

    const document = await instance.buildDocument(fileInfo.uri);

    if (hasErrors(document)) {
        return {
            data: null,
            ...serverApi.getTrackedRequests()
        };
    }

    const transformation = document.parseResult.value;
    const reflection = instance.services.shared.AstReflection;
    if (!reflection.isInstance(transformation, ModelTransformation)) {
        throw new Error("Document root is not a ModelTransformation");
    }

    const converter = new ModelTransformationTypedAstConverter(instance.services.typir, reflection);
    const typedRoot = await converter.convertModelTransformation(document, transformation);

    return {
        data: typedRoot,
        ...serverApi.getTrackedRequests()
    };
};

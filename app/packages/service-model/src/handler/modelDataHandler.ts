/**
 * @module modelDataHandler
 *
 * Handler for computing the ModelData representation of a model file.
 * Provides a lightweight, serializable format for model data exchange.
 */
import { type ModelData, type ModelServices, ModelDataConverter, Model } from "@mdeo/language-model";
import { hasErrors, type FileDataHandler } from "@mdeo/service-common";

/**
 * Key for the model data handler.
 */
export const MODEL_DATA_HANDLER_KEY = "model-data";

/**
 * Handler for computing the ModelData representation of a model file.
 * Converts the language AST into a lightweight format suitable for serialization.
 *
 * Returns null if the document has errors to ensure only valid models are returned.
 *
 * @param context The file data context with path, content, and services
 * @returns Promise resolving to the file data result with ModelData or null
 */
export const modelDataHandler: FileDataHandler<ModelData | null, ModelServices> = async (context) => {
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

    const model = document.parseResult.value;
    const reflection = instance.services.shared.AstReflection;

    if (!reflection.isInstance(model, Model)) {
        throw new Error("Document root is not a Model");
    }

    const converter = new ModelDataConverter(reflection);
    const modelData = converter.convertModel(model);

    return {
        data: modelData,
        ...serverApi.getTrackedRequests()
    };
};

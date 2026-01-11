import { type LanguagePlugin } from "@mdeo/language-common";
import { ModelTransformationRule, ModelTransformationTerminals } from "./grammar/modelTransformationRules.js";

/**
 * The plugin for the Model Transformation language.
 * Provides minimal language server functionality for .mt files.
 */
export const modelTransformationPlugin: LanguagePlugin<object> = {
    rootRule: ModelTransformationRule,
    additionalTerminals: ModelTransformationTerminals,
    module: {}
};

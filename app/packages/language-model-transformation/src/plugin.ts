import { type LangiumLanguagePlugin, type LangiumLanguagePluginProvider } from "@mdeo/language-common";
import { ModelTransformationRule, ModelTransformationTerminals } from "./grammar/modelTransformationRules.js";

/**
 * The plugin for the Model Transformation language.
 * Provides minimal language server functionality for .mt files.
 */
const modelTransformationPlugin: LangiumLanguagePlugin<object> = {
    rootRule: ModelTransformationRule,
    additionalTerminals: ModelTransformationTerminals,
    module: {}
};

/**
 * Provider for the Model Transformation language plugin.
 */
export const modelTransformationPluginProvider: LangiumLanguagePluginProvider<object> = {
    create(): LangiumLanguagePlugin<object> {
        return modelTransformationPlugin;
    }
};
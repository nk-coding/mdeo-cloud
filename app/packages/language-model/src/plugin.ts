import { type LangiumLanguagePlugin, type LangiumLanguagePluginProvider } from "@mdeo/language-common";
import { ModelRule, ModelTerminals } from "./grammar/modelRules.js";

/**
 * The plugin for the Model language.
 * Provides minimal language server functionality for .m files.
 */
const modelPlugin: LangiumLanguagePlugin<object> = {
    rootRule: ModelRule,
    additionalTerminals: ModelTerminals,
    module: {}
};

/**
 * Provider for the Model language plugin.
 */
export const modelPluginProvider: LangiumLanguagePluginProvider<object> = {
    create(): LangiumLanguagePlugin<object> {
        return modelPlugin;
    }
};
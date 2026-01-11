import { type LanguagePlugin } from "@mdeo/language-common";
import { ModelRule, ModelTerminals } from "./grammar/modelRules.js";

/**
 * The plugin for the Model language.
 * Provides minimal language server functionality for .m files.
 */
export const modelPlugin: LanguagePlugin<object> = {
    rootRule: ModelRule,
    additionalTerminals: ModelTerminals,
    module: {}
};

import { type LanguagePlugin } from "@mdeo/language-common";
import { ConfigRule, ConfigTerminals } from "./grammar/configRules.js";

/**
 * The plugin for the Config language.
 * Provides minimal language server functionality for .config files.
 */
export const configPlugin: LanguagePlugin<object> = {
    rootRule: ConfigRule,
    additionalTerminals: ConfigTerminals,
    module: {}
};

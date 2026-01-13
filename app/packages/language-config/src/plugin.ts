import { type LangiumLanguagePlugin, type LangiumLanguagePluginProvider } from "@mdeo/language-common";
import { ConfigRule, ConfigTerminals } from "./grammar/configRules.js";

/**
 * The plugin for the Config language.
 * Provides minimal language server functionality for .config files.
 */
const configPlugin: LangiumLanguagePlugin<object> = {
    rootRule: ConfigRule,
    additionalTerminals: ConfigTerminals,
    module: {}
};

/**
 * Provider for the Config language plugin.
 */
export const configPluginProvider: LangiumLanguagePluginProvider<object> = {
    create(): LangiumLanguagePlugin<object> {
        return configPlugin;
    }
};

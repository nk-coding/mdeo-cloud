import { MetaModelRule } from "./rules.js";
import { WS, ML_COMMENT, SL_COMMENT } from "./terminals.js";
import type { LanguagePluginProvider } from "@mdeo/language-common/src/plugin/languagePlugin.js";

export const metamodelPluginProvider: LanguagePluginProvider<{}> = {
    generate: (context) => ({
        rootRule: MetaModelRule,
        additionalTerminals: [WS, ML_COMMENT, SL_COMMENT],
        module: {}
    })
};

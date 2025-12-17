import { MetaModelRule } from "./rules.js";
import { type LanguagePluginProvider, WS, ML_COMMENT, SL_COMMENT } from "@mdeo/language-common";

export const metamodelPluginProvider: LanguagePluginProvider<{}> = {
    generate: (context) => ({
        rootRule: MetaModelRule,
        additionalTerminals: [WS, ML_COMMENT, SL_COMMENT],
        module: {}
    })
};

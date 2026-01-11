import { createRule, WS, ML_COMMENT, SL_COMMENT, HIDDEN_NEWLINE } from "@mdeo/language-common";
import { Config } from "./configTypes.js";

/**
 * Minimal root rule for Config language.
 * TODO: Expand with actual grammar rules.
 */
export const ConfigRule = createRule("ConfigRule")
    .returns(Config)
    .as(() => []);

/**
 * Additional terminals for the Config language.
 */
export const ConfigTerminals = [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT];

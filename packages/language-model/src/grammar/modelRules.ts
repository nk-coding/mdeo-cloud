import { createRule, WS, ML_COMMENT, SL_COMMENT, HIDDEN_NEWLINE } from "@mdeo/language-common";
import { Model } from "./modelTypes.js";

/**
 * Minimal root rule for Model language.
 * TODO: Expand with actual grammar rules.
 */
export const ModelRule = createRule("ModelRule")
    .returns(Model)
    .as(() => []);

/**
 * Additional terminals for the Model language.
 */
export const ModelTerminals = [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT];

import { createRule, WS, ML_COMMENT, SL_COMMENT, HIDDEN_NEWLINE } from "@mdeo/language-common";
import { ModelTransformation } from "./modelTransformationTypes.js";

/**
 * Minimal root rule for Model Transformation language.
 * TODO: Expand with actual grammar rules.
 */
export const ModelTransformationRule = createRule("ModelTransformationRule")
    .returns(ModelTransformation)
    .as(() => []);

/**
 * Additional terminals for the Model Transformation language.
 */
export const ModelTransformationTerminals = [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT];

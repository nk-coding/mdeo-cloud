import type { PartialAstNode } from "@mdeo/language-common";
import type { GeneratedModelType } from "./generatedModelTypes.js";

/**
 * Partial generated model type with optional domain properties.
 */
export type PartialGeneratedModel = PartialAstNode<GeneratedModelType>;

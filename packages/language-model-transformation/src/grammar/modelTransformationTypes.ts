import { createInterface, type ASTType } from "@mdeo/language-common";

/**
 * Model Transformation root interface.
 */
export const ModelTransformation = createInterface("ModelTransformation").attrs({});

/**
 * Model Transformation AST type.
 */
export type ModelTransformationType = ASTType<typeof ModelTransformation>;

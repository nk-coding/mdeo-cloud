import { createInterface, type ASTType } from "@mdeo/language-common";

/**
 * Model root interface.
 */
export const Model = createInterface("Model").attrs({});

/**
 * Model AST type.
 */
export type ModelType = ASTType<typeof Model>;

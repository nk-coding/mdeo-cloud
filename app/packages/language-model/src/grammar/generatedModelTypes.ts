import { createInterface, type ASTType } from "@mdeo/language-common";

/**
 * Generated model root interface.
 * Contains the raw JSON content as a string field.
 * This is a minimal AST for generated model files (.m_gen),
 * where the entire content is captured as a single JSON string.
 */
export const GeneratedModel = createInterface("GeneratedModel").attrs({
    content: String
});

/**
 * Generated model AST type.
 */
export type GeneratedModelType = ASTType<typeof GeneratedModel>;

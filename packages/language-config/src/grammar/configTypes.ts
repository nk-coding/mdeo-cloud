import { createInterface, type ASTType } from "@mdeo/language-common";

/**
 * Config root interface.
 */
export const Config = createInterface("Config").attrs({});

/**
 * Config AST type.
 */
export type ConfigType = ASTType<typeof Config>;

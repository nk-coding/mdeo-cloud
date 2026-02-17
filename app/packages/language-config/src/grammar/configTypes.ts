import { createInterface, type ASTType, type Interface } from "@mdeo/language-common";

/**
 * Base interface for all config sections.
 * Plugin sections should extend this interface.
 */
export const BaseConfigSection = createInterface("BaseConfigSection").attrs({});

/**
 * BaseConfigSection AST type.
 */
export type BaseConfigSectionType = ASTType<typeof BaseConfigSection>;

/**
 * Config root interface containing all sections.
 */
export const Config = createInterface("Config").attrs({
    sections: [BaseConfigSection]
});

/**
 * Config AST type.
 */
export type ConfigType = ASTType<typeof Config>;

/**
 * Helper type for getting a reference to the BaseConfigSection interface for extension
 */
export function getBaseConfigSectionInterface(): Interface<BaseConfigSectionType> {
    return BaseConfigSection;
}

import type { AstNode } from "langium";
import { createInterface } from "@mdeo/language-common";
import { Ref, Optional } from "@mdeo/language-common";
import type { FileScopingConfig } from "./config.js";

/**
 * Generates import-related type interfaces for a given entity type.
 *
 * @template T The AstNode type being imported
 * @param config Configuration for file-scoped composition
 * @returns An object containing the importType and fileImportType interfaces
 */
export function generateImportTypes<T extends AstNode>(config: FileScopingConfig<T>) {
    const importType = createInterface(config.importTypeName).attrs({
        entity: Ref(config.type),
        name: Optional(String)
    });
    const fileImportType = createInterface(config.fileImportTypeName).attrs({
        file: String,
        imports: [importType]
    });

    return { importType, fileImportType };
}

/**
 * Type representing a single import statement that references an entity.
 * Contains the entity reference and an optional alias name.
 *
 * @template T - The AstNode type being imported
 */
export type ImportType<T extends AstNode> = ReturnType<typeof generateImportTypes<T>>["importType"];

/**
 * Type representing a file import statement that contains multiple entity imports.
 * Follows the pattern: `import { entity1, entity2 as alias } from "file"`
 *
 * @template T - The AstNode type being imported
 */
export type FileImportType<T extends AstNode> = ReturnType<typeof generateImportTypes<T>>["fileImportType"];

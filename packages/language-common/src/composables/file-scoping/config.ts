import type { BaseType } from "../../grammar/type/types.js";
import type { AstNode } from "langium";

/**
 * Configuration for file-scoped composition.
 *
 * @template T - The element type that will be scoped by file.
 */
export class FileScopingConfig<T extends AstNode> {
    /**
     * Creates a new FileScopingConfig.
     *
     * @param prefix - Prefix for naming generated rules and types.
     * @param type - The element type handled by this scoping config.
     */
    constructor(
        readonly prefix: string,
        readonly type: BaseType<T>
    ) {}

    /**
     * Gets the name for the Import type/rule.
     * Format: `{prefix}Import`
     */
    get importTypeName(): string {
        return this.prefix + "Import";
    }

    /**
     * Gets the name for the FileImport type/rule.
     * Format: `{prefix}FileImport`
     */
    get fileImportTypeName(): string {
        return this.prefix + "FileImport";
    }

    /**
     * Gets the name for the Import rule.
     * Format: `{prefix}ImportRule`
     */
    get importRuleName(): string {
        return this.prefix + "ImportRule";
    }

    /**
     * Gets the name for the FileImport rule.
     * Format: `{prefix}FileImportRule`
     */
    get fileImportRuleName(): string {
        return this.prefix + "FileImportRule";
    }
}

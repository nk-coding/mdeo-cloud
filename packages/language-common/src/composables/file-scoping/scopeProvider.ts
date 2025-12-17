import type {
    AstNode,
    AstNodeDescriptionProvider,
    AstReflection,
    IndexManager,
    LangiumDocument,
    NameProvider,
    ReferenceInfo,
    Scope
} from "langium";
import type { FileScopingConfig } from "./config.js";
import type { ASTType, BaseType } from "../../grammar/type/types.js";
import type { FileImportType } from "./types.js";
import type { PluginContext } from "../../plugin/pluginContext.js";

/**
 * Determines if the given reference info represents an import statement.
 *
 * @param context The reference info to check, containing the reference property and container
 * @param config The file scoping configuration defining the import type name and structure
 * @returns `true` if the reference is an import statement, `false` otherwise
 */
export function isImportReference(context: ReferenceInfo, config: FileScopingConfig<any>): boolean {
    return context.property === "entity" && context.container.$type === config.importTypeName;
}

/**
 * Checks if the given reference info points to a (potentially) imported entity type.
 *
 * @param context The reference info to check, containing reference metadata
 * @param targetType The base type that is expected to be imported (e.g., a specific entity type)
 * @param astReflection The AST reflection service used to resolve reference types
 * @returns `true` if the reference type matches the target type name, `false` otherwise
 */
export function isReferenceToImport(
    context: ReferenceInfo,
    targetType: BaseType<any>,
    astReflection: AstReflection
): boolean {
    const referenceType = astReflection.getReferenceType(context);
    return referenceType === targetType.name;
}

/**
 * Retrieves exported entities from the global scope based on file imports.
 *
 * @template T - The AST node type of the entities being imported
 * @param context - The plugin context containing Langium utilities
 * @param context.langium - Langium services including UriUtils and StreamScope
 * @param document - The current document where the import statement appears
 * @param referenceInfo - Information about the reference, including the import container
 * @param config - File scoping configuration specifying the entity type and import structure
 * @param indexManager - The index manager used to query exported entities across files
 * @returns A scope containing all exported entities from the imported file that match the configured type
 * ```
 */
export function getExportedEntitiesFromGlobalScope<T extends AstNode>(
    { langium }: PluginContext,
    document: LangiumDocument,
    referenceInfo: ReferenceInfo,
    config: FileScopingConfig<T>,
    indexManager: IndexManager
): Scope {
    const currentUri = document.uri;
    const uris = new Set<string>();
    const dirname = langium.UriUtils.dirname(currentUri);
    const fileImport = referenceInfo.container.$container as ASTType<FileImportType<T>>;
    uris.add(langium.UriUtils.joinPath(dirname, fileImport.file).toString());
    const astNodeDescriptions = indexManager.allElements(config.type.name, uris).toArray();
    return new langium.StreamScope(langium.stream(astNodeDescriptions));
}

/**
 * Creates a scope containing all entities imported into the current file.
 * Does NOT handle locally defined entities; use createLocalScope for that.
 *
 * @template T - The AST node type of the entities being imported
 * @param context - The plugin context containing Langium utilities
 * @param context.langium - Langium services including StreamScope and stream utilities
 * @param fileImports - Array of all import statements in the current file
 * @param nameProvider - Service for retrieving the canonical name of AST nodes
 * @param descriptions - Provider for creating AST node descriptions
 *
 * @returns A scope containing descriptions of all imported entities accessible in the current file
 */
export function getImportedEntitiesFromCurrentFile<T extends AstNode>(
    { langium }: PluginContext,
    fileImports: ASTType<FileImportType<T>>[],
    nameProvider: NameProvider,
    descriptions: AstNodeDescriptionProvider
): Scope {
    const importDescriptions = fileImports
        .flatMap((fileImport) =>
            fileImport.imports.map((entityImport) => {
                if (entityImport.name != undefined) {
                    return descriptions.createDescription(entityImport, entityImport.name);
                }
                if (entityImport.entity.ref != undefined) {
                    return descriptions.createDescription(
                        entityImport.entity.ref,
                        nameProvider.getName(entityImport.entity.ref)
                    );
                }
                return undefined;
            })
        )
        .filter((description) => description != undefined);

    return new langium.StreamScope(langium.stream(importDescriptions));
}

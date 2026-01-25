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
import type { ASTType, BaseType } from "@mdeo/language-common";
import type { FileImportType, ImportType } from "./types.js";
import { sharedImport } from "../sharedImport.js";
import { resolveRelativePath } from "./util.js";

const { StreamScope, stream, EMPTY_SCOPE } = sharedImport("langium");

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
 * @template T The AST node type of the entities being imported
 * @param document The current document where the import statement appears
 * @param referenceInfo Information about the reference, including the import container
 * @param config File scoping configuration specifying the entity type and import structure
 * @param indexManager The index manager used to query exported entities across files
 * @returns A scope containing all exported entities from the imported file that match the configured type
 */
export function getExportedEntitiesFromGlobalScope<T extends AstNode>(
    document: LangiumDocument,
    referenceInfo: ReferenceInfo,
    config: FileScopingConfig<T>,
    indexManager: IndexManager
): Scope {
    return getExportetEntitiesFromRelativeFile(
        document,
        (referenceInfo.container.$container as ASTType<FileImportType<T>>).file,
        [config.type],
        indexManager
    );
}

/**
 * Retrieves exported entities from a file specified by a relative path.
 *
 * @template T The AST node type of the entities being imported
 * @param document The current document where the import statement appears
 * @param file The relative file path to import entities from
 * @param types The base type of the entities to retrieve
 * @param indexManager The index manager used to query exported entities across files
 * @returns A scope containing all exported entities from the specified file that match the given type
 */
export function getExportetEntitiesFromRelativeFile<T extends AstNode>(
    document: LangiumDocument,
    file: string | undefined,
    types: BaseType<T>[],
    indexManager: IndexManager
): Scope {
    if (file == undefined) {
        return EMPTY_SCOPE;
    }
    const targetUri = resolveRelativePath(document, file).toString();
    const astNodeDescriptions = types.flatMap((type) =>
        indexManager.allElements(type.name, new Set([targetUri])).toArray()
    );
    return new StreamScope(stream(astNodeDescriptions));
}

/**
 * Creates a scope containing all entities imported into the current file.
 * Does NOT handle locally defined entities; use createLocalScope for that.
 *
 * @template T The AST node type of the entities being imported
 * @param fileImports Array of all import statements in the current file
 * @param nameProvider Service for retrieving the canonical name of AST nodes
 * @param descriptions Provider for creating AST node descriptions
 * @param filter Optional filter function to include only specific imports
 * @returns A scope containing descriptions of all imported entities accessible in the current file
 */
export function getImportedEntitiesFromCurrentFile<T extends AstNode>(
    fileImports: ASTType<FileImportType<T>>[],
    nameProvider: NameProvider,
    descriptions: AstNodeDescriptionProvider,
    filter: (imp: ASTType<ImportType<T>>) => boolean = () => true
): Scope {
    const importDescriptions = fileImports
        .flatMap((fileImport) =>
            fileImport.imports.map((entityImport) => {
                if (!filter(entityImport)) {
                    return undefined;
                }
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

    return new StreamScope(stream(importDescriptions));
}

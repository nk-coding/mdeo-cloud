import {
    sharedImport,
    getExportedEntitiesFromGlobalScope,
    getImportedEntitiesFromCurrentFile,
    isImportReference,
    isReferenceToImport,
    createLocalScope
} from "@mdeo/language-shared";
import type { ReferenceInfo, Scope } from "langium";
import { MetaClassOrImport, metamodelFileScopingConfig, type MetaModelType } from "../grammar/metamodelTypes.js";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * The scope provider for the Metamodel language.
 *
 * This scope provider handles resolution of references to metaclasses, supporting both:
 * - Local references to classes defined in the same file
 * - Cross-file references via imports
 *
 * It distinguishes between:
 * - Import references (the entity being imported in an import statement)
 * - References to imported entities (usage of imported classes in the model)
 */
export class MetamodelScopeProvider extends DefaultScopeProvider {
    /**
     * Computes the scope for a given reference.
     *
     * This method is called during linking to resolve cross-references in the AST.
     * It provides different scopes based on the type of reference:
     *
     * 1. For import references (in import statements): Returns global scope with all
     *    exportable entities from all files in the workspace.
     *
     * 2. For references to metaclasses in the model: Returns a combined scope of
     *    locally defined classes and imported classes.
     *
     * @param referenceInfo Information about the reference to resolve, including
     *                      the container node, reference property, and index
     * @returns A Scope containing the valid target entities for this reference
     */
    override getScope(referenceInfo: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(referenceInfo.container);
        const model = document.parseResult.value as MetaModelType;

        if (isImportReference(referenceInfo, metamodelFileScopingConfig)) {
            return getExportedEntitiesFromGlobalScope(
                document,
                referenceInfo,
                metamodelFileScopingConfig,
                this.indexManager
            );
        } else if (isReferenceToImport(referenceInfo, MetaClassOrImport, this.reflection)) {
            const importScope = getImportedEntitiesFromCurrentFile(model.imports, this.nameProvider, this.descriptions);
            return createLocalScope(referenceInfo, document, this.reflection, importScope);
        }

        return EMPTY_SCOPE;
    }
}

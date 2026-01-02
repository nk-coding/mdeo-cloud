import {
    type PluginContext,
    getExportedEntitiesFromGlobalScope,
    getImportedEntitiesFromCurrentFile,
    isImportReference,
    isReferenceToImport
} from "@mdeo/language-common";
import type { ReferenceInfo, Scope, ScopeProvider } from "langium";
import type { MetamodelServiceProvider } from "../plugin.js";
import { MetaClassOrImport, metamodelFileScopingConfig, type MetaModelType } from "../grammar/types.js";
import { createLocalScope } from "@mdeo/language-common";

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
 *
 * @param context The plugin context providing access to language services
 * @returns A factory function that creates the scope provider instance
 */
export const MetamodelScopeProvider: MetamodelServiceProvider<ScopeProvider> = (context: PluginContext) => {
    class ScopeProvider extends context.langium.DefaultScopeProvider {
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
            const document = context.langium.AstUtils.getDocument(referenceInfo.container);
            const model = document.parseResult.value as MetaModelType;

            if (isImportReference(referenceInfo, metamodelFileScopingConfig)) {
                return getExportedEntitiesFromGlobalScope(
                    context,
                    document,
                    referenceInfo,
                    metamodelFileScopingConfig,
                    this.indexManager
                );
            } else if (isReferenceToImport(referenceInfo, MetaClassOrImport, this.reflection)) {
                const importScope = getImportedEntitiesFromCurrentFile(
                    context,
                    model.imports,
                    this.nameProvider,
                    this.descriptions
                );
                return createLocalScope(context, referenceInfo, document, this.reflection, importScope);
            }

            return context.langium.EMPTY_SCOPE;
        }
    }

    return (services) => new ScopeProvider(services);
};

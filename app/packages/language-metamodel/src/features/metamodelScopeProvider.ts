import {
    sharedImport,
    getExportedEntitiesFromGlobalScope,
    getImportedEntitiesFromCurrentFile,
    isImportReference,
    isReferenceToImport,
    createLocalScope
} from "@mdeo/language-shared";
import type { ReferenceInfo, Scope } from "langium";
import { ClassOrImport, metamodelFileScopingConfig, type MetaModelType } from "../grammar/metamodelTypes.js";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * The scope provider for the Metamodel language.
 *
 * This scope provider handles resolution of references to classes, supporting both:
 * - Local references to classes defined in the same file
 * - Cross-file references via imports
 *
 * It distinguishes between:
 * - Import references (the entity being imported in an import statement)
 * - References to imported entities (usage of imported classes in the model)
 */
export class MetamodelScopeProvider extends DefaultScopeProvider {
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
        } else if (isReferenceToImport(referenceInfo, ClassOrImport, this.reflection)) {
            const importScope = getImportedEntitiesFromCurrentFile(model.imports, this.nameProvider, this.descriptions);
            return createLocalScope(referenceInfo, document, this.reflection, importScope);
        }

        return EMPTY_SCOPE;
    }
}

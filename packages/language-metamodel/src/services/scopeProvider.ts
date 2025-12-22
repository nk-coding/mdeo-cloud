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
import { createLocalScope } from "@mdeo/language-common/src/composables/scoping/localScopeProvider.js";

export const MetamodelScopeProvider: MetamodelServiceProvider<ScopeProvider> = (context: PluginContext) => {
    class ScopeProvider extends context.langium.DefaultScopeProvider {
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

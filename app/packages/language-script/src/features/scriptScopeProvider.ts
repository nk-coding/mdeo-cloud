import { getExportedEntitiesFromGlobalScope, isImportReference, sharedImport } from "@mdeo/language-shared";
import type { ReferenceInfo, Scope } from "langium";
import { scriptFileScopingConfig } from "../grammar/scriptTypes.js";

const { DefaultScopeProvider, AstUtils, EMPTY_SCOPE } = sharedImport("langium");

/**
 * The (langium) scope provider for the Script language.
 *
 * This scope providers handles resolution of imports to other Script files.
 */
export class ScriptLangiumScopeProvider extends DefaultScopeProvider {
    override getScope(referenceInfo: ReferenceInfo): Scope {
        const document = AstUtils.getDocument(referenceInfo.container);

        if (isImportReference(referenceInfo, scriptFileScopingConfig)) {
            return getExportedEntitiesFromGlobalScope(
                document,
                referenceInfo,
                scriptFileScopingConfig,
                this.indexManager
            );
        }

        return EMPTY_SCOPE;
    }
}

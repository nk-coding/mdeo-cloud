import type { LangiumCoreServices } from "langium";
import { sharedImport } from "../sharedImport.js";
import type {
    ExternalReferenceAdditionalServices,
    ExternalReferenceSharedAdditionalServices
} from "@mdeo/language-common";

const { DocumentState } = sharedImport("langium");

/**
 * Adds a document build phase to collect and resolve external references.
 *
 * @param services The Langium core services extended with external reference services.
 */
export function addExternalReferenceCollectionPhase(
    services: LangiumCoreServices &
        ExternalReferenceAdditionalServices & { shared: ExternalReferenceSharedAdditionalServices }
) {
    const documentBuilder = services.shared.workspace.DocumentBuilder;
    const externalReferenceCollector = services.references.ExternalReferenceCollector;
    const externalReferenceResolver = services.shared.references.ExternalReferenceResolver;
    const LangiumDocuments = services.shared.workspace.LangiumDocuments;

    documentBuilder.onBuildPhase(DocumentState.IndexedContent, async (docs) => {
        const references = externalReferenceCollector.findExternalReferences(
            docs.filter((doc) => doc.textDocument.languageId === services.LanguageMetaData.languageId)
        );
        const missingReferences = references.filter((uri) => !LangiumDocuments.hasDocument(uri));
        await Promise.all(missingReferences.map((uri) => externalReferenceResolver.loadExternalDocument(uri)));
    });
}

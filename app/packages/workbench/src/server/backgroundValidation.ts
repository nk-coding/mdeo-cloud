import type { LangiumSharedCoreServices } from "langium";
import { DocumentState } from "langium";
import type { DependencyAwareDocumentBuilder } from "./dependencyAwareDocumentBuilder";
import type { ExternalReferenceCollector } from "@mdeo/language-common";
import type { ResolvedServerLanguagePlugin } from "./types";

/**
 * Sets up dependency tracking for the DependencyAwareDocumentBuilder.
 *
 * After each document reaches the Linked state, external references are collected
 * and stored in the builder's dependency index. This information is then used by
 * the builder's `shouldRelink` override to determine which documents need
 * re-validation when a file changes.
 *
 * @param shared The shared Langium services
 * @param resolvedPlugins The resolved language plugins (which may have ExternalReferenceCollectors)
 */
export function setupDependencyTracking(
    shared: LangiumSharedCoreServices,
    resolvedPlugins: ResolvedServerLanguagePlugin[]
): void {
    const documentBuilder = shared.workspace.DocumentBuilder as DependencyAwareDocumentBuilder;

    const collectorsByLanguage = new Map<string, ExternalReferenceCollector>();
    for (const plugin of resolvedPlugins) {
        if (plugin.services) {
            const collector = (plugin.services as Record<string, any>).references?.ExternalReferenceCollector as
                | ExternalReferenceCollector
                | undefined;
            if (collector) {
                collectorsByLanguage.set(plugin.id, collector);
            }
        }
    }

    documentBuilder.onBuildPhase(DocumentState.Linked, async (docs) => {
        for (const doc of docs) {
            const languageId = doc.textDocument.languageId;
            const collector = collectorsByLanguage.get(languageId);
            if (!collector) {
                continue;
            }

            const refs = collector.findExternalReferences([doc]);
            const depUris = [...refs.local, ...refs.external].map((uri) => uri.toString());
            documentBuilder.setDependencies(doc.uri.toString(), depUris);
        }
    });

    documentBuilder.onUpdate(async (_changed, deleted) => {
        for (const deletedUri of deleted) {
            documentBuilder.removeDependencies(deletedUri.toString());
        }
    });
}

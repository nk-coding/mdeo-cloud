import type { LangiumCoreServices } from "langium";
import type {
    ExternalReferenceAdditionalServices,
    ExternalReferenceCollector,
    ExternalReferences
} from "@mdeo/language-common";
import type { LangiumDocument, URI } from "langium";
import type { ResolvedConfigContributionPlugins } from "../plugin/resolvePlugins.js";
import { getServicesByLanguageId } from "./util.js";

/**
 * A delegating external reference collector for the config language.
 *
 * Forwards reference collection to each contribution plugin's own
 * {@link ExternalReferenceCollector}, looked up at call-time from the
 * {@link ServiceRegistry}. This mirrors the pattern used by
 * {@link ConfigDelegatingScopeProvider} and
 * {@link ConfigDelegatingCompletionProvider}.
 *
 * Results from all plugins are merged and deduplicated before being returned.
 */
export class ConfigDelegatingExternalReferenceCollector implements ExternalReferenceCollector {
    private readonly services: LangiumCoreServices;

    /**
     * Unique plugin language keys derived from the resolved sections.
     */
    private readonly pluginLanguageKeys: ReadonlySet<string>;

    constructor(services: LangiumCoreServices, resolvedPlugins: ResolvedConfigContributionPlugins) {
        this.services = services;

        const keys = new Set<string>();
        for (const sectionInfo of resolvedPlugins.sections.values()) {
            keys.add(sectionInfo.plugin.languageKey);
        }
        this.pluginLanguageKeys = keys;
    }

    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const local: URI[] = [];
        const external: URI[] = [];
        const localSeen = new Set<string>();
        const externalSeen = new Set<string>();

        const registry = this.services.shared.ServiceRegistry;

        for (const languageKey of this.pluginLanguageKeys) {
            const pluginServices = getServicesByLanguageId(registry, languageKey) as
                | (LangiumCoreServices & Partial<ExternalReferenceAdditionalServices>)
                | undefined;
            const collector = pluginServices?.references?.ExternalReferenceCollector;
            if (collector == undefined) {
                continue;
            }

            const refs = collector.findExternalReferences(docs);

            for (const uri of refs.local) {
                const key = uri.toString();
                if (!localSeen.has(key)) {
                    localSeen.add(key);
                    local.push(uri);
                }
            }
            for (const uri of refs.external) {
                const key = uri.toString();
                if (!externalSeen.has(key)) {
                    externalSeen.add(key);
                    external.push(uri);
                }
            }
        }

        return { local, external };
    }
}

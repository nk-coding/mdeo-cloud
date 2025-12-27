import type { PluginContext } from "@mdeo/language-common";
import type { TypirLangiumServices, TypirLangiumSpecifics } from "typir-langium";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { ScopeProviderCaching } from "../scope/scopeProviderCache.js";
import type { Scope } from "../scope/scope.js";
import type { DocumentCache } from "langium";

/**
 * Generates a ScopeProviderCache class that uses Langium's DocumentCache
 * to cache scope providers for language nodes.
 *
 * @param context The plugin context providing access to shared dependencies
 * @returns An object containing the ScopeProviderCache class provider
 */
export function generateScopeProviderCache<Specifics extends TypirLangiumSpecifics>(
    context: PluginContext
): {
    ScopeProvider: (
        services: ExtendedTypirServices<Specifics> & TypirLangiumServices<Specifics>
    ) => ScopeProviderCaching<Specifics>;
} {
    const { langium, ["typir-langium"]: typirLangium } = context;

    class LangiumScopeProviderCache implements ScopeProviderCaching<Specifics> {
        protected readonly cache: DocumentCache<unknown, Scope<Specifics>>;

        constructor(services: ExtendedTypirServices<Specifics> & TypirLangiumServices<Specifics>) {
            this.cache = new langium.DocumentCache(
                services.langium.LangiumServices as any,
                langium.DocumentState.IndexedReferences
            );
        }

        cacheSet(languageNode: Specifics["LanguageType"], scope: Scope<Specifics>): void {
            this.cache.set(typirLangium.getDocumentKey(languageNode), languageNode, scope);
        }

        cacheGet(languageNode: Specifics["LanguageType"]): Scope<Specifics> | undefined {
            return this.cache.get(typirLangium.getDocumentKey(languageNode), languageNode);
        }

        cacheClear(): void {
            this.cache.clear();
        }
    }

    return {
        ScopeProvider: (services) => new LangiumScopeProviderCache(services)
    };
}

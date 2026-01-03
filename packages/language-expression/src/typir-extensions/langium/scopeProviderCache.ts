import { sharedImport } from "@mdeo/language-shared";
import type { TypirLangiumServices, TypirLangiumSpecifics } from "typir-langium";
import type { ExtendedTypirServices } from "../service/extendedTypirServices.js";
import type { ScopeProviderCaching } from "../scope/scopeProviderCache.js";
import type { Scope } from "../scope/scope.js";
import type { DocumentCache as DocumentCacheType } from "langium";

const { DocumentCache, DocumentState } = sharedImport("langium");
const { getDocumentKey } = sharedImport("typir-langium");

/**
 * Generates a ScopeProviderCache class that uses Langium's DocumentCache
 * to cache scope providers for language nodes.
 *
 * @returns An object containing the ScopeProviderCache class provider
 */
export function generateScopeProviderCache<Specifics extends TypirLangiumSpecifics>(): {
    ScopeProvider: (
        services: ExtendedTypirServices<Specifics> & TypirLangiumServices<Specifics>
    ) => ScopeProviderCaching<Specifics>;
} {
    class LangiumScopeProviderCache implements ScopeProviderCaching<Specifics> {
        protected readonly cache: DocumentCacheType<unknown, Scope<Specifics>>;

        constructor(services: ExtendedTypirServices<Specifics> & TypirLangiumServices<Specifics>) {
            this.cache = new DocumentCache(services.langium.LangiumServices as any, DocumentState.IndexedReferences);
        }

        cacheSet(languageNode: Specifics["LanguageType"], scope: Scope<Specifics>): void {
            this.cache.set(getDocumentKey(languageNode), languageNode, scope);
        }

        cacheGet(languageNode: Specifics["LanguageType"]): Scope<Specifics> | undefined {
            return this.cache.get(getDocumentKey(languageNode), languageNode);
        }

        cacheClear(): void {
            this.cache.clear();
        }
    }

    return {
        ScopeProvider: (services) => new LangiumScopeProviderCache(services)
    };
}

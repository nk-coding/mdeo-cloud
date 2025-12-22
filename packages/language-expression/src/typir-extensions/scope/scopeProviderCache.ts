import type { Scope } from "./scope.js";

export interface ScopeProviderCaching {
    cacheSet(languageNode: unknown, scope: Scope): void;
    cacheGet(languageNode: unknown): Scope | undefined;
    cacheClear(): void;
}

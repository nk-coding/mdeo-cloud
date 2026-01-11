import type { TypirSpecifics } from "typir";
import type { Scope } from "./scope.js";

/**
 * Caches computed scopes for language nodes to avoid recomputation.
 *
 * @template Specifics The Typir-specific types used by the Scope.
 */
export interface ScopeProviderCaching<Specifics extends TypirSpecifics> {
    /**
     * Store a computed scope for a language node.
     *
     * @param languageNode The language AST node (opaque) for which the scope applies.
     * @param scope The computed Scope instance to cache.
     */
    cacheSet(languageNode: unknown, scope: Scope<Specifics>): void;

    /**
     * Retrieve a cached scope for a language node.
     *
     * @param languageNode The language AST node (opaque) whose cached scope is requested.
     * @returns The cached Scope if present, otherwise undefined.
     */
    cacheGet(languageNode: unknown): Scope<Specifics> | undefined;

    /**
     * Clear all cached scopes.
     */
    cacheClear(): void;
}

import type { TypirSpecifics } from "typir";
import type { BoundScope } from "./scope.js";

/**
 * Interface for a ScopeProvider that provides scopes for language AST nodes.
 */
export interface ScopeProvider<Specifics extends TypirSpecifics> {
    /**
     * Gets a scope for the given language node.
     *
     * @param languageNode The language AST node (opaque) for which the scope is requested.
     * @returns The Scope instance for the given language node.
     */
    getScope(languageNode: Specifics["LanguageType"]): BoundScope<Specifics>;
}

/**
 * Default scope provider which does not implement any scope logic.
 */
export class DefaultScopeProvider<Specifics extends TypirSpecifics> implements ScopeProvider<Specifics> {
    getScope(): BoundScope<Specifics> {
        throw new Error("Method not implemented.");
    }
}

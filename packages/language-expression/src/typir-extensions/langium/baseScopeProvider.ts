import type { TypirLangiumSpecifics } from "typir-langium";
import type { ScopeProvider } from "../scope/scopeProvider.js";
import { DefaultBoundScope, type BoundScope, type Scope } from "../scope/scope.js";
import type { ExtendedTypirLangiumServices } from "../service/extendedTypirServices.js";
import type { ScopeProviderCaching } from "../scope/scopeProviderCache.js";
import type { ExtendedTypeSystemDefinition } from "./typeSystemDefinition.js";

/**
 * Abstract base implementation of a scope provider for Typir-Langium integration.
 *
 * Provides scope resolution with caching support by traversing the AST hierarchy
 * to find scope-relevant nodes and build a chain of nested scopes.
 *
 * @template Specifics The language-specific Typir-Langium configuration.
 * @template S The extended Typir-Langium services type.
 */
export abstract class BaseScopeProvider<
    Specifics extends TypirLangiumSpecifics,
    S extends ExtendedTypirLangiumServices<Specifics>
> implements ScopeProvider<Specifics>
{
    /**
     * Caching mechanism for storing and retrieving scopes associated with language nodes.
     */
    private readonly caching: ScopeProviderCaching<Specifics>;

    /**
     * The type system definition containing the global scope and type system configuration.
     */
    private readonly typeSystemDefinition: ExtendedTypeSystemDefinition<Specifics>;

    /**
     * Constructs a new BaseScopeProvider instance.
     *
     * @param typir The Typir services including Langium-specific services for scope management.
     */
    constructor(protected readonly typir: S) {
        this.caching = typir.caching.ScopeProvider;
        this.typeSystemDefinition = typir.langium.TypeSystemDefinition as ExtendedTypeSystemDefinition<Specifics>;
    }

    /**
     * Retrieves the scope for a given language node.
     *
     * Traverses the AST hierarchy upward to find the closest scope-relevant node,
     * then builds or retrieves the scope chain from the cache. If no scope-relevant
     * node is found, returns the global scope.
     *
     * @param languageNode The language AST node for which to retrieve the scope.
     * @returns The scope associated with the language node.
     */
    getScope(languageNode: Specifics["LanguageType"]): BoundScope<Specifics> {
        const closestScope = this.findClosestScopeNode(languageNode);
        if (closestScope == undefined) {
            return new DefaultBoundScope(this.typeSystemDefinition.globalScope, -1);
        }
        const cachedScope = this.caching.cacheGet(closestScope.node);
        if (cachedScope != undefined) {
            return new DefaultBoundScope(cachedScope, closestScope.position ?? -1);
        }
        const parentScopeNode = closestScope.node.$container as Specifics["LanguageType"] | undefined;
        let parentScope: BoundScope<Specifics>;
        if (parentScopeNode != undefined) {
            parentScope = this.getScope(parentScopeNode);
        } else {
            parentScope = new DefaultBoundScope(this.typeSystemDefinition.globalScope, -1);
        }

        const newScope = this.createScope(closestScope.node, parentScope);
        this.caching.cacheSet(closestScope.node, newScope);
        return new DefaultBoundScope(newScope, closestScope.position ?? -1);
    }

    /**
     * Finds the closest ancestor node that is scope-relevant.
     *
     * Traverses upward through the AST starting from the given node until
     * a scope-relevant node is found or the root is reached.
     *
     * @param node The starting language AST node.
     * @returns The closest scope-relevant ancestor node, or undefined if none is found, and the previous node
     */
    private findClosestScopeNode(node: Specifics["LanguageType"]):
        | {
              node: Specifics["LanguageType"];
              position: number | undefined;
          }
        | undefined {
        let currentNode: Specifics["LanguageType"] | undefined = node;
        let position: number | undefined = undefined;
        while (currentNode != undefined) {
            if (this.isScopeRelevantNode(currentNode)) {
                return {
                    node: currentNode,
                    position: position
                };
            }
            position = currentNode.$containerIndex;
            currentNode = currentNode.$container as Specifics["LanguageType"] | undefined;
        }
        return undefined;
    }

    /**
     * Checks if the given node will create a scope.
     *
     * Subclasses must implement this method to determine which AST nodes
     * define new scopes in their language (e.g., functions, blocks, classes).
     *
     * @param node The language AST node to check.
     * @returns True if the node defines a scope, false otherwise.
     */
    abstract isScopeRelevantNode(node: Specifics["LanguageType"]): boolean;

    /**
     * Creates a scope for the given language node.
     * Will only be called for nodes where isScopeRelevantNode returned true.
     *
     * @param languageNode The language AST node for which to create the scope.
     * @param parentScope The parent scope, if any.
     * @returns The created Scope instance.
     */
    abstract createScope(languageNode: Specifics["LanguageType"], parentScope: BoundScope<Specifics>): Scope<Specifics>;
}

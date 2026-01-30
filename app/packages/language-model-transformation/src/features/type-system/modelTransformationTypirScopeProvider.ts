import {
    BaseScopeProvider,
    DefaultScope,
    type BoundScope,
    type Scope,
    type ExpressionTypirServices
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";

/**
 * Typir scope provider for the Model Transformation language.
 * Provides scoping for type inference within transformation constructs.
 *
 * Infrastructure is set up but implementation is deferred
 * as scoping rules are complex and require further specification.
 */
export class ModelTransformationTypirScopeProvider extends BaseScopeProvider<
    TypirLangiumSpecifics,
    ExpressionTypirServices<TypirLangiumSpecifics>
> {
    /**
     * Creates an instance of ModelTransformationTypirScopeProvider.
     *
     * @param typir The typir services for Model Transformation.
     */
    constructor(typir: ExpressionTypirServices<TypirLangiumSpecifics>) {
        super(typir);
    }

    /**
     * Determines if a node creates a new scope for type inference.
     * Infrastructure ready - implementation to be added when scoping rules are specified.
     *
     * @param node The AST node to check.
     * @returns True if the node creates a new scope.
     */
    override isScopeRelevantNode(node: TypirLangiumSpecifics["LanguageType"]): boolean {
        // Infrastructure ready for future implementation
        return false;
    }

    /**
     * Creates a scope for a given node.
     * Infrastructure ready - implementation to be added when scoping rules are specified.
     *
     * @param languageNode The AST node to create a scope for.
     * @param parentScope The parent scope.
     * @returns A new scope for the node.
     */
    override createScope(
        languageNode: TypirLangiumSpecifics["LanguageType"],
        parentScope: BoundScope<TypirLangiumSpecifics> | undefined
    ): Scope<TypirLangiumSpecifics> {
        // Infrastructure ready for future implementation
        // Return a default empty scope
        return new DefaultScope<TypirLangiumSpecifics>(
            parentScope,
            () => [],
            () => [],
            [],
            languageNode
        );
    }
}

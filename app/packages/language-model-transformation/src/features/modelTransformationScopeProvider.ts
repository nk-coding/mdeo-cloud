import { sharedImport } from "@mdeo/language-shared";
import type { LangiumCoreServices, ReferenceInfo, Scope } from "langium";

const { DefaultScopeProvider } = sharedImport("langium");

/**
 * Langium scope provider for the Model Transformation language.
 * Handles cross-file reference resolution for imports.
 *
 * Infrastructure is set up but implementation is deferred
 * as scoping rules are complex and require further specification.
 */
export class ModelTransformationLangiumScopeProvider extends DefaultScopeProvider {
    /**
     * Creates an instance of ModelTransformationLangiumScopeProvider.
     *
     * @param services The language services.
     */
    constructor(services: LangiumCoreServices) {
        super(services);
    }

    /**
     * Gets the scope for a reference.
     * Infrastructure ready - delegates to default scoping for now.
     * Implementation to be added when scoping rules are fully specified.
     *
     * @param referenceInfo The reference information to resolve.
     * @returns The scope containing potential reference targets.
     */
    override getScope(referenceInfo: ReferenceInfo): Scope {
        // Infrastructure ready for future implementation
        // Delegate to default scoping for basic reference resolution
        return super.getScope(referenceInfo);
    }
}

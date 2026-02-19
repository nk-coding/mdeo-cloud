import type { LangiumDocument, URI } from "langium";
import type { MdeoMetamodelResolver } from "@mdeo/language-config-mdeo";

/**
 * Service-side implementation of MdeoMetamodelResolver.
 * This resolver receives the metamodel URI from the request context
 * rather than looking it up from the problem section.
 *
 * This is necessary because the service operates on partial config documents
 * and may not have access to the optimization plugin's problem section.
 */
export class ServiceMdeoMetamodelResolver implements MdeoMetamodelResolver {
    private metamodelUri: URI | undefined;

    /**
     * Sets the metamodel URI from the request.
     * This should be called before processing a request.
     *
     * @param uri The metamodel file URI
     */
    setMetamodelData(uri: URI | undefined): void {
        this.metamodelUri = uri;
    }

    /**
     * Clears the metamodel data.
     * Called after processing a request to clean up state.
     */
    clearMetamodelData(): void {
        this.metamodelUri = undefined;
    }

    /**
     * Gets the URI of the metamodel file.
     *
     * @param _document The current document (unused in service context)
     * @returns The metamodel URI
     */
    getMetamodelUri(_document: LangiumDocument): URI | undefined {
        return this.metamodelUri;
    }
}

import type { LangiumDocument, URI } from "langium";

/**
 * Service for resolving the metamodel used in MDEO sections.
 * This service abstracts the metamodel URI resolution logic so it can be
 * implemented differently in the browser (using the problem section)
 * and in the service (using data provided in the request).
 */
export interface MdeoMetamodelResolver {
    /**
     * Returns the URI of the metamodel file for external reference collection
     * and scope resolution.
     *
     * @param document The current document
     * @returns The metamodel URI, or undefined if not resolvable
     */
    getMetamodelUri(document: LangiumDocument): URI | undefined;
}

/**
 * Symbol for the MdeoMetamodelResolver service in the dependency injection container.
 */
export const MdeoMetamodelResolver = Symbol("MdeoMetamodelResolver");

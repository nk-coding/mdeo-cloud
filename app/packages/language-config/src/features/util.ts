import type { LangiumCoreServices, ServiceRegistry } from "langium";

/**
 * Gets a language services by its language ID from the Langium ServiceRegistry.
 *
 * @param registry The Langium service registry
 * @param languageId The language ID to find
 * @returns The language services, or undefined if not found
 */
export function getServicesByLanguageId(
    registry: ServiceRegistry,
    languageId: string
): LangiumCoreServices | undefined {
    // @ts-expect-error - Accessing protected property to get services by language ID
    return registry.languageIdMap.get(languageId);
}

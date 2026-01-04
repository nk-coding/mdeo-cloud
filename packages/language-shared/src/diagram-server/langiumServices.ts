import type { MetadataFileSystemProviderAdditionalServices } from "@mdeo/language-common";
import type { LangiumCoreServices } from "langium";

/**
 * Langium services injected into the GLSP server injection key.
 */
export const LangiumServices = Symbol("LangiumServices");

/**
 * Langium services injected into the GLSP server.
 */
export type LangiumServices = LangiumCoreServices & MetadataFileSystemProviderAdditionalServices 
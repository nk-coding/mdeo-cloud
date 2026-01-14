import type { LangiumLanguagePluginProvider } from "@mdeo/language-common";
import { HttpServerApi } from "../service/serverApi.js";

/**
 * Additional shared services for the service context
 */
export interface ServiceAdditionalSharedServices {
    /**
     * Server API for backend communication
     */
    ServerApi: HttpServerApi;
}

/**
 * Additional services for the service context
 */
export interface ServiceAdditionalServices {
    shared: ServiceAdditionalSharedServices;
}

/**
 * Configuration for the Langium instance pool
 */
export interface LangiumPoolConfig {
    /**
     * Maximum number of instances to keep in the pool
     */
    maxInstances: number;

    /**
     * The language plugin to use for creating instances
     */
    languagePluginProvider: LangiumLanguagePluginProvider<any>;

    /**
     * The language ID
     */
    languageId: string;

    /**
     * The file extension for this language
     */
    extension: string;

    /**
     * The server API to use for file operations
     */
    backendUrl: string;
}

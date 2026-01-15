import type { LangiumLanguagePluginProvider } from "@mdeo/language-common";
import type { HttpServerApi } from "../service/serverApi.js";
import type { JsonAstSerializer } from "./jsonAstSerializer.js";
import type { ExtendedIndexManager } from "./extendedIndexManager.js";

/**
 * Additional shared services for the service context
 */
export interface ServiceAdditionalSharedServices {
    /**
     * Server API for backend communication
     */
    ServerApi: HttpServerApi;
    serializer: {
        /**
         * The JSON AST serializer for serializing and deserializing ASTs
         */
        JsonAstSerializer: JsonAstSerializer;
    };
    workspace: {
        /**
         * The extended index manager for managing the language index
         */
        IndexManager: ExtendedIndexManager;
    };
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

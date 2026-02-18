import type { LangiumLanguagePluginProvider, LanguageServices, PartialLanguageServices } from "@mdeo/language-common";
import type { HttpServerApi } from "../service/serverApi.js";
import type { JsonAstSerializer } from "./jsonAstSerializer.js";
import type { ExtendedIndexManager } from "./extendedIndexManager.js";
import type { DeepPartial, Module } from "langium";

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
export interface LangiumPoolConfig<T = object> {
    /**
     * Maximum number of instances to keep in the pool
     */
    maxInstances: number;

    /**
     * The language plugin to use for creating instances
     */
    languagePluginProvider: LangiumLanguagePluginProvider<T>;

    /**
     * Optional service-specific module
     */
    serviceModule: Module<LanguageServices & T, PartialLanguageServices & DeepPartial<T>> | undefined;

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

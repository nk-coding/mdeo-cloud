import type { LangiumLanguagePluginProvider, LanguageServices } from "@mdeo/language-common";
import type { Plugin, LanguagePlugin } from "@mdeo/plugin";
import type { FastifyInstance } from "fastify";
import type { ServerApi } from "./serverApi.js";
import type { URI } from "vscode-uri";
import type { LangiumInstance } from "../langium/langiumInstance.js";

/**
 * Plugin definition for the service.
 * This is the Plugin interface from @mdeo/plugin with the 'url' field omitted
 * since the URL is derived from the service's deployment.
 */
export type ServicePlugin = Omit<Plugin, "url">;

/**
 * Definition for constructing a service plugin.
 */
export interface ServicePluginDefinition extends Omit<ServicePlugin, "languagePlugins"> {
    /**
     * Language plugins provided by the service plugin
     */
    languagePlugin: LanguagePlugin;
}

/**
 * Handler for file data computation requests
 * @template T The return type of the handler
 */
export type FileDataHandler<T = unknown> = (context: FileDataContext) => Promise<FileDataResult<T>>;

/**
 * Context provided to file data handlers
 */
export interface FileDataContext {
    /**
     * The URI of the file, already registered in the Langium workspace
     */
    uri: URI;

    /**
     * The version of the file
     */
    version: number;

    /**
     * The Langium instance to use for processing
     */
    instance: LangiumInstance;

    /**
     * The Langium services for this language
     */
    services: LanguageServices;

    /**
     * Server API for accessing backend endpoints
     */
    serverApi: ServerApi;
}

/**
 * Result of a file data computation
 * @template T The type of the computed data
 */
export interface FileDataResult<T = unknown> {
    /**
    * The computed data (JSON value)
     */
    data: T;

    /**
     * File dependencies (paths and versions)
     */
    fileDependencies?: FileDependency[];

    /**
     * Data dependencies (other file data computations)
     */
    dataDependencies?: DataDependency[];

    /**
     * Additional file data computed as part of this request
     */
    additionalFileData?: AdditionalFileData[];
}

/**
 * File dependency information
 */
export interface FileDependency {
    path: string;
    version: number;
}

/**
 * Data dependency information
 */
export interface DataDependency {
    path: string;
    key: string;
    version: number;
}

/**
 * Additional file data computed as part of another computation
 */
export interface AdditionalFileData {
    path: string;
    key: string;
    data: any;
    sourceVersion: number;
    fileDependencies?: FileDependency[];
    dataDependencies?: DataDependency[];
}

/**
 * Configuration for the language service
 */
export interface ServiceConfig {
    /**
     * Port to run the service on
     */
    port: number;

    /**
     * Host to bind to (default: '0.0.0.0')
     */
    host?: string;

    /**
     * Maximum number of Langium instances to keep in the pool
     */
    maxLangiumInstances?: number;

    /**
     * Base URL of the backend API for ServerApi
     */
    backendApiUrl: string;

    /**
     * Plugin definition for this service
     */
    plugin: ServicePluginDefinition;

    /**
     * Langium language plugin provider
     */
    languagePluginProvider: LangiumLanguagePluginProvider<any>;

    /**
     * File data handlers keyed by data key (e.g., "ast", "diagram")
     */
    handlers: Record<string, FileDataHandler>;

    /**
     * Whether to serve static files (default: true)
     */
    serveStatic?: boolean;

    /**
     * Path to static files to serve (default: dist directory)
     */
    staticPath?: string;
}

/**
 * Directory entry returned by listDirectory
 */
export interface DirectoryEntry {
    name: string;
    isFile: boolean;
    isDirectory: boolean;
}

/**
 * Request body for file data computation
 */
export interface FileDataComputeRequest {
    path: string;
    project: string;
    version: number;
    content: string;
    contributionPlugins?: object[];
}

/**
 * Response body for file data computation
 */
export interface FileDataComputeResponse {
    data: any;
    fileDependencies: FileDependency[];
    dataDependencies: DataDependency[];
    additionalFileData: AdditionalFileData[];
}

/**
 * Factory function type for creating a language service
 */
export type ServiceFactory = (fastify: FastifyInstance) => Promise<void>;

/**
 * Contribution plugin configuration passed in requests
 */
export interface ContributionPluginConfig {
    [key: string]: unknown;
}

/**
 * Key for identifying a Langium instance based on contribution plugins
 */
export type ContributionPluginKey = string;

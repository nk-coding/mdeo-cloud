import type { LangiumLanguagePluginProvider } from "@mdeo/language-common";
import type { Plugin, LanguagePlugin } from "@mdeo/plugin";
import type { FastifyInstance } from "fastify";
import type { FileDataHandler, FileDataResult } from "../handler/types.js";

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
 * Configuration for the language service
 *
 * @template T Type of additional services
 */
export interface ServiceConfig<T = object> {
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
    languagePluginProvider: LangiumLanguagePluginProvider<T>;

    /**
     * File data handlers keyed by data key (e.g., "ast", "diagram")
     */
    handlers: Record<string, FileDataHandler<unknown, T>>;

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
    /**
     * Name of the entry
     */
    name: string;
    /**
     * Whether the entry is a file or directory
     */
    isFile: boolean;
    /**
     * Whether the entry is a directory
     */
    isDirectory: boolean;
}

/**
 * Request body for file data computation
 */
export interface FileDataComputeRequest {
    /**
     * Path to the file
     */
    path: string;
    /**
     * Project identifier for the project the file belongs to
     */
    project: string;
    /**
     * Version of the file
     */
    version: number;
    /**
     * Content of the file
     */
    content: string;
    /**
     * Contribution plugins to apply for this request
     */
    contributionPlugins?: object[];
}

/**
 * Response body for file data computation
 */
export type FileDataComputeResponse<T = unknown> = Required<FileDataResult<T>>;

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

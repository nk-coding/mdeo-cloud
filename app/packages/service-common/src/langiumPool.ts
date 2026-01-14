import {
    inject,
    type LangiumCoreServices,
    type LangiumGeneratedCoreServices,
    type LangiumGeneratedSharedCoreServices,
    type LangiumSharedCoreServices,
    type LanguageMetaData,
    type Module
} from "langium";
import type {
    LangiumLanguagePlugin,
    LanguageServices,
    PluginContext,
    ExternalReferenceSharedAdditionalServices
} from "@mdeo/language-common";
import type { ServerContributionPlugin } from "@mdeo/plugin";
import { createModule } from "@mdeo/language-common";
import type { HttpServerApi } from "./serverApi.js";
import type { ContributionPluginKey } from "./types.js";
import { createDefaultModule, createDefaultSharedModule } from "langium/lsp";
import * as langium from "langium";
import * as langiumGrammar from "langium/grammar";

/**
 * A managed Langium instance with its services and state
 */
export interface LangiumInstance {
    /**
     * The Langium services for this instance
     */
    services: LanguageServices;

    /**
     * Key identifying the contribution plugin configuration
     */
    contributionPluginKey: ContributionPluginKey;

    /**
     * Whether this instance is currently processing a request
     */
    busy: boolean;

    /**
     * Promise that resolves when the instance becomes available
     * Used as a mutex for waiting on busy instances
     */
    busyPromise: Promise<void> | null;

    /**
     * Resolver function for the busy promise
     */
    busyResolver: (() => void) | null;

    /**
     * Timestamp of last use (for LRU eviction)
     */
    lastUsed: number;
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
    languagePlugin: LangiumLanguagePlugin<any>;

    /**
     * The language ID
     */
    languageId: string;

    /**
     * The file extension for this language
     */
    extension: string;

    /**
     * The server API for backend communication
     */
    serverApi: HttpServerApi;
}

/**
 * Manages a pool of Langium instances for handling file data requests.
 *
 * Each instance is tied to a specific contribution plugin configuration.
 * Instances are reused when the same configuration is requested, and
 * evicted based on LRU when the pool is full.
 */
export class LangiumInstancePool {
    private readonly instances: Map<string, LangiumInstance> = new Map();
    private readonly config: LangiumPoolConfig;
    private instanceCounter = 0;

    constructor(config: LangiumPoolConfig) {
        this.config = config;
    }

    /**
     * Generates a unique key for a contribution plugin configuration.
     * Uses a stable JSON representation of the sorted plugins.
     *
     * @param contributionPlugins - Array of contribution plugin configurations
     * @returns A unique string key identifying this configuration
     */
    private generateContributionKey(contributionPlugins: ServerContributionPlugin[]): ContributionPluginKey {
        if (contributionPlugins.length === 0) {
            return "default";
        }
        // Create a stable key based on the contribution plugins configuration
        const sorted = [...contributionPlugins].sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b)));
        return JSON.stringify(sorted);
    }

    /**
     * Acquires a Langium instance for the given contribution plugins.
     * This method handles instance creation, reuse, and eviction.
     *
     * @param contributionPlugins - The contribution plugins configuration
     * @param jwt - The JWT token for this request
     * @returns Promise resolving to the acquired Langium instance
     */
    async acquire(contributionPlugins: ServerContributionPlugin[], jwt: string): Promise<LangiumInstance> {
        const key = this.generateContributionKey(contributionPlugins);

        // Try to find an existing instance with matching configuration
        for (const [instanceId, instance] of this.instances) {
            if (instance.contributionPluginKey === key) {
                // Wait if busy
                if (instance.busy && instance.busyPromise) {
                    await instance.busyPromise;
                }

                // Mark as busy
                this.markBusy(instance);
                instance.lastUsed = Date.now();

                // Set JWT on the server API
                this.config.serverApi.setJwt(jwt);

                return instance;
            }
        }

        // Need to create a new instance
        // Check if we need to evict
        if (this.instances.size >= this.config.maxInstances) {
            await this.evictOne(key);
        }

        // Create new instance
        const instance = await this.createInstance(contributionPlugins, key);
        const instanceId = `instance-${++this.instanceCounter}`;
        this.instances.set(instanceId, instance);

        // Mark as busy and set JWT
        this.markBusy(instance);
        this.config.serverApi.setJwt(jwt);

        return instance;
    }

    /**
     * Releases an instance after request handling is complete.
     * Clears the JWT and marks the instance as available.
     *
     * @param instance - The instance to release
     */
    release(instance: LangiumInstance): void {
        // Clear JWT
        this.config.serverApi.clearJwt();

        // TODO: Add cleanup logic here
        // - Clear document caches
        // - Reset any request-specific state

        // Mark as not busy
        this.markAvailable(instance);
    }

    /**
     * Evicts one instance to make room for a new one.
     * Prioritizes evicting instances with different plugin configurations.
     *
     * @param newKey - The contribution plugin key for the new instance
     * @returns Promise that resolves when eviction is complete
     */
    private async evictOne(newKey: ContributionPluginKey): Promise<void> {
        // Find the oldest instance with a different configuration
        let oldestDifferent: { id: string; instance: LangiumInstance } | null = null;
        let oldestSame: { id: string; instance: LangiumInstance } | null = null;

        for (const [instanceId, instance] of this.instances) {
            if (instance.contributionPluginKey !== newKey) {
                if (!oldestDifferent || instance.lastUsed < oldestDifferent.instance.lastUsed) {
                    oldestDifferent = { id: instanceId, instance };
                }
            } else {
                if (!oldestSame || instance.lastUsed < oldestSame.instance.lastUsed) {
                    oldestSame = { id: instanceId, instance };
                }
            }
        }

        if (oldestDifferent) {
            // Remove the oldest instance with different configuration
            // Even if busy, we just remove it from the map (it will complete its current request)
            this.instances.delete(oldestDifferent.id);
        } else if (oldestSame) {
            // All instances have the same configuration - wait for one to become available
            if (oldestSame.instance.busy && oldestSame.instance.busyPromise) {
                await oldestSame.instance.busyPromise;
            }
            // Now it's available, but we'll reuse it instead of evicting
            // Actually, we shouldn't evict same-configuration instances
            // Instead, just wait and the acquire logic will reuse it
            return;
        }
    }

    /**
     * Creates a new Langium instance with the given configuration.
     *
     * @param contributionPlugins - The contribution plugins for this instance
     * @param key - The contribution plugin key
     * @returns Promise resolving to the created Langium instance
     */
    private async createInstance(
        contributionPlugins: ServerContributionPlugin[],
        key: ContributionPluginKey
    ): Promise<LangiumInstance> {
        const plugin = this.config.languagePlugin;

        const languageModule = createModule([plugin], {
            langium,
            "langium/grammar": langiumGrammar
        });

        // Create shared services
        const generatedSharedModule: Module<
            LangiumSharedCoreServices,
            LangiumGeneratedSharedCoreServices & ExternalReferenceSharedAdditionalServices
        > = {
            AstReflection: () => languageModule.reflection,
            references: {
                ExternalReferenceResolver: () => new NoopExternalReferenceResolver()
            }
        };

        // Create file system provider
        const fileSystemProvider = this.createFileSystemProvider();

        const shared = inject(
            createDefaultSharedModule({ fileSystemProvider: () => fileSystemProvider }),
            generatedSharedModule
        );

        // Create language metadata
        const grammar = languageModule.grammars.get(plugin)!;
        const languageMetaData: LanguageMetaData = {
            languageId: this.config.languageId,
            fileExtensions: [this.config.extension],
            caseInsensitive: false,
            mode: "development"
        };

        const generatedModule: Module<LangiumCoreServices, LangiumGeneratedCoreServices> = {
            Grammar: () => grammar,
            LanguageMetaData: () => languageMetaData,
            parser: {}
        };

        const services = langium.inject(
            createDefaultModule({ shared }),
            generatedModule,
            plugin.module
        ) as LanguageServices;

        shared.ServiceRegistry.register(services);

        if (plugin.postCreate) {
            plugin.postCreate(services, { fileSystemProvider: () => fileSystemProvider });
        }

        return {
            services,
            contributionPluginKey: key,
            busy: false,
            busyPromise: null,
            busyResolver: null,
            lastUsed: Date.now()
        };
    }

    /**
     * Creates a file system provider that uses the ServerApi.
     *
     * @returns A file system provider compatible with Langium
     */
    private createFileSystemProvider() {
        const serverApi = this.config.serverApi;
        const { URI } = require("vscode-uri");

        return {
            readFile: async (uri: typeof URI): Promise<string> => {
                const path = uri.path;
                const result = await serverApi.readFile(path);
                return result.content;
            },

            readFileSync: (): string => {
                throw new Error("Sync file operations not supported in service context");
            },

            readBinary: async (): Promise<Uint8Array> => {
                throw new Error("Binary file operations not supported");
            },

            readBinarySync: (): Uint8Array => {
                throw new Error("Sync binary file operations not supported");
            },

            stat: async (uri: typeof URI): Promise<{ isFile: boolean; isDirectory: boolean; uri: typeof URI }> => {
                const path = uri.path;
                const exists = await serverApi.fileExists(path);
                // Assume it's a file if it exists (directory checking would require additional API)
                return {
                    isFile: exists,
                    isDirectory: false,
                    uri
                };
            },

            statSync: (): { isFile: boolean; isDirectory: boolean; uri: typeof URI } => {
                throw new Error("Sync file operations not supported in service context");
            },

            readDirectory: async (
                uri: typeof URI
            ): Promise<Array<{ isFile: boolean; isDirectory: boolean; uri: typeof URI }>> => {
                const path = uri.path;
                const entries = await serverApi.listDirectory(path);
                return entries.map((entry) => ({
                    isFile: entry.isFile,
                    isDirectory: entry.isDirectory,
                    uri: URI.joinPath(uri, entry.name)
                }));
            },

            readDirectorySync: (): Array<{ isFile: boolean; isDirectory: boolean; uri: typeof URI }> => {
                throw new Error("Sync file operations not supported in service context");
            },

            exists: async (uri: typeof URI): Promise<boolean> => {
                const path = uri.path;
                return serverApi.fileExists(path);
            },

            existsSync: (): boolean => {
                throw new Error("Sync file operations not supported in service context");
            },

            readMetadata: async (): Promise<object> => {
                return {};
            },

            writeMetadata: async (): Promise<void> => {
                // No-op for service context
            }
        };
    }

    private markBusy(instance: LangiumInstance): void {
        instance.busy = true;
        instance.busyPromise = new Promise((resolve) => {
            instance.busyResolver = resolve;
        });
    }

    private markAvailable(instance: LangiumInstance): void {
        instance.busy = false;
        if (instance.busyResolver) {
            instance.busyResolver();
            instance.busyResolver = null;
        }
        instance.busyPromise = null;
    }
}

/**
 * No-op external reference resolver for service context
 */
class NoopExternalReferenceResolver {
    async loadExternalDocument(): Promise<void> {
        // No-op - external references are not supported in service context
    }
}

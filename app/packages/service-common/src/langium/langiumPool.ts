import {
    EmptyFileSystemProvider,
    inject,
    type LangiumCoreServices,
    type LangiumGeneratedCoreServices,
    type LangiumGeneratedSharedCoreServices,
    type LangiumSharedCoreServices,
    type LanguageMetaData,
    type Module
} from "langium";
import type { LanguageServices, ExternalReferenceSharedAdditionalServices } from "@mdeo/language-common";
import type { ServerContributionPlugin } from "@mdeo/plugin";
import { createGLSPModule, createModule } from "@mdeo/language-common";
import { HttpServerApi } from "../service/serverApi.js";
import type { ContributionPluginKey } from "../service/types.js";
import { createDefaultModule, createDefaultSharedModule } from "langium/lsp";
import * as langium from "langium";
import * as langiumGrammar from "langium/grammar";
import type { ServiceAdditionalSharedServices, LangiumPoolConfig } from "./types.js";
import { BackendExternalReferencesResolver } from "./backendExternalReferencesResolver.js";
import { LangiumInstance } from "./langiumInstance.js";
import { JsonAstSerializer } from "./jsonAstSerializer.js";
import { ExtendedIndexManager } from "./extendedIndexManager.js";

/**
 * Manages a pool of Langium instances for handling file data requests.
 *
 * Each instance is tied to a specific contribution plugin configuration.
 * Instances are reused when the same configuration is requested, and
 * evicted based on LRU when the pool is full.
 */
export class LangiumInstancePool<T> {
    /**
     * Map of instance IDs to Langium instances
     */
    private readonly instances: Map<string, LangiumInstance<T>> = new Map();
    /**
     * Counter for generating unique instance IDs
     */
    private instanceCounter = 0;

    /**
     * Queue of waiters for instances when all are busy
     */
    private readonly instanceWaitQueue: ((instance: LangiumInstance<T>) => void)[] = [];

    constructor(private readonly config: LangiumPoolConfig<T>) {
        this.config = config;
    }

    /**
     * Generates a unique key for a contribution plugin configuration.
     * Uses a stable JSON representation of the sorted plugins.
     *
     * @param contributionPlugins Array of contribution plugin configurations
     * @returns A unique string key identifying this configuration
     */
    private generateContributionKey(contributionPlugins: ServerContributionPlugin[]): ContributionPluginKey {
        if (contributionPlugins.length === 0) {
            return "[]";
        }
        const sorted = contributionPlugins.map((plugin) => JSON.stringify(plugin)).sort();
        return JSON.stringify(sorted);
    }

    /**
     * Acquires a Langium instance for the given contribution plugins.
     * This method handles instance creation, reuse, and eviction.
     *
     * @param contributionPlugins The contribution plugins configuration
     * @param jwt The JWT token for this request
     * @param project The project context for this request
     * @returns Promise resolving to the acquired Langium instance
     */
    async acquire(
        contributionPlugins: ServerContributionPlugin[],
        jwt: string,
        project: string
    ): Promise<LangiumInstance<T>> {
        const key = this.generateContributionKey(contributionPlugins);

        let instance: LangiumInstance<T> | undefined = undefined;

        for (const candidate of this.instances.values()) {
            if (candidate.contributionPluginKey === key) {
                if (!candidate.busy) {
                    instance = candidate;
                    break;
                }
            }
        }

        if (instance == undefined && this.instances.size < this.config.maxInstances) {
            instance = this.createInstance(contributionPlugins, key);
        }

        const usedInstance = instance ?? (await this.evictOrWait(key, contributionPlugins));

        usedInstance.configure(jwt, project);
        return usedInstance;
    }

    /**
     * Evicts the least recently used available instance or waits for one to become available.
     *
     * @param key The contribution plugin key
     * @param contributionPlugins The contribution plugins configuration
     * @returns Promise resolving to the acquired Langium instance
     */
    private async evictOrWait(
        key: ContributionPluginKey,
        contributionPlugins: ServerContributionPlugin[]
    ): Promise<LangiumInstance<T>> {
        let oldestAvailable: LangiumInstance<T> | undefined = undefined;

        for (const instance of this.instances.values()) {
            if (instance.contributionPluginKey !== key && !instance.busy) {
                if (oldestAvailable == undefined || instance.lastUsed < oldestAvailable.lastUsed) {
                    oldestAvailable = instance;
                }
            }
        }

        if (oldestAvailable != undefined) {
            this.instances.delete(oldestAvailable.id);
            return this.createInstance(contributionPlugins, key);
        }

        return new Promise<LangiumInstance<T>>((resolve) => {
            this.instanceWaitQueue.push((availableInstance) => {
                if (availableInstance.contributionPluginKey === key) {
                    resolve(availableInstance);
                } else {
                    this.instances.delete(availableInstance.id);
                    const newInstance = this.createInstance(contributionPlugins, key);
                    resolve(newInstance);
                }
            });
        });
    }

    /**
     * Releases an instance after request handling is complete.
     * Clears the JWT and marks the instance as available.
     *
     * @param instance The instance to release
     */
    release(instance: LangiumInstance<T>): void {
        instance.reset();
        if (this.instanceWaitQueue.length > 0) {
            const waiter = this.instanceWaitQueue.shift()!;
            waiter(instance);
        }
    }

    /**
     * Creates a new Langium instance with the given configuration.
     *
     * @param contributionPlugins The contribution plugins for this instance
     * @param key The contribution plugin key
     * @returns Promise resolving to the created Langium instance
     */
    private createInstance(
        contributionPlugins: ServerContributionPlugin[],
        key: ContributionPluginKey
    ): LangiumInstance<T> {
        const plugin = this.config.languagePluginProvider.create(contributionPlugins);

        const languageModule = createModule([plugin], {
            langium,
            "langium/grammar": langiumGrammar
        });

        const generatedSharedModule: Module<
            LangiumSharedCoreServices & ServiceAdditionalSharedServices,
            LangiumGeneratedSharedCoreServices &
                ExternalReferenceSharedAdditionalServices &
                ServiceAdditionalSharedServices
        > = {
            AstReflection: () => languageModule.reflection,
            ServerApi: () => new HttpServerApi(this.config.backendUrl),
            references: {
                ExternalReferenceResolver: (services) => new BackendExternalReferencesResolver(services)
            },
            serializer: {
                JsonAstSerializer: (services) => new JsonAstSerializer(services)
            },
            workspace: {
                IndexManager: (services) => new ExtendedIndexManager(services)
            }
        };

        const fileSystemProvider = new EmptyFileSystemProvider();

        const glspModule = createGLSPModule(globalThis.pluginContext!);

        const shared = inject(
            createDefaultSharedModule({ fileSystemProvider: () => fileSystemProvider }),
            generatedSharedModule,
            glspModule
        );

        const grammar = languageModule.grammars.get(plugin)!;
        const fileExtensions = this.config.extension ? [this.config.extension] : [];
        const languageMetaData: LanguageMetaData = {
            languageId: this.config.languageId,
            fileExtensions,
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
            plugin.module,
            this.config.serviceModule
        ) as LanguageServices & { shared: ServiceAdditionalSharedServices } & T;

        shared.ServiceRegistry.register(services);

        if (plugin.postCreate) {
            plugin.postCreate(services, { fileSystemProvider: () => fileSystemProvider });
        }

        const newInstance = new LangiumInstance(`instance-${++this.instanceCounter}`, services, key);
        this.instances.set(newInstance.id, newInstance);
        return newInstance;
    }
}

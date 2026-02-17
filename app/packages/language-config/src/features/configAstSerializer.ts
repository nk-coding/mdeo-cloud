import type { LangiumCoreServices, ServiceRegistry } from "langium";
import { DefaultAstSerializer } from "@mdeo/language-shared";
import type { ConfigContributionPlugin } from "../plugin/configContributionPlugin.js";
import type { AstSerializer, PrimitiveValue, PrintContext } from "@mdeo/language-common";
import type { Doc } from "prettier";

/**
 * Gets language services by language ID from the Langium ServiceRegistry.
 *
 * @param registry The Langium service registry
 * @param languageId The language ID to find
 * @returns The language services, or undefined if not found
 */
function getServicesByLanguageId(registry: ServiceRegistry, languageId: string): LangiumCoreServices | undefined {
    for (const services of registry.all) {
        if (services.LanguageMetaData.languageId === languageId) {
            return services;
        }
    }
    return undefined;
}

/**
 * AST serializer for the config language that delegates section serialization to plugin serializers.
 *
 * This serializer extends DefaultAstSerializer and overrides serializeNode to handle config sections.
 * When serializing a section node, it delegates to the appropriate plugin's AstSerializer if available.
 */
export class ConfigAstSerializer extends DefaultAstSerializer {

    /**
     * The service registry for accessing plugin language services.
     */
    protected readonly serviceRegistry: ServiceRegistry;

    /**
     * All unique plugins (used to avoid processing the same plugin multiple times).
     */
    protected readonly plugins: ConfigContributionPlugin[];

    /**
     * Cached combined node serializers map.
     */
    private combinedNodeSerializers?: Map<string, (context: PrintContext) => Doc>;

    /**
     * Cached combined primitive serializers map.
     */
    private combinedPrimitiveSerializers?: Map<string, (primitive: PrimitiveValue<unknown>) => string>;

    /**
     * Creates a new ConfigAstSerializer.
     *
     * @param services The config language services
     * @param serviceRegistry The service registry for accessing plugin services
     * @param plugins The config contribution plugins
     */
    constructor(
        services: LangiumCoreServices,
        serviceRegistry: ServiceRegistry,
        plugins: ConfigContributionPlugin[]
    ) {
        super(services);
        this.serviceRegistry = serviceRegistry;
        this.plugins = plugins;
    }

    /**
     * Gets the AstSerializer from the plugin's language services.
     *
     * @param plugin The contrib plugin
     * @returns The plugin's AstSerializer, or undefined if not found
     */
    protected getPluginSerializer(plugin: ConfigContributionPlugin): AstSerializer | undefined {
        const pluginServices = getServicesByLanguageId(this.serviceRegistry, plugin.languageKey);
        if (!pluginServices) {
            throw new Error(`Could not find services for plugin language: ${plugin.languageKey}`);
        }

        const anyServices = pluginServices as Record<string, unknown>;
        if (anyServices.AstSerializer && typeof anyServices.AstSerializer === "object") {
            const serializer = anyServices.AstSerializer as AstSerializer;
            if (typeof serializer.serializeNode === "function") {
                return serializer;
            }
        }

        return undefined;
    }

    override getNodeSerializers(): Map<string, (context: PrintContext) => Doc> {
        if (this.combinedNodeSerializers) {
            return this.combinedNodeSerializers;
        }

        const combined = new Map(super.getNodeSerializers());

        for (const plugin of this.plugins) {
            const pluginSerializer = this.getPluginSerializer(plugin);
            if (pluginSerializer) {
                const pluginSerializers = pluginSerializer.getNodeSerializers();
                for (const [key, value] of pluginSerializers.entries()) {
                    combined.set(key, value);
                }
            }
        }

        this.combinedNodeSerializers = combined;
        return combined;
    }

    override getPrimitiveSerializers(): Map<string, (primitive: PrimitiveValue<unknown>) => string> {
        if (this.combinedPrimitiveSerializers) {
            return this.combinedPrimitiveSerializers;
        }

        const combined = new Map(super.getPrimitiveSerializers());

        for (const plugin of this.plugins) {
            const pluginSerializer = this.getPluginSerializer(plugin);
            if (pluginSerializer) {
                const pluginSerializers = pluginSerializer.getPrimitiveSerializers();
                for (const [key, value] of pluginSerializers.entries()) {
                    combined.set(key, value);
                }
            }
        }

        this.combinedPrimitiveSerializers = combined;
        return combined;
    }
}

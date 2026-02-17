import type { AstNode, LangiumCoreServices, ReferenceInfo, Scope, ServiceRegistry } from "langium";
import { DefaultScopeProvider, EMPTY_SCOPE } from "langium";
import type { ConfigContributionPlugin } from "../plugin/configContributionPlugin.js";
import type { BaseConfigSectionType } from "../grammar/configTypes.js";
import type { ResolvedConfigContributionPlugins } from "../plugin/resolvePlugins.js";

/**
 * Gets a language services by its language ID from the Langium ServiceRegistry.
 *
 * @param registry The Langium service registry
 * @param languageId The language ID to find
 * @returns The language services, or undefined if not found
 */
function getServicesByLanguageId(registry: ServiceRegistry, languageId: string): LangiumCoreServices | undefined {
    // Use the 'all' property to iterate through registered services
    for (const services of registry.all) {
        if (services.LanguageMetaData.languageId === languageId) {
            return services;
        }
    }
    return undefined;
}

/**
 * A delegating scope provider for the config language.
 * It delegates scope computation to the plugin-specific language services
 * based on the section type being processed.
 *
 * This allows each plugin to define its own scope resolution logic
 * while the config language coordinates between them.
 */
export class ConfigDelegatingScopeProvider extends DefaultScopeProvider {
    /**
     * Map from section type name to the plugin that contributes it.
     */
    protected readonly sectionToPlugin = new Map<string, ConfigContributionPlugin>();

    /**
     * The service registry for getting language services.
     */
    protected readonly serviceRegistry: ServiceRegistry;

    /**
     * The original plugins for reference.
     */
    protected readonly plugins: ConfigContributionPlugin[];

    /**
     * Creates a new ConfigDelegatingScopeProvider.
     *
     * @param services The Langium services
     * @param serviceRegistry The service registry for getting plugin language services
     * @param plugins The config contribution plugins
     * @param resolvedPlugins Optional resolved plugins with wrapper type information
     */
    constructor(
        services: LangiumCoreServices,
        serviceRegistry: ServiceRegistry,
        plugins: ConfigContributionPlugin[],
        resolvedPlugins?: ResolvedConfigContributionPlugins
    ) {
        super(services);
        this.serviceRegistry = serviceRegistry;
        this.plugins = plugins;

        // Build section to plugin mapping using wrapper type names
        if (resolvedPlugins) {
            for (const [qualifiedName, sectionInfo] of resolvedPlugins.sections) {
                // Map the wrapper interface name to the plugin
                this.sectionToPlugin.set(sectionInfo.interface.name, sectionInfo.plugin);
            }
        } else {
            // Fallback: map section names (without wrappers)
            for (const plugin of plugins) {
                for (const section of plugin.sections) {
                    this.sectionToPlugin.set(section.name, plugin);
                }
            }
        }
    }

    /**
     * Gets the scope for a reference, delegating to the appropriate plugin language.
     *
     * @param context The reference info containing context about the reference
     * @returns The scope for the reference
     */
    override getScope(context: ReferenceInfo): Scope {
        // Find which section wrapper this node belongs to
        const sectionWrapper = this.findContainingSection(context.container);
        if (!sectionWrapper) {
            return super.getScope(context);
        }

        // Get the plugin for this section wrapper
        const plugin = this.getPluginForSection(sectionWrapper);
        if (!plugin) {
            return super.getScope(context);
        }

        // Get the services for the plugin language
        const pluginServices = getServicesByLanguageId(this.serviceRegistry, plugin.languageKey);
        if (!pluginServices) {
            console.warn(`No services found for language '${plugin.languageKey}'`);
            return EMPTY_SCOPE;
        }

        // Delegate to the plugin's scope provider
        // Note: The context.container is already within the content field, so we don't need to unwrap it
        const scopeProvider = pluginServices.references.ScopeProvider;
        return scopeProvider.getScope(context);
    }

    /**
     * Finds the containing section for an AST node.
     *
     * @param node The AST node
     * @returns The section node, or undefined if not in a section
     */
    protected findContainingSection(node: AstNode): BaseConfigSectionType | undefined {
        let current: AstNode | undefined = node;
        while (current) {
            if (this.isConfigSection(current)) {
                return current as BaseConfigSectionType;
            }
            current = current.$container;
        }
        return undefined;
    }

    /**
     * Checks if an AST node is a config section.
     *
     * @param node The AST node to check
     * @returns True if the node is a config section
     */
    protected isConfigSection(node: AstNode): boolean {
        // Check if the node's type is one of the known section types
        // Section types include both the direct section rule names and interfaces
        return this.sectionToPlugin.has(node.$type);
    }

    /**
     * Gets the plugin that contributes a section.
     *
     * @param section The section node
     * @returns The plugin, or undefined if not found
     */
    protected getPluginForSection(section: BaseConfigSectionType): ConfigContributionPlugin | undefined {
        return this.sectionToPlugin.get(section.$type);
    }
}

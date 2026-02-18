import type { LangiumCoreServices, ReferenceInfo, Scope, ServiceRegistry } from "langium";
import type { ConfigContributionPlugin } from "../plugin/configContributionPlugin.js";
import type { BaseConfigSectionType } from "../grammar/configTypes.js";
import type { ResolvedConfigContributionPlugins } from "../plugin/resolvePlugins.js";
import { sharedImport } from "@mdeo/language-shared";
import { getServicesByLanguageId } from "./util.js";

const { EMPTY_SCOPE, DefaultScopeProvider, AstUtils } = sharedImport("langium");

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
        resolvedPlugins: ResolvedConfigContributionPlugins
    ) {
        super(services);
        this.serviceRegistry = serviceRegistry;
        this.plugins = plugins;

        for (const sectionInfo of resolvedPlugins.sections.values()) {
            this.sectionToPlugin.set(sectionInfo.interface.name, sectionInfo.plugin);
        }
    }

    /**
     * Gets the scope for a reference, delegating to the appropriate plugin language.
     *
     * @param context The reference info containing context about the reference
     * @returns The scope for the reference
     */
    override getScope(context: ReferenceInfo): Scope {
        const sectionWrapper = AstUtils.getContainerOfType(context.container, (node): node is BaseConfigSectionType =>
            this.sectionToPlugin.has(node.$type)
        );
        if (sectionWrapper == undefined) {
            return EMPTY_SCOPE;
        }

        const plugin = this.getPluginForSection(sectionWrapper);
        if (plugin == undefined) {
            return EMPTY_SCOPE;
        }

        const pluginServices = getServicesByLanguageId(this.serviceRegistry, plugin.languageKey);
        if (pluginServices == undefined) {
            throw new Error(
                `No language services found for plugin '${plugin.id}' with language key '${plugin.languageKey}'. Make sure the plugin's language is registered in the service registry.`
            );
        }

        const scopeProvider = pluginServices.references.ScopeProvider;
        return scopeProvider.getScope(context);
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

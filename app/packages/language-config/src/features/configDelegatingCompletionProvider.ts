import type { LangiumCoreServices, MaybePromise, ServiceRegistry } from "langium";
import type {
    CompletionAcceptor,
    CompletionContext,
    CompletionProviderOptions,
    LangiumLSPServices,
    LangiumServices,
    NextFeature
} from "langium/lsp";
import { sharedImport } from "@mdeo/language-shared";
import type { BaseConfigSectionType } from "../grammar/configTypes.js";
import type { ConfigContributionPlugin } from "../plugin/configContributionPlugin.js";
import type { ResolvedConfigContributionPlugins } from "../plugin/resolvePlugins.js";
import { getServicesByLanguageId } from "./util.js";

const { DefaultCompletionProvider } = sharedImport("langium/lsp");
const { AstUtils } = sharedImport("langium");

/**
 * A delegating completion provider for the config language.
 *
 * When completing inside a plugin-contributed section block, it forwards the request
 * to the plugin language's own CompletionProvider. This allows each plugin to supply
 * its full custom completion logic (path completions, function references, keyword
 * suggestions, cross-references, etc.) without any duplication.
 *
 * For completions that are not inside any plugin section (e.g., section-level keywords
 * at the top of a config document), the request falls back to the standard
 * DefaultCompletionProvider behaviour.
 */
export class ConfigDelegatingCompletionProvider extends DefaultCompletionProvider {
    override readonly completionOptions: CompletionProviderOptions;

    /**
     * Map from section wrapper AST-type name to the plugin that contributes it.
     * Built once at construction time from the resolved plugin contributions.
     */
    private readonly sectionToPlugin = new Map<string, ConfigContributionPlugin>();

    /**
     * The service registry used to look up plugin language services at completion time.
     */
    private readonly serviceRegistry: ServiceRegistry;

    /**
     * Creates a new ConfigDelegatingCompletionProvider.
     *
     * @param services The config language services (passed as-is to DefaultCompletionProvider)
     * @param resolvedPlugins The resolved plugin contributions providing section type information
     */
    constructor(services: LangiumServices, resolvedPlugins: ResolvedConfigContributionPlugins) {
        super(services);
        this.serviceRegistry = services.shared.ServiceRegistry;

        for (const sectionInfo of resolvedPlugins.sections.values()) {
            this.sectionToPlugin.set(sectionInfo.interface.name, sectionInfo.plugin);
        }

        const triggerCharSet = new Set<string>();
        for (const plugin of this.sectionToPlugin.values()) {
            const pluginServices = getServicesByLanguageId(this.serviceRegistry, plugin.languageKey);
            const pluginCompletion = (pluginServices as (LangiumCoreServices & LangiumLSPServices) | undefined)?.lsp
                ?.CompletionProvider;
            const chars = pluginCompletion?.completionOptions?.triggerCharacters;
            if (chars != undefined) {
                for (const char of chars) {
                    triggerCharSet.add(char);
                }
            }
        }
        this.completionOptions = { triggerCharacters: triggerCharSet.size > 0 ? [...triggerCharSet] : undefined };
    }

    /**
     * Routes completion to the appropriate plugin language when inside a section block,
     * and falls back to the default completion logic for config-level constructs
     * (e.g., section-name keywords).
     *
     * When the cursor is within a node that belongs to a plugin-contributed section,
     * the request is forwarded to that plugin's CompletionProvider so that the plugin
     * can apply its own custom completion rules (path completions, function references,
     * cross-reference scoping, etc.).
     *
     * @param context The current completion context including the AST node at the cursor
     * @param next Describes the grammar feature being completed
     * @param acceptor The acceptor function to register completion items
     * @returns A promise or void when completion is complete
     */
    protected override completionFor(
        context: CompletionContext,
        next: NextFeature,
        acceptor: CompletionAcceptor
    ): MaybePromise<void> {
        const node = context.node;

        if (node != undefined) {
            const sectionWrapper = AstUtils.getContainerOfType(node, (node): node is BaseConfigSectionType =>
                this.sectionToPlugin.has(node.$type)
            );

            if (sectionWrapper != undefined) {
                const plugin = this.sectionToPlugin.get(sectionWrapper.$type);
                if (plugin != undefined) {
                    const pluginServices = getServicesByLanguageId(this.serviceRegistry, plugin.languageKey);
                    const pluginCompletionProvider = (
                        pluginServices as (LangiumCoreServices & LangiumLSPServices) | undefined
                    )?.lsp?.CompletionProvider;
                    if (
                        pluginCompletionProvider != undefined &&
                        pluginCompletionProvider instanceof DefaultCompletionProvider
                    ) {
                        // @ts-expect-error protected method access
                        return pluginCompletionProvider.completionFor(context, next, acceptor);
                    }
                }
            }
        }

        return super.completionFor(context, next, acceptor);
    }
}

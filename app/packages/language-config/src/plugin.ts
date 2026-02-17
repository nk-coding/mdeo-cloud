import {
    type LangiumLanguagePlugin,
    type LangiumLanguagePluginProvider,
    type ExternalReferenceAdditionalServices,
    type ExternalReferenceCollector,
    type ExternalReferences,
    ID,
    NEWLINE,
    FLOAT,
    INT,
    HIDDEN_NEWLINE,
    GrammarDeserializationContext,
    STRING
} from "@mdeo/language-common";
import { createConfigRule, ConfigTerminals } from "./grammar/configRules.js";
import { resolveConfigPlugins, type ResolvedConfigContributionPlugins } from "./plugin/resolvePlugins.js";
import { ConfigContributionPlugin } from "./plugin/configContributionPlugin.js";
import { ConfigDelegatingScopeProvider } from "./features/configDelegatingScopeProvider.js";
import { ConfigAstSerializer } from "./features/configAstSerializer.js";
import {
    IdValueConverter,
    NewlineAwareTokenBuilder,
    SerializerFormatter,
    registerDefaultTokenSerializers
} from "@mdeo/language-shared";
import type { LangiumDocument } from "langium";
import { registerConfigSerializers } from "./features/configSerializers.js";

/**
 * External reference collector for the config language.
 * Config files don't have external references in the traditional sense (everything is inline),
 * so this returns empty collections.
 */
class ConfigExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        return {
            local: [],
            external: []
        };
    }
}

/**
 * Additional services for the Config language.
 */
export interface ConfigAdditionalServices extends ExternalReferenceAdditionalServices {
    /**
     * The resolved contribution plugins for the config language.
     */
    config: {
        ResolvedPlugins: ResolvedConfigContributionPlugins;
        ContributionPlugins: ConfigContributionPlugin[];
    };
}

/**
 * Provider for the Config language plugin.
 */
export const configPluginProvider: LangiumLanguagePluginProvider<ConfigAdditionalServices> = {
    create(contributionPlugins: ConfigContributionPlugin[]): LangiumLanguagePlugin<ConfigAdditionalServices> {
        const configPlugins = contributionPlugins.filter(ConfigContributionPlugin.is);

        const deserializationContext = GrammarDeserializationContext.create(
            [],
            [],
            [ID, NEWLINE, HIDDEN_NEWLINE, INT, FLOAT, STRING]
        );

        const resolvedPlugins = resolveConfigPlugins(configPlugins, deserializationContext);

        const configRule = createConfigRule(resolvedPlugins);

        return {
            rootRule: configRule,
            additionalTerminals: ConfigTerminals,
            module: {
                parser: {
                    TokenBuilder: () =>
                        new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
                    ValueConverter: () => new IdValueConverter()
                },
                references: {
                    ScopeProvider: (services) =>
                        new ConfigDelegatingScopeProvider(services, services.shared.ServiceRegistry, configPlugins, resolvedPlugins),
                    ExternalReferenceCollector: () => new ConfigExternalReferenceCollector()
                },
                lsp: {
                    Formatter: (services) => new SerializerFormatter(services)
                },
                AstSerializer: (services) =>
                    new ConfigAstSerializer(services, services.shared.ServiceRegistry, configPlugins),
                config: {
                    ResolvedPlugins: () => resolvedPlugins,
                    ContributionPlugins: () => configPlugins
                }
            },
            postCreate(services) {
                registerDefaultTokenSerializers(services);
                registerConfigSerializers(services, resolvedPlugins);
            }
        };
    }
};

import { type LangiumLanguagePlugin, type LangiumLanguagePluginProvider, type ExternalReferenceCollector, type ExternalReferences, type ExternalReferenceAdditionalServices, createRule, createInterface, ML_COMMENT, SL_COMMENT } from "@mdeo/language-common";
import {
    DefaultAstSerializer,
    IdValueConverter,
    SerializerFormatter,
    registerDefaultTokenSerializers
} from "@mdeo/language-shared";
import { OptimizationScopeProvider } from "./features/optimizationScopeProvider.js";
import { registerOptimizationSerializers } from "./features/optimizationSerializers.js";
import type { LangiumDocument } from "langium";

/**
 * External reference collector for the config-optimization language.
 * Since optimization sections are embedded within config files, we don't need external reference tracking.
 */
class OptimizationExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        return {
            local: [],
            external: []
        };
    }
}

/**
 * The plugin for the Config Optimization generated language.
 * This language is marked as generated, meaning it doesn't have its own root file type
 * but provides services (scope provider, serializers) for the config language's
 * optimization sections.
 */
const configOptimizationPlugin: LangiumLanguagePlugin<ExternalReferenceAdditionalServices> = {
    rootRule: createRule("EMPTY").returns(createInterface("EMPTY").attrs({})).as(() => []),
    additionalTerminals: [SL_COMMENT, ML_COMMENT],
    module: {
        parser: {
            ValueConverter: () => new IdValueConverter()
        },
        references: {
            ScopeProvider: (services) => new OptimizationScopeProvider(services),
            ExternalReferenceCollector: () => new OptimizationExternalReferenceCollector()
        },
        lsp: {
            Formatter: (services) => new SerializerFormatter(services)
        },
        AstSerializer: (services) => new DefaultAstSerializer(services)
    },
    postCreate(services) {
        registerDefaultTokenSerializers(services);
        registerOptimizationSerializers(services);
    }
};

/**
 * Provider for the Config Optimization language plugin.
 */
export const configOptimizationPluginProvider: LangiumLanguagePluginProvider<ExternalReferenceAdditionalServices> = {
    create(): LangiumLanguagePlugin<ExternalReferenceAdditionalServices> {
        return configOptimizationPlugin;
    }
};

import {
    type ExternalReferenceAdditionalServices,
    type LangiumLanguagePlugin,
    type LangiumLanguagePluginProvider
} from "@mdeo/language-common";
import { ModelRule, ModelTerminals } from "./grammar/modelRules.js";
import {
    addExternalReferenceCollectionPhase,
    type ActionAdditionalServices,
    DefaultAstSerializer,
    IdValueConverter,
    NewlineAwareTokenBuilder,
    SerializerFormatter
} from "@mdeo/language-shared";
import { ModelScopeProvider } from "./features/modelScopeProvider.js";
import { ModelExternalReferenceCollector } from "./features/modelExternalReferenceCollector.js";
import { ModelActionHandler } from "./features/modelActionHandler.js";

export type ModelServices = ExternalReferenceAdditionalServices & ActionAdditionalServices;

/**
 * The plugin for the Model language.
 * Provides minimal language server functionality for .m files.
 */
const modelPlugin: LangiumLanguagePlugin<ModelServices> = {
    rootRule: ModelRule,
    additionalTerminals: ModelTerminals,
    module: {
        parser: {
            TokenBuilder: () => new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
            ValueConverter: () => new IdValueConverter()
        },
        references: {
            ScopeProvider: (services) => new ModelScopeProvider(services),
            ExternalReferenceCollector: () => new ModelExternalReferenceCollector()
        },
        lsp: {
            Formatter: (services) => new SerializerFormatter(services)
        },
        AstSerializer: (services) => new DefaultAstSerializer(services),
        action: {
            ActionHandler: (services) => new ModelActionHandler(services.shared)
        }
    },
    postCreate(services) {
        addExternalReferenceCollectionPhase(services);
    }
};

/**
 * Provider for the Model language plugin.
 */
export const modelPluginProvider: LangiumLanguagePluginProvider<ModelServices> = {
    create(): LangiumLanguagePlugin<ModelServices> {
        return modelPlugin;
    }
};

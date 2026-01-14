import { type LangiumLanguagePlugin, type LangiumLanguagePluginProvider } from "@mdeo/language-common";
import { ModelRule, ModelTerminals } from "./grammar/modelRules.js";
import {
    DefaultAstSerializer,
    IdValueConverter,
    NewlineAwareTokenBuilder,
    SerializerFormatter
} from "@mdeo/language-shared";
import { ModelScopeProvider } from "./features/modelScopeProvider.js";

/**
 * The plugin for the Model language.
 * Provides minimal language server functionality for .m files.
 */
const modelPlugin: LangiumLanguagePlugin<object> = {
    rootRule: ModelRule,
    additionalTerminals: ModelTerminals,
    module: {
        parser: {
            TokenBuilder: () => new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
            ValueConverter: () => new IdValueConverter()
        },
        references: {
            ScopeProvider: (services) => new ModelScopeProvider(services)
        },
        lsp: {
            Formatter: (services) => new SerializerFormatter(services)
        },
        AstSerializer: (services) => new DefaultAstSerializer(services)
    }
};

/**
 * Provider for the Model language plugin.
 */
export const modelPluginProvider: LangiumLanguagePluginProvider<object> = {
    create(): LangiumLanguagePlugin<object> {
        return modelPlugin;
    }
};

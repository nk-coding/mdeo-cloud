import { MetaModelRule } from "./grammar/rules.js";
import { type LanguagePlugin, WS, ML_COMMENT, SL_COMMENT, HIDDEN_NEWLINE } from "@mdeo/language-common";
import type { AstSerializerAdditionalServices } from "@mdeo/language-shared";
import {
    IdValueConverter,
    NewlineAwareTokenBuilder,
    DefaultAstSerializer,
    SerializerFormatter,
    registerDefaultTokenSerializers
} from "@mdeo/language-shared";
import { MetamodelScopeProvider } from "./features/scopeProvider.js";
import { registerMetamodelSerializers } from "./features/metamodelSerializers.js";

/**
 * The additional services for the Metamodel language.
 */
export type MetamodelServices = AstSerializerAdditionalServices;

/**
 * The plugin for the Metamodel language.
 * Configures the language with newline-aware lexing and custom parsing behavior.
 */
export const metamodelPlugin: LanguagePlugin<MetamodelServices> = {
    rootRule: MetaModelRule,
    additionalTerminals: [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT],
    module: {
        parser: {
            TokenBuilder: () => new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
            ValueConverter: () => new IdValueConverter()
        },
        references: {
            ScopeProvider: (services) => new MetamodelScopeProvider(services)
        },
        lsp: {
            Formatter: (services) => new SerializerFormatter(services)
        },
        AstSerializer: (services) => new DefaultAstSerializer(services)
    },
    postCreate(services) {
        registerDefaultTokenSerializers(services);
        registerMetamodelSerializers(services);
    }
};

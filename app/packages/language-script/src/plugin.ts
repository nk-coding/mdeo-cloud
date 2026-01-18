import {
    HIDDEN_NEWLINE,
    ML_COMMENT,
    SL_COMMENT,
    WS,
    type ExternalReferenceAdditionalServices,
    type LangiumLanguagePlugin,
    type LangiumLanguagePluginProvider
} from "@mdeo/language-common";
import {
    IdValueConverter,
    NewlineAwareTokenBuilder,
    DefaultAstSerializer,
    SerializerFormatter,
    registerDefaultTokenSerializers,
    sharedImport,
    addExternalReferenceCollectionPhase
} from "@mdeo/language-shared";
import { ScriptRule } from "./grammar/scriptRules.js";
import {
    defaultExtendedTypirServices,
    type AdditionalTypirServices,
    type ExpressionTypirServices,
    registerExpressionSerializers,
    registerStatementSerializers,
    registerTypeSerializers
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";
import { ScriptTypeSystem } from "./features/type-system/scriptTypeSystem.js";
import { ScriptScopeProvider } from "./features/type-system/scriptScopeProvider.js";
import { registerScriptSerializers } from "./features/type-system/scriptSerializers.js";
import { expressionTypes, statementTypes, typeTypes } from "./grammar/scriptTypes.js";
import { ScriptLangiumScopeProvider } from "./features/scriptScopeProvider.js";
import { ScriptExternalReferenceCollector } from "./features/scriptExternalReferenceCollector.js";

const { createTypirLangiumServicesWithAdditionalServices, initializeLangiumTypirServices } =
    sharedImport("typir-langium");

/**
 * The Typir specifics for the Script language.
 */
export type ScriptTypirSpecifics = TypirLangiumSpecifics;

/**
 * The additional services for the Script language.
 */
export type ScriptServices = {
    typir: ExpressionTypirServices<ScriptTypirSpecifics>;
} & ExternalReferenceAdditionalServices;

/**
 * The plugin for the Script language.
 */
const scriptPlugin: LangiumLanguagePlugin<ScriptServices> = {
    rootRule: ScriptRule,
    additionalTerminals: [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT],
    module: {
        parser: {
            TokenBuilder: () => new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
            ValueConverter: () => new IdValueConverter()
        },
        references: {
            ScopeProvider: (services) => new ScriptLangiumScopeProvider(services),
            ExternalReferenceCollector: () => new ScriptExternalReferenceCollector()
        },
        typir: (services) =>
            createTypirLangiumServicesWithAdditionalServices<
                ScriptTypirSpecifics,
                AdditionalTypirServices<ScriptTypirSpecifics>
            >(
                services.shared as any,
                services.shared.AstReflection as any,
                new ScriptTypeSystem(),
                defaultExtendedTypirServices<ScriptTypirSpecifics>(),
                {
                    ScopeProvider: (services) =>
                        new ScriptScopeProvider(services as ExpressionTypirServices<ScriptTypirSpecifics>)
                }
            ) as ExpressionTypirServices<ScriptTypirSpecifics>,
        lsp: {
            Formatter: (services) => new SerializerFormatter(services)
        },
        AstSerializer: (services) => new DefaultAstSerializer(services)
    },
    postCreate(services) {
        initializeLangiumTypirServices(services as any, services.typir);
        registerDefaultTokenSerializers(services);
        registerTypeSerializers(services, typeTypes);
        registerExpressionSerializers(services, expressionTypes);
        registerStatementSerializers(services, statementTypes);
        registerScriptSerializers(services);
        addExternalReferenceCollectionPhase(services);
    }
};

/**
 * Provider for the Script language plugin.
 */
export const scriptPluginProvider: LangiumLanguagePluginProvider<ScriptServices> = {
    create(): LangiumLanguagePlugin<ScriptServices> {
        return scriptPlugin;
    }
};

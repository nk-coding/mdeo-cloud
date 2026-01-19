import {
    HIDDEN_NEWLINE,
    ML_COMMENT,
    SL_COMMENT,
    WS,
    type AstReflection,
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
import { registerScriptSerializers } from "./features/scriptSerializers.js";
import { expressionTypes, statementTypes, typeTypes } from "./grammar/scriptTypes.js";
import { ScriptLangiumScopeProvider } from "./features/scriptScopeProvider.js";
import { ScriptExternalReferenceCollector } from "./features/scriptExternalReferenceCollector.js";
import type { ResolvedScriptContributionPlugins, ScriptContributionPlugin } from "./plugin/scriptContributionPlugin.js";
import { generateScriptRule } from "./grammar/scriptRules.js";
import type { AbstractAstReflection } from "langium";

const { createTypirLangiumServicesWithAdditionalServices, initializeLangiumTypirServices } =
    sharedImport("typir-langium");

/**
 * The Typir specifics for the Script language.
 */
export type ScriptTypirSpecifics = TypirLangiumSpecifics;

/**
 * The resolved contribution plugins for the Script language.
 */
type AdditionalScriptTypirServices = AdditionalTypirServices<ScriptTypirSpecifics> & {
    ResolvedContributionPlugins: ResolvedScriptContributionPlugins;
};

/**
 * Typir services with additional services for the Script language.
 */
export type ScriptTypirServices = ExpressionTypirServices<ScriptTypirSpecifics> & AdditionalScriptTypirServices;

/**
 * The additional services for the Script language.
 */
export type ScriptServices = {
    typir: ScriptTypirServices;
} & ExternalReferenceAdditionalServices;

/**
 * Provider for the Script language plugin.
 */
export const scriptPluginProvider: LangiumLanguagePluginProvider<ScriptServices> = {
    create(contributionPlugins: ScriptContributionPlugin[]): LangiumLanguagePlugin<ScriptServices> {
        const { rule, resolvedPlugins } = generateScriptRule(contributionPlugins);
        return {
            rootRule: rule,
            additionalTerminals: [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT],
            module: {
                parser: {
                    TokenBuilder: () =>
                        new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
                    ValueConverter: () => new IdValueConverter()
                },
                references: {
                    ScopeProvider: (services) => new ScriptLangiumScopeProvider(services),
                    ExternalReferenceCollector: () => new ScriptExternalReferenceCollector()
                },
                typir: (services) =>
                    createTypirLangiumServicesWithAdditionalServices<
                        ScriptTypirSpecifics,
                        AdditionalScriptTypirServices
                    >(
                        services.shared,
                        services.shared.AstReflection as AbstractAstReflection & AstReflection,
                        new ScriptTypeSystem(resolvedPlugins),
                        {
                            ...defaultExtendedTypirServices<ScriptTypirSpecifics>(),
                            ScopeProvider: (services) => new ScriptScopeProvider(services as ScriptTypirServices),
                            ResolvedContributionPlugins: () => resolvedPlugins
                        }
                    ) as ScriptTypirServices,
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
    }
};

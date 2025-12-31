import {
    generateIdValueConverter,
    generateNewlineAwareTokenBuilder,
    HIDDEN_NEWLINE,
    ML_COMMENT,
    SL_COMMENT,
    WS,
    type LanguagePluginProvider,
    type ServiceProvider,
    type AstSerializerAdditionalServices,
    generateDefaultAstSerializer,
    generateSerializerFormatter,
    registerDefaultTokenSerializers
} from "@mdeo/language-common";
import { ScriptRule } from "./grammar/rule.js";
import {
    defaultExtendedTypirServices,
    type AdditionalTypirServices,
    type ExpressionTypirServices,
    registerExpressionSerializers,
    registerStatementSerializers,
    registerTypeSerializers
} from "@mdeo/language-expression";
import { type TypirLangiumSpecifics } from "typir-langium";
import { ScriptTypeSystem } from "./features/typeSystem.js";
import { ScriptScopeProvider } from "./features/scopeProvider.js";
import { registerScriptSerializers } from "./features/scriptSerializers.js";
import { expressionTypes, statementTypes, typeTypes } from "./grammar/types.js";

/**
 * The Typir specifics for the Script language.
 */
export type ScriptTypirSpecifics = TypirLangiumSpecifics;

/**
 * The additional services for the Script language.
 */
export type ScriptServices = {
    typir: ExpressionTypirServices<ScriptTypirSpecifics>;
} & AstSerializerAdditionalServices;

/**
 * The service provider for the Script language.
 * Used for the plugin architecture.
 */
export type ScriptServiceProvider<T> = ServiceProvider<ScriptServices, T>;

/**
 * The plugin provider for the Script language.
 */
export const scriptPluginProvider: LanguagePluginProvider<ScriptServices> = {
    generate: (context) => ({
        rootRule: ScriptRule,
        additionalTerminals: [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT],
        module: {
            parser: {
                ...generateNewlineAwareTokenBuilder(context, new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
                ...generateIdValueConverter(context)
            },
            typir: (services) =>
                context["typir-langium"].createTypirLangiumServicesWithAdditionalServices<
                    ScriptTypirSpecifics,
                    AdditionalTypirServices<ScriptTypirSpecifics>
                >(
                    services.shared as any,
                    services.shared.AstReflection as any,
                    new ScriptTypeSystem(),
                    defaultExtendedTypirServices<ScriptTypirSpecifics>(context),
                    {
                        ScopeProvider: (services) =>
                            new ScriptScopeProvider(services as ExpressionTypirServices<ScriptTypirSpecifics>)
                    }
                ) as ExpressionTypirServices<ScriptTypirSpecifics>,
            lsp: {
                ...generateSerializerFormatter(context)
            },
            ...generateDefaultAstSerializer(context)
        },
        postCreate(services) {
            context["typir-langium"].initializeLangiumTypirServices(services as any, services.typir);
            registerDefaultTokenSerializers(context, services);
            registerTypeSerializers(context, services, typeTypes);
            registerExpressionSerializers(context, services, expressionTypes);
            registerStatementSerializers(context, services, statementTypes);
            registerScriptSerializers(context, services);
        }
    })
};

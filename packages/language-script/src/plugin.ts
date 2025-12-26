import {
    generateNewlineAwareTokenBuilder,
    HIDDEN_NEWLINE,
    ML_COMMENT,
    SL_COMMENT,
    WS,
    type LanguagePluginProvider,
    type ServiceProvider
} from "@mdeo/language-common";
import { ScriptRule } from "./grammar/rule.js";
import {
    defaultExtendedTypirServices,
    type AdditionalTypirServices,
    type ExtendedTypirServices
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";
import { ScriptTypeSystem } from "./services/typeSystem.js";

export interface ScriptTypirSpecifics extends TypirLangiumSpecifics {}

/**
 * The additional services for the Script language.
 */
export type ScriptServices = {
    typir: ExtendedTypirServices<ScriptTypirSpecifics>;
};

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
                ...generateNewlineAwareTokenBuilder(context, new Set(["{"]), new Set(["("]), new Set(["}", ")"]))
            },
            typir: (services) =>
                context["typir-langium"].createTypirLangiumServicesWithAdditionalServices<
                    ScriptTypirSpecifics,
                    AdditionalTypirServices<ScriptTypirSpecifics>
                >(
                    services.shared as any,
                    services.shared.AstReflection as any,
                    new ScriptTypeSystem(),
                    defaultExtendedTypirServices<ScriptTypirSpecifics>(context)
                )
        }
    })
};

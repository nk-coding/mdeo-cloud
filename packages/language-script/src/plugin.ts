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

/**
 * The additional services for the Script language.
 */
export type ScriptServices = object;

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
            }
        }
    })
};

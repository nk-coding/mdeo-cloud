import { MetaModelRule } from "./grammar/rules.js";
import {
    type LanguagePluginProvider,
    WS,
    ML_COMMENT,
    SL_COMMENT,
    type ServiceProvider,
    HIDDEN_NEWLINE
} from "@mdeo/language-common";
import { MetamodelScopeProvider } from "./services/scopeProvider.js";

/**
 * The additional services for the Metamodel language.
 */
export type MetamodelServices = object;

/**
 * The service provider for the Metamodel language.
 * Used for the plugin architecture.
 */
export type MetamodelServiceProvider<T> = ServiceProvider<MetamodelServices, T>;

/**
 * The plugin provider for the Metamodel language.
 */
export const metamodelPluginProvider: LanguagePluginProvider<MetamodelServices> = {
    generate: (context) => ({
        rootRule: MetaModelRule,
        additionalTerminals: [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT],
        module: {
            references: {
                ScopeProvider: MetamodelScopeProvider(context)
            }
        }
    })
};

import { MetaModelRule } from "./grammar/rules.js";
import { type LanguagePluginProvider, WS, ML_COMMENT, SL_COMMENT, type ServiceProvider } from "@mdeo/language-common";
import { MetamodelScopeProvider } from "./services/scopeProvider.js";

export type MetamodelServices = {};

export type MetamodelServiceProvider<T> = ServiceProvider<MetamodelServices, T>;

export const metamodelPluginProvider: LanguagePluginProvider<{}> = {
    generate: (context) => ({
        rootRule: MetaModelRule,
        additionalTerminals: [WS, ML_COMMENT, SL_COMMENT],
        module: {
            references: {
                ScopeProvider: MetamodelScopeProvider(context)
            }
        }
    })
};

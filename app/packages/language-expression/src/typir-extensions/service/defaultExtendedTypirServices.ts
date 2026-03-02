import type { DeepPartial, Module } from "typir";
import type { AdditionalTypirServices, ExtendedTypirLangiumServices } from "./extendedTypirServices.js";
import { DefaultTypeDefinitionService } from "./typeDefinitionService.js";
import { CustomClassKind, CustomClassKindName } from "../kinds/custom-class/custom-class-kind.js";
import { CustomLambdaKind, CustomLambdaKindName } from "../kinds/custom-lambda/custom-lambda-kind.js";
import { CustomFunctionKind, CustomFunctionKindName } from "../kinds/custom-function/custom-function-kind.js";
import { CustomValueKind, CustomValueKindName } from "../kinds/custom-value/custom-value-kind.js";
import { CustomVoidKind, CustomVoidKindName } from "../kinds/custom-void/custom-void-kind.js";
import { CustomNullKind, CustomNullKindName } from "../kinds/custom-null/custom-null-kind.js";
import type { TypirLangiumAddedServices, TypirLangiumServices, TypirLangiumSpecifics } from "typir-langium";
import { generateTypeCreator } from "../langium/typeCreator.js";
import { generateScopeProviderCache } from "../langium/scopeProviderCache.js";
import { DefaultScopeProvider } from "../scope/scopeProvider.js";
import { CustomTypeGraph } from "./customTypeGraph.js";

/**
 * Provides the default implementation for the additional typir services
 *
 * @returns The module with the default extended typir services
 */
export function defaultExtendedTypirServices<Specifics extends TypirLangiumSpecifics>(): Module<
    ExtendedTypirLangiumServices<Specifics> & DeepPartial<TypirLangiumAddedServices<Specifics>>,
    AdditionalTypirServices<Specifics> & DeepPartial<TypirLangiumServices<Specifics>>
> {
    return {
        factory: {
            CustomClasses: (services) =>
                services.infrastructure.Kinds.getOrCreateKind(
                    CustomClassKindName,
                    () => new CustomClassKind<Specifics>(services)
                ),
            CustomLambdas: (services) =>
                services.infrastructure.Kinds.getOrCreateKind(
                    CustomLambdaKindName,
                    () => new CustomLambdaKind<Specifics>(services)
                ),
            CustomFunctions: (services) =>
                services.infrastructure.Kinds.getOrCreateKind(
                    CustomFunctionKindName,
                    () => new CustomFunctionKind<Specifics>(services)
                ),
            CustomValues: (services) =>
                services.infrastructure.Kinds.getOrCreateKind(
                    CustomValueKindName,
                    () => new CustomValueKind<Specifics>(services)
                ),
            CustomVoid: (services) =>
                services.infrastructure.Kinds.getOrCreateKind(
                    CustomVoidKindName,
                    () => new CustomVoidKind<Specifics>(services)
                ),
            CustomNull: (services) =>
                services.infrastructure.Kinds.getOrCreateKind(
                    CustomNullKindName,
                    () => new CustomNullKind<Specifics>(services)
                )
        },
        caching: {
            ...generateScopeProviderCache<Specifics>()
        },
        langium: {
            ...generateTypeCreator<Specifics>()
        },
        infrastructure: {
            Graph: () => new CustomTypeGraph()
        },
        TypeDefinitions: (services) => new DefaultTypeDefinitionService(services),
        ScopeProvider: () => new DefaultScopeProvider<Specifics>()
    };
}

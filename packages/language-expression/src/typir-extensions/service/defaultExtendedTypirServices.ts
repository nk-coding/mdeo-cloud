import type { Module, TypirSpecifics } from "typir";
import type { AdditionalTypirServices, ExtendedTypirServices } from "./extendedTypirServices.js";
import { DefaultTypeDefinitionService } from "./type-definition-service.js";
import type { PluginContext } from "@mdeo/language-common";
import { CustomClassKind, CustomClassKindName } from "../kinds/custom-class/custom-class-kind.js";
import { CustomLambdaKind, CustomLambdaKindName } from "../kinds/custom-lambda/custom-lambda-kind.js";
import { CustomFunctionKind, CustomFunctionKindName } from "../kinds/custom-function/custom-function-kind.js";
import { CustomValueKind, CustomValueKindName } from "../kinds/custom-value/custom-value-kind.js";

/**
 * Provides the default implementation for the additional typir services
 * 
 * @param context The plugin context
 * @returns The module with the default extended typir services
 */
export function defaultExtendedTypirServices<Specifics extends TypirSpecifics>(
    context: PluginContext
): Module<ExtendedTypirServices<Specifics>, AdditionalTypirServices<Specifics>> {
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
                )
        },
        TypeDefinitions: (services) => new DefaultTypeDefinitionService(services),
        context: () => context
    };
}

import {
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
    DefaultActionProvider,
    ActionHandlerRegistry,
    type ActionHandlerRegistryAdditionalServices
} from "@mdeo/language-shared";
import {
    defaultExtendedTypirServices,
    type AdditionalTypirServices,
    type ExpressionTypirServices,
    registerExpressionSerializers,
    registerTypeSerializers
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";
import type { AbstractAstReflection } from "langium";
import { generateModelTransformationRules, ModelTransformationTerminals } from "./grammar/modelTransformationRules.js";
import { expressionTypes, typeTypes } from "./grammar/modelTransformationTypes.js";
import { ModelTransformationLangiumScopeProvider } from "./features/modelTransformationScopeProvider.js";
import { ModelTransformationTypeSystem } from "./features/type-system/modelTransformationTypeSystem.js";
import { ModelTransformationTypirScopeProvider } from "./features/type-system/modelTransformationTypirScopeProvider.js";
import { registerModelTransformationSerializers } from "./features/modelTransformationSerializers.js";
import { ModelTransformationExternalReferenceCollector } from "./features/modelTransformationExternalReferenceCollector.js";
import { NewFileActionHandler } from "./action-handlers/newFileActionHandler.js";
import { addExternalReferenceCollectionPhase } from "@mdeo/language-shared";

const { createTypirLangiumServicesWithAdditionalServices, initializeLangiumTypirServices } =
    sharedImport("typir-langium");

/**
 * Typir specifics for the Model Transformation language.
 */
export type ModelTransformationTypirSpecifics = TypirLangiumSpecifics;

/**
 * Additional Typir services for Model Transformation.
 */
type AdditionalModelTransformationTypirServices = AdditionalTypirServices<ModelTransformationTypirSpecifics>;

/**
 * Typir services for Model Transformation.
 */
export type ModelTransformationTypirServices = ExpressionTypirServices<ModelTransformationTypirSpecifics> &
    AdditionalModelTransformationTypirServices;

/**
 * Additional services for the Model Transformation language.
 */
export type ModelTransformationServices = {
    typir: ModelTransformationTypirServices;
} & ExternalReferenceAdditionalServices &
    ActionHandlerRegistryAdditionalServices;

/**
 * Creates the Model Transformation language plugin.
 *
 * @returns The Model Transformation language plugin.
 */
function createModelTransformationPlugin(): LangiumLanguagePlugin<ModelTransformationServices> {
    const { rule } = generateModelTransformationRules();

    return {
        rootRule: rule,
        additionalTerminals: ModelTransformationTerminals,
        module: {
            parser: {
                TokenBuilder: () => new NewlineAwareTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
                ValueConverter: () => new IdValueConverter()
            },
            references: {
                ScopeProvider: (services) => new ModelTransformationLangiumScopeProvider(services),
                ExternalReferenceCollector: () => new ModelTransformationExternalReferenceCollector()
            },
            typir: (services) =>
                createTypirLangiumServicesWithAdditionalServices<
                    ModelTransformationTypirSpecifics,
                    AdditionalModelTransformationTypirServices
                >(
                    services.shared,
                    services.shared.AstReflection as AbstractAstReflection & AstReflection,
                    new ModelTransformationTypeSystem(),
                    {
                        ...defaultExtendedTypirServices<ModelTransformationTypirSpecifics>(),
                        ScopeProvider: (typirServices) =>
                            new ModelTransformationTypirScopeProvider(
                                typirServices as ExpressionTypirServices<ModelTransformationTypirSpecifics>
                            )
                    }
                ) as ModelTransformationTypirServices,
            lsp: {
                Formatter: (services) => new SerializerFormatter(services)
            },
            AstSerializer: (services) => new DefaultAstSerializer(services),
            action: {
                ActionHandlerRegistry: (services) => {
                    const registry = new ActionHandlerRegistry();
                    registry.register("new-file", new NewFileActionHandler(services.shared));
                    return registry;
                },
                ActionProvider: () => new DefaultActionProvider()
            }
        },
        postCreate(services) {
            initializeLangiumTypirServices(services as any, services.typir);
            registerDefaultTokenSerializers(services);
            registerTypeSerializers(services, typeTypes);
            registerExpressionSerializers(services, expressionTypes);
            registerModelTransformationSerializers(services);
            addExternalReferenceCollectionPhase(services);
        }
    };
}

/**
 * Provider for the Model Transformation language plugin.
 */
export const modelTransformationPluginProvider: LangiumLanguagePluginProvider<ModelTransformationServices> = {
    create(): LangiumLanguagePlugin<ModelTransformationServices> {
        return createModelTransformationPlugin();
    }
};

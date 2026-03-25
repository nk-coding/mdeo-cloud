import {
    type AstReflection,
    type ExternalReferenceAdditionalServices,
    type LangiumLanguagePlugin,
    type LangiumLanguagePluginProvider
} from "@mdeo/language-common";
import {
    IdValueConverter,
    DefaultAstSerializer,
    SerializerFormatter,
    registerDefaultTokenSerializers,
    sharedImport,
    ActionHandlerRegistry,
    generateExtendedParser,
    type ActionHandlerRegistryAdditionalServices,
    DefaultWorkspaceEditService
} from "@mdeo/language-shared";
import {
    defaultExtendedTypirServices,
    type AdditionalTypirServices,
    type ExpressionTypirServices,
    registerExpressionSerializers,
    registerTypeSerializers,
    generateExpressionRuleOverride,
    DefaultDocumentPackageCacheService,
    type DocumentPackageCacheService,
    getClassPackage,
    getEnumPackage
} from "@mdeo/language-expression";
import { getAllMetamodelAbsolutePaths } from "@mdeo/language-metamodel";
import { resolveRelativePath } from "@mdeo/language-shared";
import type { TypirLangiumSpecifics } from "typir-langium";
import type { AbstractAstReflection, LangiumDocument } from "langium";
import { generateModelTransformationRules, ModelTransformationTerminals } from "./grammar/modelTransformationRules.js";
import { ModelTransformationCompletionProvider } from "./features/modelTransformationCompletionProvider.js";
import { ModelTransformationTokenBuilder } from "./features/modelTransformationTokenBuilder.js";
import {
    expressionConfig,
    expressionTypes,
    typeTypes,
    type ModelTransformationType
} from "./grammar/modelTransformationTypes.js";
import { ModelTransformationLangiumScopeProvider } from "./features/modelTransformationScopeProvider.js";
import { ModelTransformationScopeComputation } from "./features/modelTransformationScopeComputation.js";
import { ModelTransformationTypeSystem } from "./features/type-system/modelTransformationTypeSystem.js";
import { ModelTransformationTypirScopeProvider } from "./features/type-system/modelTransformationTypirScopeProvider.js";
import { registerModelTransformationSerializers } from "./features/modelTransformationSerializers.js";
import { ModelTransformationExternalReferenceCollector } from "./features/modelTransformationExternalReferenceCollector.js";
import { NewFileActionHandler } from "./action-handlers/newFileActionHandler.js";
import { RunModelTransformationActionHandler } from "./action-handlers/runModelTransformationActionHandler.js";
import { ModelTransformationActionProvider } from "./features/modelTransformationActionProvider.js";
import { addExternalReferenceCollectionPhase } from "@mdeo/language-shared";
import { registerModelTransformationValidationChecks } from "./validation/modelTransformationValidator.js";
import { ModelTransformationDiagramModule } from "./features/diagram-server/modelTransformationDiagramModule.js";

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
                TokenBuilder: () =>
                    new ModelTransformationTokenBuilder(new Set(["{"]), new Set(["("]), new Set(["}", ")"])),
                ValueConverter: () => new IdValueConverter(),
                ...generateExtendedParser(generateExpressionRuleOverride(expressionConfig)),
                ParserConfig: () => ({
                    maxLookahead: 4
                })
            },
            references: {
                ScopeProvider: (services) => new ModelTransformationLangiumScopeProvider(services),
                ScopeComputation: (services) => new ModelTransformationScopeComputation(services),
                ExternalReferenceCollector: () => new ModelTransformationExternalReferenceCollector()
            },
            typir: (outerServices) => {
                const langiumSharedServices = outerServices.shared;
                const computePackageMap = (document: LangiumDocument): Map<string, string[]> => {
                    const map = new Map<string, string[]>();
                    map.set("builtin", ["builtin"]);

                    const root = document.parseResult?.value as ModelTransformationType | undefined;
                    const importFile = root?.import?.file;
                    if (importFile == undefined) {
                        return map;
                    }

                    const langiumDocuments = langiumSharedServices.workspace.LangiumDocuments;
                    const metamodelUri = resolveRelativePath(document, importFile);
                    const metamodelDoc = langiumDocuments.getDocument(metamodelUri);
                    if (metamodelDoc == undefined) {
                        return map;
                    }

                    const absolutePaths = getAllMetamodelAbsolutePaths(metamodelDoc, langiumDocuments);
                    const classPackages: string[] = [];
                    const enumPackages: string[] = [];
                    for (const absolutePath of absolutePaths) {
                        classPackages.push(getClassPackage(absolutePath));
                        enumPackages.push(getEnumPackage(absolutePath));
                    }
                    map.set("class", classPackages);
                    map.set("enum", enumPackages);
                    return map;
                };

                return createTypirLangiumServicesWithAdditionalServices<
                    ModelTransformationTypirSpecifics,
                    AdditionalModelTransformationTypirServices
                >(
                    langiumSharedServices,
                    langiumSharedServices.AstReflection as AbstractAstReflection & AstReflection,
                    new ModelTransformationTypeSystem(),
                    {
                        ...defaultExtendedTypirServices<ModelTransformationTypirSpecifics>(),
                        ScopeProvider: (typirServices) =>
                            new ModelTransformationTypirScopeProvider(
                                typirServices as ExpressionTypirServices<ModelTransformationTypirSpecifics>
                            ),
                        PackageMapCache: (): DocumentPackageCacheService =>
                            new DefaultDocumentPackageCacheService(langiumSharedServices, computePackageMap)
                    }
                ) as ModelTransformationTypirServices;
            },
            lsp: {
                CompletionProvider: (services) => new ModelTransformationCompletionProvider(services, expressionTypes),
                Formatter: (services) => new SerializerFormatter(services)
            },
            AstSerializer: (services) => new DefaultAstSerializer(services),
            action: {
                ActionHandlerRegistry: (services) => {
                    const registry = new ActionHandlerRegistry();
                    registry.register("new-file", new NewFileActionHandler(services.shared));
                    registry.register("run", new RunModelTransformationActionHandler(services.shared));
                    return registry;
                },
                ActionProvider: () => new ModelTransformationActionProvider()
            },
            workspace: {
                WorkspaceEdit: (services) => new DefaultWorkspaceEditService(services)
            }
        },
        postCreate(services) {
            initializeLangiumTypirServices(services, services.typir);
            registerDefaultTokenSerializers(services);
            registerTypeSerializers(services, typeTypes);
            registerExpressionSerializers(services, expressionTypes);
            registerModelTransformationSerializers(services);
            registerModelTransformationValidationChecks(services);
            services.shared.glsp.serverModule.configureDiagramModule(new ModelTransformationDiagramModule(services));
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

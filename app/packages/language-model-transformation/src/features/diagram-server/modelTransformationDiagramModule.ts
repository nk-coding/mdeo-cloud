import type {
    BindingTarget,
    DiagramConfiguration,
    GModelFactory,
    InstanceMultiBinding,
    OperationHandlerConstructor,
    ToolPaletteItemProvider
} from "@eclipse-glsp/server";
import type { BaseLayoutEngine, MetadataManager, ModelIdProvider } from "@mdeo/language-shared";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { ModelTransformationGModelFactory } from "./modelTransformationGModelFactory.js";
import { ModelTransformationDiagramConfiguration } from "./modelTransformationDiagramConfiguration.js";
import { ModelTransformationModelIdProvider } from "./modelTransformationModelIdProvider.js";
import { ModelTransformationMetadataManager } from "./modelTransformationMetadataManager.js";
import { ModelTransformationLayoutEngine } from "./modelTransformationLayoutEngine.js";
import { ModelTransformationCreateEdgeSchemaResolver } from "./modelTransformationCreateEdgeSchemaResolver.js";
import { CreatePatternInstanceOperationHandler } from "./handler/createPatternInstanceOperationHandler.js";
import { CreatePatternLinkOperationHandler } from "./handler/createPatternLinkOperationHandler.js";
import { ModelTransformationToolPaletteItemProvider } from "./modelTransformationToolPaletteItemProvider.js";
import type { CreateEdgeSchemaResolver } from "@mdeo/language-shared";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for model transformation visualizations.
 * Configures the GLSP diagram with model-transformation-specific factory and configuration.
 */
@injectable()
export class ModelTransformationDiagramModule extends BaseDiagramModule {
    protected override bindDiagramConfiguration(): BindingTarget<DiagramConfiguration> {
        return ModelTransformationDiagramConfiguration;
    }

    protected override bindGModelFactory(): BindingTarget<GModelFactory> {
        return ModelTransformationGModelFactory;
    }

    protected override bindModelIdProvider(): BindingTarget<ModelIdProvider> {
        return ModelTransformationModelIdProvider;
    }

    protected override bindMetadataManager(): BindingTarget<MetadataManager> {
        return ModelTransformationMetadataManager;
    }

    protected override bindCustomLayoutEngine(): BindingTarget<BaseLayoutEngine> {
        return ModelTransformationLayoutEngine;
    }

    protected override bindCreateEdgeSchemaResolver(): BindingTarget<CreateEdgeSchemaResolver> {
        return ModelTransformationCreateEdgeSchemaResolver;
    }

    protected override configureOperationHandlers(binding: InstanceMultiBinding<OperationHandlerConstructor>): void {
        super.configureOperationHandlers(binding);
        binding.add(CreatePatternInstanceOperationHandler);
        binding.add(CreatePatternLinkOperationHandler);
    }

    protected override bindToolPaletteItemProvider(): BindingTarget<ToolPaletteItemProvider> {
        return ModelTransformationToolPaletteItemProvider;
    }
}

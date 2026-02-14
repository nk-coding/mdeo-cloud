import type { BindingTarget, DiagramConfiguration, GModelFactory } from "@eclipse-glsp/server";
import type { BaseLayoutEngine, MetadataManager, ModelIdProvider } from "@mdeo/language-shared";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { ModelTransformationGModelFactory } from "./modelTransformationGModelFactory.js";
import { ModelTransformationDiagramConfiguration } from "./modelTransformationDiagramConfiguration.js";
import { ModelTransformationModelIdProvider } from "./modelTransformationModelIdProvider.js";
import { ModelTransformationMetadataManager } from "./modelTransformationMetadataManager.js";
import { ModelTransformationLayoutEngine } from "./modelTransformationLayoutEngine.js";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for model transformation visualizations.
 * Configures the GLSP diagram with model-transformation-specific factory and configuration.
 */
@injectable()
export class ModelTransformationDiagramModule extends BaseDiagramModule {
    /**
     * Binds the diagram configuration for model transformation diagrams.
     *
     * @returns The ModelTransformationDiagramConfiguration class
     */
    protected override bindDiagramConfiguration(): BindingTarget<DiagramConfiguration> {
        return ModelTransformationDiagramConfiguration;
    }

    /**
     * Binds the GModel factory for creating graph models from AST.
     *
     * @returns The ModelTransformationGModelFactory class
     */
    protected override bindGModelFactory(): BindingTarget<GModelFactory> {
        return ModelTransformationGModelFactory;
    }

    /**
     * Binds the model ID provider for generating element IDs.
     *
     * @returns The ModelTransformationModelIdProvider class
     */
    protected override bindModelIdProvider(): BindingTarget<ModelIdProvider> {
        return ModelTransformationModelIdProvider;
    }

    /**
     * Binds the metadata manager for handling layout metadata.
     *
     * @returns The ModelTransformationMetadataManager class
     */
    protected override bindMetadataManager(): BindingTarget<MetadataManager> {
        return ModelTransformationMetadataManager;
    }

    /**
     * Binds the layout engine for automatic layout computation.
     *
     * @returns The ModelTransformationLayoutEngine class
     */
    protected override bindCustomLayoutEngine(): BindingTarget<BaseLayoutEngine> {
        return ModelTransformationLayoutEngine;
    }
}

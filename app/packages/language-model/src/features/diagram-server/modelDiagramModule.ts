import type {
    InstanceMultiBinding,
    LabelEditValidator,
    OperationHandlerConstructor,
    ToolPaletteItemProvider
} from "@eclipse-glsp/server";
import type { BindingTarget, DiagramConfiguration, GModelFactory } from "@eclipse-glsp/server";
import type { BaseLayoutEngine, MetadataManager, ModelIdProvider } from "@mdeo/language-shared";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { ModelGModelFactory } from "./modelGModelFactory.js";
import { ModelDiagramConfiguration } from "./modelDiagramConfiguration.js";
import { ModelModelIdProvider } from "./modelModelIdProvider.js";
import { ModelMetadataManager } from "./modelMetadataManager.js";
import { ModelApplyLabelEditOperationHandler } from "./handler/modelApplyLabelEditOperationHandler.js";
import { ModelReconnectEdgeOperationHandler } from "./handler/modelReconnectEdgeOperationHandler.js";
import { CreateObjectOperationHandler } from "./handler/createObjectOperationHandler.js";
import { ModelDeleteElementOperationHandler } from "./handler/modelDeleteElementOperationHandler.js";
import { ModelLabelEditValidator } from "./modelLabelEditValidator.js";
import { ModelToolPaletteItemProvider } from "./modelToolPaletteItemProvider.js";
import { ModelLayoutEngine } from "./modelLayoutEngine.js";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for model visualizations.
 * Configures the GLSP diagram with model-specific factory and configuration.
 */
@injectable()
export class ModelDiagramModule extends BaseDiagramModule {
    /**
     * Binds the diagram configuration for model diagrams.
     *
     * @returns The ModelDiagramConfiguration class
     */
    protected override bindDiagramConfiguration(): BindingTarget<DiagramConfiguration> {
        return ModelDiagramConfiguration;
    }

    /**
     * Binds the GModel factory for creating graph models from AST.
     *
     * @returns The ModelGModelFactory class
     */
    protected override bindGModelFactory(): BindingTarget<GModelFactory> {
        return ModelGModelFactory;
    }

    /**
     * Binds the model ID provider for generating element IDs.
     *
     * @returns The ModelModelIdProvider class
     */
    protected override bindModelIdProvider(): BindingTarget<ModelIdProvider> {
        return ModelModelIdProvider;
    }

    /**
     * Binds the metadata manager for handling layout metadata.
     *
     * @returns The ModelMetadataManager class
     */
    protected override bindMetadataManager(): BindingTarget<MetadataManager> {
        return ModelMetadataManager;
    }

    /**
     * Configures operation handlers for model diagram operations.
     *
     * @param binding The binding to add operation handlers to
     */
    protected override configureOperationHandlers(binding: InstanceMultiBinding<OperationHandlerConstructor>): void {
        super.configureOperationHandlers(binding);
        binding.add(ModelApplyLabelEditOperationHandler);
        binding.add(ModelReconnectEdgeOperationHandler);
        binding.add(CreateObjectOperationHandler);
        binding.add(ModelDeleteElementOperationHandler);
    }

    /**
     * Binds the label edit validator for validating label edits.
     *
     * @returns The ModelLabelEditValidator class
     */
    protected override bindLabelEditValidator(): BindingTarget<LabelEditValidator> {
        return ModelLabelEditValidator;
    }

    /**
     * Binds the tool palette item provider for the diagram toolbox.
     *
     * @returns The ModelToolPaletteItemProvider class
     */
    protected override bindToolPaletteItemProvider(): BindingTarget<ToolPaletteItemProvider> {
        return ModelToolPaletteItemProvider;
    }

    /**
     * Binds the layout engine for automatic layout computation.
     *
     * @returns The ModelLayoutEngine class
     */
    protected override bindCustomLayoutEngine(): BindingTarget<BaseLayoutEngine> {
        return ModelLayoutEngine;
    }
}

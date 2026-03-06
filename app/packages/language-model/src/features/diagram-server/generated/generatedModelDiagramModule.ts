import type { BindingTarget, DiagramConfiguration, GModelFactory, ToolPaletteItemProvider } from "@eclipse-glsp/server";
import type { BaseLayoutEngine, MetadataManager, ModelIdProvider } from "@mdeo/language-shared";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { GeneratedModelGModelFactory } from "./generatedModelGModelFactory.js";
import { GeneratedModelDiagramConfiguration } from "./generatedModelDiagramConfiguration.js";
import { GeneratedModelModelIdProvider } from "./generatedModelModelIdProvider.js";
import { GeneratedModelMetadataManager } from "./generatedModelMetadataManager.js";
import { GeneratedModelToolPaletteItemProvider } from "./generatedModelToolPaletteItemProvider.js";
import { ModelLayoutEngine } from "../modelLayoutEngine.js";
import { GeneratedModelCreateEdgeSchemaResolver } from "./generatedModelCreateEdgeSchemaResolver.js";
import type { CreateEdgeSchemaResolver } from "@mdeo/language-shared";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for generated model visualizations.
 * Configures the GLSP diagram with generated model-specific factory and configuration.
 * This is a read-only diagram with no operation handlers beyond the defaults.
 */
@injectable()
export class GeneratedModelDiagramModule extends BaseDiagramModule {
    /**
     * Binds the diagram configuration for generated model diagrams.
     */
    protected override bindDiagramConfiguration(): BindingTarget<DiagramConfiguration> {
        return GeneratedModelDiagramConfiguration;
    }

    /**
     * Binds the GModel factory for creating graph models from generated model JSON.
     */
    protected override bindGModelFactory(): BindingTarget<GModelFactory> {
        return GeneratedModelGModelFactory;
    }

    /**
     * Binds the model ID provider for generating element IDs.
     */
    protected override bindModelIdProvider(): BindingTarget<ModelIdProvider> {
        return GeneratedModelModelIdProvider;
    }

    /**
     * Binds the metadata manager for handling layout metadata.
     */
    protected override bindMetadataManager(): BindingTarget<MetadataManager> {
        return GeneratedModelMetadataManager;
    }

    /**
     * Binds the tool palette item provider (empty for read-only diagrams).
     */
    protected override bindToolPaletteItemProvider(): BindingTarget<ToolPaletteItemProvider> {
        return GeneratedModelToolPaletteItemProvider;
    }

    /**
     * Reuses the model layout engine for automatic layout computation.
     */
    protected override bindCustomLayoutEngine(): BindingTarget<BaseLayoutEngine> {
        return ModelLayoutEngine;
    }

    protected override bindCreateEdgeSchemaResolver(): BindingTarget<CreateEdgeSchemaResolver> {
        return GeneratedModelCreateEdgeSchemaResolver;
    }
}

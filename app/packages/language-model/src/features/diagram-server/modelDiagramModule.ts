import type {
    InstanceMultiBinding,
    LabelEditValidator,
    OperationHandlerConstructor,
    ToolPaletteItemProvider,
    ActionHandlerConstructor
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
import { ModelCreateEdgeSchemaResolver } from "./modelCreateEdgeSchemaResolver.js";
import { CreateLinkOperationHandler } from "./handler/createLinkOperationHandler.js";
import { AddPropertyValueOperationHandler } from "./handler/addPropertyValueOperationHandler.js";
import { ChangeLinkTypeOperationHandler } from "./handler/changeLinkTypeOperationHandler.js";
import type { CreateEdgeSchemaResolver } from "@mdeo/language-shared";
import { ModelRequestClipboardDataActionHandler } from "./handler/modelRequestClipboardDataActionHandler.js";
import { ModelPasteOperationHandler } from "./handler/modelPasteOperationHandler.js";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for model visualizations.
 * Configures the GLSP diagram with model-specific factory and configuration.
 */
@injectable()
export class ModelDiagramModule extends BaseDiagramModule {
    protected override bindDiagramConfiguration(): BindingTarget<DiagramConfiguration> {
        return ModelDiagramConfiguration;
    }

    protected override bindGModelFactory(): BindingTarget<GModelFactory> {
        return ModelGModelFactory;
    }

    protected override bindModelIdProvider(): BindingTarget<ModelIdProvider> {
        return ModelModelIdProvider;
    }

    protected override bindMetadataManager(): BindingTarget<MetadataManager> {
        return ModelMetadataManager;
    }

    protected override configureOperationHandlers(binding: InstanceMultiBinding<OperationHandlerConstructor>): void {
        super.configureOperationHandlers(binding);
        binding.add(ModelApplyLabelEditOperationHandler);
        binding.add(ModelReconnectEdgeOperationHandler);
        binding.add(CreateLinkOperationHandler);
        binding.add(CreateObjectOperationHandler);
        binding.add(ModelDeleteElementOperationHandler);
        binding.add(AddPropertyValueOperationHandler);
        binding.add(ChangeLinkTypeOperationHandler);
        binding.add(ModelPasteOperationHandler);
    }

    protected override configureActionHandlers(binding: InstanceMultiBinding<ActionHandlerConstructor>): void {
        super.configureActionHandlers(binding);
        binding.add(ModelRequestClipboardDataActionHandler);
    }

    protected override bindCreateEdgeSchemaResolver(): BindingTarget<CreateEdgeSchemaResolver> {
        return ModelCreateEdgeSchemaResolver;
    }

    protected override bindLabelEditValidator(): BindingTarget<LabelEditValidator> {
        return ModelLabelEditValidator;
    }

    protected override bindToolPaletteItemProvider(): BindingTarget<ToolPaletteItemProvider> {
        return ModelToolPaletteItemProvider;
    }

    protected override bindCustomLayoutEngine(): BindingTarget<BaseLayoutEngine> {
        return ModelLayoutEngine;
    }
}

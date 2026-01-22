import type {
    InstanceMultiBinding,
    LabelEditValidator,
    OperationHandlerConstructor,
    ToolPaletteItemProvider
} from "@eclipse-glsp/server";
import type { BindingTarget, DiagramConfiguration, GModelFactory } from "@eclipse-glsp/server";
import type { MetadataManager, ModelIdProvider } from "@mdeo/language-shared";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { MetamodelGModelFactory } from "./metamodelGModelFactory.js";
import { MetamodelDiagramConfiguration } from "./metamodelDiagramConfiguration.js";
import { MetamodelModelIdProvider } from "./metamodelModelIdProvider.js";
import { MetamodelMetadataManager } from "./metamodelMetadataManager.js";
import { MetamodelApplyLabelEditOperationHandler } from "./handler/metamodelApplyLabelEditOperationHandler.js";
import { MetamodelReconnectEdgeOperationHandler } from "./handler/metamodelReconnectEdgeOperationHandler.js";
import { CreateClassOperationHandler } from "./handler/createClassOperationHandler.js";
import { MetamodelDeleteNodeOperationHandler } from "./handler/metamodelDeleteElementOperationHandler.js";
import { MetamodelLabelEditValidator } from "./metamodelLabelEditValidator.js";
import { MetamodelToolPaletteItemProvider } from "./metamodelToolPaletteItemProvider.js";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for metamodel visualizations.
 * Configures the GLSP diagram with metamodel-specific factory and configuration.
 */
@injectable()
export class MetamodelDiagramModule extends BaseDiagramModule {
    protected override bindDiagramConfiguration(): BindingTarget<DiagramConfiguration> {
        return MetamodelDiagramConfiguration;
    }

    protected override bindGModelFactory(): BindingTarget<GModelFactory> {
        return MetamodelGModelFactory;
    }

    protected override bindModelIdProvider(): BindingTarget<ModelIdProvider> {
        return MetamodelModelIdProvider;
    }

    protected override bindMetadataManager(): BindingTarget<MetadataManager> {
        return MetamodelMetadataManager;
    }

    protected override configureOperationHandlers(binding: InstanceMultiBinding<OperationHandlerConstructor>): void {
        super.configureOperationHandlers(binding);
        binding.add(MetamodelApplyLabelEditOperationHandler);
        binding.add(MetamodelReconnectEdgeOperationHandler);
        binding.add(CreateClassOperationHandler);
        binding.add(MetamodelDeleteNodeOperationHandler);
    }

    protected override bindLabelEditValidator(): BindingTarget<LabelEditValidator> {
        return MetamodelLabelEditValidator;
    }

    protected override bindToolPaletteItemProvider(): BindingTarget<ToolPaletteItemProvider> {
        return MetamodelToolPaletteItemProvider;
    }
}

import type { InstanceMultiBinding, OperationHandlerConstructor } from "@eclipse-glsp/server";
import type { BindingTarget, DiagramConfiguration, GModelFactory } from "@eclipse-glsp/server";
import type { MetadataManager, ModelIdProvider } from "@mdeo/language-shared";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { MetamodelGModelFactory } from "./metamodelGModelFactory.js";
import { MetamodelDiagramConfiguration } from "./metamodelDiagramConfiguration.js";
import { MetamodelModelIdProvider } from "./metamodelModelIdProvider.js";
import { MetamodelMetadataManager } from "./metamodelMetadataManager.js";
import { ApplyLabelEditOperationHandler } from "./handler/applyLabelEditOperationHandler.js";

const { injectable } = sharedImport("inversify");

/**
 * Diagram module for metamodel visualizations.
 * Configures the GLSP diagram with metamodel-specific factory and configuration.
 */
@injectable()
export class MetamodelDiagramModule extends BaseDiagramModule {
    override readonly diagramType = "metamodel";

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
        binding.add(ApplyLabelEditOperationHandler);
    }
}

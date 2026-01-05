import type { BindingTarget, DiagramConfiguration, GModelFactory } from "@eclipse-glsp/server";
import { BaseDiagramModule, sharedImport } from "@mdeo/language-shared";
import { MetamodelGModelFactory } from "./metamodelGModelFactory.js";
import { MetamodelDiagramConfiguration } from "./metamodelDiagramConfiguration.js";

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
}

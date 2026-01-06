import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import { metamodelDiagramModule } from "./module.js";

/**
 * Editor plugin for the metamodel editor
 */
export const metamodelEditorPlugin: ContainerConfiguration = [metamodelDiagramModule];

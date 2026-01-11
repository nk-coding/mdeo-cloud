import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import { metamodelDiagramModule } from "./module.js";
import { DEFAULT_MODULES } from "@mdeo/editor-shared";

/**
 * Editor plugin for the metamodel editor
 */
export const metamodelEditorPlugin: ContainerConfiguration = [...DEFAULT_MODULES, metamodelDiagramModule];

import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import { modelTransformationDiagramModule } from "./module.js";
import { DEFAULT_MODULES } from "@mdeo/editor-shared";

/**
 * Editor plugin for the model transformation editor.
 * Includes:
 * - Default shared modules for diagram editing
 * - Model transformation diagram module for transformation-specific elements and views
 */
export const modelTransformationEditorPlugin: ContainerConfiguration = [
    ...DEFAULT_MODULES,
    modelTransformationDiagramModule
];

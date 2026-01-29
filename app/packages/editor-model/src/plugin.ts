import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import { modelDiagramModule } from "./module.js";
import { DEFAULT_MODULES } from "@mdeo/editor-shared";

/**
 * Editor plugin for the model editor.
 * Includes:
 * - Default shared modules for diagram editing
 * - Model diagram module for model-specific elements and views
 */
export const modelEditorPlugin: ContainerConfiguration = [...DEFAULT_MODULES, modelDiagramModule];

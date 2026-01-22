import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import { metamodelDiagramModule } from "./module.js";
import { DEFAULT_MODULES } from "@mdeo/editor-shared";

/**
 * Editor plugin for the metamodel editor.
 * Includes:
 * - Default shared modules for diagram editing
 * - Metamodel diagram module for metamodel-specific elements and views
 * - Metamodel toolbox module for connection type selection and palette items
 */
export const metamodelEditorPlugin: ContainerConfiguration = [...DEFAULT_MODULES, metamodelDiagramModule];

import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import { modelTransformationDiagramModule } from "./module.js";
import { DEFAULT_MODULES } from "@mdeo/editor-shared";
import { modelTransformationBoundsModule } from "./features/bounds/featureModule.js";
import { modelTransformationToolboxModule } from "./features/toolbox/featureModule.js";
import { modelTransformationIconRegistryModule } from "./features/icon-registry/featureModule.js";

/**
 * Editor plugin for the model transformation editor.
 * Includes:
 * - Default shared modules for diagram editing
 * - Model transformation diagram module for transformation-specific elements and views
 * - Model transformation toolbox module for node creation mode selection
 */
export const modelTransformationEditorPlugin: ContainerConfiguration = [
    ...DEFAULT_MODULES,
    modelTransformationBoundsModule,
    modelTransformationDiagramModule,
    modelTransformationToolboxModule,
    modelTransformationIconRegistryModule
];

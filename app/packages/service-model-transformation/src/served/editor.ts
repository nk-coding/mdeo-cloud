/**
 * Re-exports the model transformation editor plugin for client-side usage.
 * This file is served as an ESM module for the workbench to import.
 */
import { modelTransformationEditorPlugin } from "@mdeo/editor-model-transformation";
import "@mdeo/editor-model-transformation/styles";

export default modelTransformationEditorPlugin;

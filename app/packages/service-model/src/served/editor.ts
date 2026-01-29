/**
 * Re-exports the model editor plugin for client-side usage.
 * This file is served as an ESM module for the workbench to import.
 */
import { modelEditorPlugin } from "@mdeo/editor-model";
import "@mdeo/editor-model/styles";

export default modelEditorPlugin;

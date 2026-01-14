/**
 * Re-exports the metamodel editor plugin for client-side usage.
 * This file is served as an ESM module for the workbench to import.
 */
import { metamodelEditorPlugin } from "@mdeo/editor-metamodel";
import "@mdeo/editor-metamodel/styles";

export default metamodelEditorPlugin;

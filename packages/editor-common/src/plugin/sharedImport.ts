import type { PluginContext } from "./pluginContext.js";

declare global {
    var editorPluginContext: PluginContext | undefined;
}

export function initializeEditorPluginContext(context: PluginContext): void {
    globalThis.editorPluginContext = context;
}

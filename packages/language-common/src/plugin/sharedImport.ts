import type { PluginContext } from "./pluginContext.js";

declare global {
    var pluginContext: PluginContext | undefined;
}

export function initializePluginContext(context: PluginContext): void {
    globalThis.pluginContext = context;
}

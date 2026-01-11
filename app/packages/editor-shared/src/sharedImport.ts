import type { PluginContext } from "@mdeo/editor-common";

export function sharedImport<T extends keyof PluginContext>(key: T): PluginContext[T] {
    if (!globalThis.editorPluginContext) {
        throw new Error("Editor plugin context is not initialized.");
    }
    return globalThis.editorPluginContext[key];
}

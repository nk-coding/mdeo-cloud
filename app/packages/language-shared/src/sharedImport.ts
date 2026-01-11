import type { PluginContext } from "@mdeo/language-common";

export function sharedImport<T extends keyof PluginContext>(key: T): PluginContext[T] {
    if (!globalThis.pluginContext) {
        throw new Error("Plugin context is not initialized.");
    }
    return globalThis.pluginContext[key];
}

import type { Plugin } from "@mdeo/plugin";
import type { WorkbenchPlugin } from "./plugin";
import { deserializeMonarchTokensProvider } from "@mdeo/language-common";

/**
 * Resolves a plugin by loading its editor container configuration
 *
 * @param plugin The plugin to resolve
 * @returns The resolved workbench plugin
 */
export async function resolvePlugin(plugin: Plugin): Promise<WorkbenchPlugin> {
    return {
        ...plugin,
        languagePlugins: await Promise.all(
            plugin.languagePlugins.map(async (languagePlugin) => ({
                ...languagePlugin,
                monarchTokensProvider: deserializeMonarchTokensProvider(languagePlugin.monarchTokensProvider),
                editorPlugin:
                    languagePlugin.editorPlugin != undefined
                        ? {
                              ...languagePlugin.editorPlugin,
                              containerConfiguration: (
                                  await import(/* @vite-ignore */ languagePlugin.editorPlugin.import)
                              ).default
                          }
                        : undefined
            }))
        )
    };
}

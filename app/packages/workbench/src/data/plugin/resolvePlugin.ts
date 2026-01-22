import type { Plugin } from "@mdeo/plugin";
import type { WorkbenchPlugin } from "./plugin";
import { deserializeMonarchTokensProvider } from "@mdeo/language-common";

/**
 * Resolves a plugin by loading its editor container configuration.
 * Deserializes monarch tokens provider and loads graphical editor imports.
 *
 * @param plugin The plugin to resolve
 * @returns The resolved workbench plugin with loaded dependencies
 */
export async function resolvePlugin(plugin: Plugin): Promise<WorkbenchPlugin> {
    return {
        ...plugin,
        languagePlugins: await Promise.all(
            plugin.languagePlugins.map(async (languagePlugin) => ({
                ...languagePlugin,
                textualEditorPlugin:
                    languagePlugin.textualEditorPlugin != undefined
                        ? {
                              ...languagePlugin.textualEditorPlugin,
                              monarchTokensProvider: deserializeMonarchTokensProvider(
                                  languagePlugin.textualEditorPlugin.monarchTokensProvider
                              )
                          }
                        : undefined,
                graphicalEditorPlugin:
                    languagePlugin.graphicalEditorPlugin != undefined
                        ? {
                              ...languagePlugin.graphicalEditorPlugin,
                              containerConfiguration: (
                                  await import(/* @vite-ignore */ languagePlugin.graphicalEditorPlugin.import)
                              ).default
                          }
                        : undefined
            }))
        )
    };
}

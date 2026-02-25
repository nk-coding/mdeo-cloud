import type { ServicePluginDefinition, ServicePlugin } from "./types.js";
import type { FileDependency, DataDependency } from "../handler/types.js";

/**
 * Builds the plugin manifest for the GET / endpoint.
 * Relative URLs are kept relative - the backend will resolve them.
 *
 * @param plugin The service plugin definition
 * @returns The plugin manifest object
 */
export function buildManifest(plugin: ServicePluginDefinition, rawVersion?: string): ServicePlugin {
    const version = pluginVersionSegment(rawVersion);

    return {
        id: plugin.id,
        name: plugin.name,
        description: plugin.description,
        icon: plugin.icon,
        languagePlugins: plugin.languagePlugins.map((languagePlugin) => ({
            ...languagePlugin,
            serverPlugin: {
                ...languagePlugin.serverPlugin,
                import: toStaticAssetPath(languagePlugin.serverPlugin.import, version)
            },
            graphicalEditorPlugin:
                languagePlugin.graphicalEditorPlugin == undefined
                    ? undefined
                    : {
                          ...languagePlugin.graphicalEditorPlugin,
                          import: toStaticAssetPath(languagePlugin.graphicalEditorPlugin.import, version),
                          stylesUrl: toStaticAssetPath(languagePlugin.graphicalEditorPlugin.stylesUrl, version)
                      }
        })),
        contributionPlugins: plugin.contributionPlugins ?? []
    };
}

/**
 * Extracts a clean version segment for static asset paths from the raw version string.
 *
 * @param rawVersion The raw version string, possibly with leading/trailing slashes or whitespace
 * @returns A cleaned version segment without slashes, or undefined if the input is empty/invalid
 */
function pluginVersionSegment(rawVersion?: string): string | undefined {
    const version = rawVersion?.trim();
    if (!version) {
        return undefined;
    }
    return version.replace(/^\/+|\/+$/g, "");
}

/**
 * Converts a given path to a static asset path, prefixing with the versioned static directory if it's a relative path.
 *
 * @param path The original relative path
 * @param version Optional version segment to include in the static path prefix
 * @returns The static asset path
 */
function toStaticAssetPath(path: string, version?: string): string {
    const normalizedPath = path.replace(/^\/+/, "").replace(/^static\//, "");
    const staticPrefix = version ? `static/${version}` : "static";
    return `${staticPrefix}/${normalizedPath}`;
}

/**
 * Merges tracked file dependencies into existing dependencies, avoiding duplicates.
 *
 * @param existing Existing file dependencies from handler result
 * @param tracked Tracked file dependencies from ServerApi
 * @returns Merged array of file dependencies
 */
export function mergeFileDependencies(existing: FileDependency[], tracked: FileDependency[]): FileDependency[] {
    const merged = [...existing];
    for (const trackedDep of tracked) {
        if (!merged.some((dep) => dep.path === trackedDep.path)) {
            merged.push(trackedDep);
        }
    }
    return merged;
}

/**
 * Merges tracked data dependencies into existing dependencies, avoiding duplicates.
 *
 * @param existing Existing data dependencies from handler result
 * @param tracked Tracked data dependencies from ServerApi
 * @returns Merged array of data dependencies
 */
export function mergeDataDependencies(existing: DataDependency[], tracked: DataDependency[]): DataDependency[] {
    const merged = [...existing];
    for (const trackedDep of tracked) {
        if (!merged.some((dep) => dep.path === trackedDep.path && dep.key === trackedDep.key)) {
            merged.push(trackedDep);
        }
    }
    return merged;
}

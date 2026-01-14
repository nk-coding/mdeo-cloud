import type { ServicePluginDefinition, ServicePlugin, FileDependency, DataDependency } from "./types.js";

/**
 * Builds the plugin manifest for the GET / endpoint.
 * Relative URLs are kept relative - the backend will resolve them.
 *
 * @param plugin - The service plugin definition
 * @returns The plugin manifest object
 */
export function buildManifest(plugin: ServicePluginDefinition): ServicePlugin {
    return {
        id: plugin.id,
        name: plugin.name,
        description: plugin.description,
        icon: plugin.icon,
        languagePlugins: [plugin.languagePlugin],
        contributionPlugins: plugin.contributionPlugins ?? []
    };
}

/**
 * Merges tracked file dependencies into existing dependencies, avoiding duplicates.
 *
 * @param existing - Existing file dependencies from handler result
 * @param tracked - Tracked file dependencies from ServerApi
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
 * @param existing - Existing data dependencies from handler result
 * @param tracked - Tracked data dependencies from ServerApi
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
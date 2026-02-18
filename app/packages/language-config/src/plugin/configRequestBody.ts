/**
 * Dependency data passed to each plugin request handler.
 * Keys are plugin short names; values are section-name → data maps from
 * plugins that this plugin declared as sectionDependencies.
 */
export type ConfigPluginDependencyData = Record<string, Record<string, unknown>>;

/**
 * Request body sent to each contribution plugin's request handler.
 */
export interface ConfigPluginRequestBody {
    /**
     * Computed data from dependency plugins, keyed by plugin short name then section name.
     */
    dependencyData: ConfigPluginDependencyData;
    /**
     * Partial config text containing only sections from this plugin, using simple keywords.
     */
    text: string;
    /**
     * The URI string of the originating config file. Used to construct a stable synthetic document URI.
     */
    configFileUri: string;
}

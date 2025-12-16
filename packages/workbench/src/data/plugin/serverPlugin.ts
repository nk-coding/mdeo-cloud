/**
 * Base interface for server-side plugins.
 * This defines the common properties shared by all server plugin types.
 */
export interface ServerPluginBase {
    /**
     * The type of the plugin.
     * - "language": A language plugin that provides language-specific functionality
     * - "contribution": A contribution plugin that extends the server with additional features
     */
    type: "language" | "contribution";
    /**
     * The module path to import for this plugin.
     * This should be a valid module specifier that can be resolved by the server at runtime.
     */
    import: string;
}

/**
 * Server plugin configuration for language-specific functionality.
 * Extends the base plugin with language identification and file extension mapping.
 */
export interface ServerLanguagePlugin extends ServerPluginBase {
    type: "language";
    /**
     * The unique identifier for the language.
     * This should match the language ID used in Monaco and other language services.
     */
    languageId: string;
    /**
     * The file extension associated with this language (e.g., ".ts", ".js", ".py").
     * Used to determine which language plugin should handle a given file.
     */
    extension: string;
}

/**
 * Server plugin configuration for general contributions.
 * These plugins extend the server with additional functionality that is not language-specific.
 */
export interface ServerContributionPlugin extends ServerPluginBase {
    type: "contribution";
}

/**
 * Union type representing all possible server plugin configurations.
 * A server plugin can be either a language plugin or a contribution plugin.
 */
export type ServerPlugin = ServerLanguagePlugin | ServerContributionPlugin;

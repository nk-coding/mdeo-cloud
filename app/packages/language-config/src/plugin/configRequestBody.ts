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

/**
 * Request body sent by service-config when forwarding an execution to
 * the contribution plugin that owns the executable config section.
 */
export interface ConfigExecutionPluginRequestBody {
    /**
     * Execution identifier created by the backend.
     */
    executionId: string;
    /**
     * Project identifier of the running execution.
     */
    project: string;
    /**
     * Path to the originating .config file.
     */
    filePath: string;
    /**
     * Raw content of the originating .config file.
     */
    fileContent: string;
    /**
     * File version of the originating .config file.
     */
    fileVersion: number;
    /**
     * Action/execution payload created by the plugin action handler.
     */
    data: object;
}

/**
 * Metadata persisted on an execution to identify where config execution requests were forwarded.
 */
export interface ConfigExecutionRoutingMetadata {
    /**
     * Language ID of the contribution plugin that handled execution.
     */
    languageId: string;
    /**
     * Executable section name in the config that initiated execution.
     */
    sectionName: string;
    /**
     * Contribution plugin short name.
     */
    pluginShortName: string;
}

/**
 * Base request body for config execution follow-up operations.
 */
export interface ConfigExecutionFollowUpRequestBody {
    executionId: string;
}

/**
 * Request body for config execution file reads.
 */
export interface ConfigExecutionFileRequestBody extends ConfigExecutionFollowUpRequestBody {
    path: string;
}

/**
 * Request-handler key for forwarding config execution to a contribution plugin language.
 */
export const CONFIG_EXECUTION_REQUEST_KEY = "config-execution";

/**
 * Request-handler key for config execution summary forwarding.
 */
export const CONFIG_EXECUTION_GET_SUMMARY_REQUEST_KEY = "config-execution-get-summary";

/**
 * Request-handler key for config execution file tree forwarding.
 */
export const CONFIG_EXECUTION_GET_FILE_TREE_REQUEST_KEY = "config-execution-get-file-tree";

/**
 * Request-handler key for config execution file forwarding.
 */
export const CONFIG_EXECUTION_GET_FILE_REQUEST_KEY = "config-execution-get-file";

/**
 * Request-handler key for config execution cancel forwarding.
 */
export const CONFIG_EXECUTION_CANCEL_REQUEST_KEY = "config-execution-cancel";

/**
 * Request-handler key for config execution delete forwarding.
 */
export const CONFIG_EXECUTION_DELETE_REQUEST_KEY = "config-execution-delete";

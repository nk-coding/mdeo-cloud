import type { RequestHandler, ExecuteResponse } from "@mdeo/service-common";
import type {
    ConfigExecutionPluginRequestBody,
    ConfigExecutionFollowUpRequestBody,
    ConfigExecutionFileRequestBody
} from "@mdeo/service-config-common";
import type { MdeoServices } from "@mdeo/language-config-mdeo";

/**
 * URL of the optimizer-execution backend service.
 * Configurable via OPTIMIZER_EXECUTION_SERVICE_URL env var.
 */
const OPTIMIZER_SERVICE_URL = process.env.OPTIMIZER_EXECUTION_SERVICE_URL ?? "http://localhost:8083";

/**
 * The key used by the config file-data handler (matches CONFIG_DATA_KEY in service-config).
 */
const CONFIG_DATA_KEY = "config";

/** Plugin short names for the two contribution plugins that make up a full config. */
const OPTIMIZATION_PLUGIN_NAME = "optimization";
const MDEO_PLUGIN_NAME = "mdeo";

/**
 * Builds the Authorization header for requests to the optimizer-execution backend.
 */
function buildHeaders(jwt: string): Record<string, string> {
    return {
        "Content-Type": "application/json",
        Authorization: `Bearer ${jwt}`
    };
}

type ConfigFileData = Record<string, Record<string, unknown>>;

/**
 * Execution request handler for MDEO config sections.
 *
 * Fetches the pre-computed config file data and composes the four sections
 * (problem, goal from the optimization plugin; search, solver from the MDEO plugin)
 * into the OptimizationConfig payload expected by the optimizer-execution backend.
 *
 * No field conversion is needed here because the plugin request handlers already
 * produce data whose shape matches the Kotlin backend types directly.
 */
export const mdeoExecutionRequestHandler: RequestHandler<ExecuteResponse, MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionPluginRequestBody>;

    if (!body.filePath) {
        throw new Error("Missing filePath in execution request body");
    }

    const fileDataResult = await context.serverApi.getFileData(body.filePath, CONFIG_DATA_KEY);
    const configData = fileDataResult.data as ConfigFileData | null;

    if (configData == null) {
        throw new Error(`Config file data not available for: ${body.filePath}`);
    }

    const optimizationData = configData[OPTIMIZATION_PLUGIN_NAME];
    const mdeoData = configData[MDEO_PLUGIN_NAME];

    if (!optimizationData?.problem) {
        throw new Error("Missing 'problem' section in config file data");
    }
    if (!optimizationData?.goal) {
        throw new Error("Missing 'goal' section in config file data");
    }
    if (!mdeoData?.search) {
        throw new Error("Missing 'search' section in config file data");
    }
    if (!mdeoData?.solver) {
        throw new Error("Missing 'solver' section in config file data");
    }

    const requestBody = {
        executionId: body.executionId,
        project: body.project,
        filePath: body.filePath,
        data: {
            problem: optimizationData.problem,
            goal: optimizationData.goal,
            search: mdeoData.search,
            solver: mdeoData.solver
        }
    };

    const response = await fetch(`${OPTIMIZER_SERVICE_URL}/api/executions`, {
        method: "POST",
        headers: buildHeaders(context.jwt),
        body: JSON.stringify(requestBody)
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
            `Optimizer execution backend returned error: ${response.status} ${response.statusText}. ${errorText}`
        );
    }

    const result = (await response.json()) as { name?: string };
    if (!result.name) {
        throw new Error("Optimizer backend did not return an execution name");
    }

    return { name: result.name };
};

/**
 * Summary request handler — forwards to optimizer-execution backend.
 */
export const mdeoExecutionGetSummaryRequestHandler: RequestHandler<string, MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionFollowUpRequestBody>;
    const response = await fetch(`${OPTIMIZER_SERVICE_URL}/api/executions/${body.executionId}/summary`, {
        headers: buildHeaders(context.jwt)
    });
    if (!response.ok) {
        throw new Error(`Failed to get optimizer execution summary: ${response.status}`);
    }
    const result = (await response.json()) as { summary?: string };
    return result.summary ?? "";
};

/**
 * File tree request handler — forwards to optimizer-execution backend.
 */
export const mdeoExecutionGetFileTreeRequestHandler: RequestHandler<unknown[], MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionFollowUpRequestBody>;
    const response = await fetch(`${OPTIMIZER_SERVICE_URL}/api/executions/${body.executionId}/file-tree`, {
        headers: buildHeaders(context.jwt)
    });
    if (!response.ok) {
        throw new Error(`Failed to get optimizer execution file tree: ${response.status}`);
    }
    const result = (await response.json()) as { files?: unknown[] };
    return result.files ?? [];
};

/**
 * File read request handler — forwards to optimizer-execution backend.
 */
export const mdeoExecutionGetFileRequestHandler: RequestHandler<string, MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionFileRequestBody>;
    const filePath = body.path ?? "";
    const response = await fetch(`${OPTIMIZER_SERVICE_URL}/api/executions/${body.executionId}/files/${filePath}`, {
        headers: buildHeaders(context.jwt)
    });
    if (!response.ok) {
        throw new Error(`Failed to get optimizer execution file: ${response.status}`);
    }
    return await response.text();
};

/**
 * Cancel request handler — forwards to optimizer-execution backend.
 */
export const mdeoExecutionCancelRequestHandler: RequestHandler<void, MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionFollowUpRequestBody>;
    const response = await fetch(`${OPTIMIZER_SERVICE_URL}/api/executions/${body.executionId}/cancel`, {
        method: "POST",
        headers: buildHeaders(context.jwt)
    });
    if (!response.ok) {
        throw new Error(`Failed to cancel optimizer execution: ${response.status}`);
    }
};

/**
 * Delete request handler — forwards to optimizer-execution backend.
 */
export const mdeoExecutionDeleteRequestHandler: RequestHandler<void, MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionFollowUpRequestBody>;
    const response = await fetch(`${OPTIMIZER_SERVICE_URL}/api/executions/${body.executionId}`, {
        method: "DELETE",
        headers: buildHeaders(context.jwt)
    });
    if (!response.ok) {
        throw new Error(`Failed to delete optimizer execution: ${response.status}`);
    }
};

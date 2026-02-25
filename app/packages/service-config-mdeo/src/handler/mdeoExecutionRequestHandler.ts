import type { RequestHandler, ExecuteResponse } from "@mdeo/service-common";
import type { ConfigExecutionPluginRequestBody } from "@mdeo/language-config";
import type { MdeoServices } from "@mdeo/language-config-mdeo";

/**
 * Dummy execution request handler for MDEO executable config sections.
 *
 * For now, this intentionally fails so the execution is marked as failed in the backend.
 */
export const mdeoExecutionRequestHandler: RequestHandler<ExecuteResponse, MdeoServices> = async (context) => {
    const body = context.body as Partial<ConfigExecutionPluginRequestBody>;
    const filePath = typeof body.filePath === "string" ? body.filePath : "<unknown>";

    throw new Error(`MDEO execution is not implemented yet for '${filePath}'. Marking execution as failed.`);
};

/**
 * Summary request handler placeholder for MDEO config executions.
 */
export const mdeoExecutionGetSummaryRequestHandler: RequestHandler<string, MdeoServices> = async () => {
    throw new Error("MDEO execution summary is not implemented yet.");
};

/**
 * File tree request handler placeholder for MDEO config executions.
 */
export const mdeoExecutionGetFileTreeRequestHandler: RequestHandler<unknown[], MdeoServices> = async () => {
    throw new Error("MDEO execution file tree is not implemented yet.");
};

/**
 * File read request handler placeholder for MDEO config executions.
 */
export const mdeoExecutionGetFileRequestHandler: RequestHandler<string, MdeoServices> = async () => {
    throw new Error("MDEO execution file access is not implemented yet.");
};

/**
 * Cancel request handler placeholder for MDEO config executions.
 */
export const mdeoExecutionCancelRequestHandler: RequestHandler<void, MdeoServices> = async () => {
    throw new Error("MDEO execution cancellation is not implemented yet.");
};

/**
 * Delete request handler placeholder for MDEO config executions.
 */
export const mdeoExecutionDeleteRequestHandler: RequestHandler<void, MdeoServices> = async () => {
    throw new Error("MDEO execution deletion is not implemented yet.");
};

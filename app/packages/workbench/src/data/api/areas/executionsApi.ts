import { ApiResult, CommonErrorCode, type ExecutionError } from "../apiResult";
import type { Execution, ExecutionWithTree, CreateExecutionRequest } from "../../execution/execution";
import type { BackendApiCore } from "../backendApi";

/**
 * API for execution management operations.
 * Provides methods for creating, listing, and managing executions
 * within projects.
 */
export class ExecutionsApi {
    /**
     * Creates a new ExecutionsApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Lists all executions for a project
     *
     * @param projectId The ID of the project
     * @returns A promise resolving to an array of executions
     */
    async list(projectId: string): Promise<ApiResult<Execution[], ExecutionError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/executions`, {
            method: "GET"
        });
    }

    /**
     * Gets an execution with its file tree
     *
     * @param projectId The ID of the project
     * @param executionId The ID of the execution
     * @returns A promise resolving to the execution with file tree
     */
    async get(projectId: string, executionId: string): Promise<ApiResult<ExecutionWithTree, ExecutionError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/executions/${executionId}`, {
            method: "GET"
        });
    }

    /**
     * Gets the summary document for an execution
     *
     * @param projectId The ID of the project
     * @param executionId The ID of the execution
     * @returns A promise resolving to the summary content as bytes
     */
    async getSummary(projectId: string, executionId: string): Promise<ApiResult<Uint8Array, ExecutionError>> {
        const result = await this.core.fetchApiResult<{ summary: string }>(
            `${this.core.baseUrl}/projects/${projectId}/executions/${executionId}/summary`,
            { method: "GET" }
        );
        if (result.success) {
            return ApiResult.success(new TextEncoder().encode(result.value.summary));
        }
        return result;
    }

    /**
     * Gets a result file for an execution
     *
     * @param projectId The ID of the project
     * @param executionId The ID of the execution
     * @param path The path to the file within the execution results
     * @returns A promise resolving to the file contents as bytes
     */
    async getFile(
        projectId: string,
        executionId: string,
        path: string
    ): Promise<ApiResult<Uint8Array, ExecutionError>> {
        const encodedPath = path.split("/").map(encodeURIComponent).join("/");
        try {
            const response = await fetch(
                `${this.core.baseUrl}/projects/${projectId}/executions/${executionId}/files/${encodedPath}`,
                {
                    method: "GET",
                    credentials: "include"
                }
            );

            if (!response.ok) {
                return this.core.parseErrorResponse(response);
            }

            const content = await response.text();
            const encodedContent = new TextEncoder().encode(content);
            return ApiResult.success(encodedContent);
        } catch (error) {
            return ApiResult.commonFailure(CommonErrorCode.Unavailable, String(error));
        }
    }

    /**
     * Cancels a running execution
     *
     * @param projectId The ID of the project
     * @param executionId The ID of the execution to cancel
     * @returns A promise resolving to success or an error
     */
    async cancel(projectId: string, executionId: string): Promise<ApiResult<void, ExecutionError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/executions/${executionId}/cancel`, {
            method: "POST"
        });
    }

    /**
     * Deletes an execution (implies cancel if still running)
     *
     * @param projectId The ID of the project
     * @param executionId The ID of the execution to delete
     * @returns A promise resolving to success or an error
     */
    async delete(projectId: string, executionId: string): Promise<ApiResult<void, ExecutionError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/executions/${executionId}`, {
            method: "DELETE"
        });
    }

    /**
     * Deletes all executions for a project
     *
     * @param projectId The ID of the project
     * @returns A promise resolving to success or an error
     */
    async deleteAll(projectId: string): Promise<ApiResult<void, ExecutionError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/executions`, {
            method: "DELETE"
        });
    }

    /**
     * Creates a new execution
     *
     * @param projectId The ID of the project
     * @param request The execution creation request containing file path and data
     * @returns A promise resolving to the created execution
     */
    async create(projectId: string, request: CreateExecutionRequest): Promise<ApiResult<Execution, ExecutionError>> {
        return this.core.fetchApiResult(`${this.core.baseUrl}/projects/${projectId}/executions`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });
    }
}

import type { ApiResult, FileDataError } from "../apiResult";
import type { BackendApiCore } from "../backendApi";

/**
 * API for file data computation operations.
 * Provides methods for retrieving computed data about files,
 * such as AST representations.
 */
export class FileDataApi {
    /**
     * Creates a new FileDataApi instance
     *
     * @param core The core backend API providing HTTP utilities
     */
    constructor(private readonly core: BackendApiCore) {}

    /**
     * Gets computed file data for a file
     *
     * @param projectId The ID of the project
     * @param path The path to the file within the project
     * @param key The type of data to compute (e.g., "ast")
     * @returns A promise resolving to the computed data as a string
     */
    async get(projectId: string, path: string, key: string): Promise<ApiResult<string, FileDataError>> {
        const params = new URLSearchParams({ path });
        return this.core.fetchApiResult(
            `${this.core.baseUrl}/projects/${projectId}/file-data/${encodeURIComponent(key)}?${params}`,
            { method: "GET" }
        );
    }
}

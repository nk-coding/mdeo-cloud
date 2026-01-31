import type {
    ExecutionHandler,
    ExecutionContext,
    CanHandleResult,
    ExecuteResponse,
    FileEntry
} from "@mdeo/service-common";

/**
 * Expected structure of execution data for model transformation executions
 */
interface ModelTransformationExecutionData {
    /**
     * Path to the transformation file
     */
    filePath: string;

    /**
     * Path to the input model file
     */
    modelPath: string;
}

/**
 * Execution handler for model transformation files.
 *
 * This handler processes execution requests for model transformation files by forwarding
 * them to the model-transformation-execution backend service. It acts as a proxy to the
 * specialized backend service and supports file tree operations for output models.
 */
export class ModelTransformationExecutionHandler implements ExecutionHandler<ExecuteResponse> {
    /**
     * URL of the model-transformation-execution backend service
     */
    private readonly backendUrl: string;

    /**
     * Creates a new model transformation execution handler.
     *
     * @param backendUrl URL of the model-transformation-execution backend service
     */
    constructor(backendUrl: string) {
        this.backendUrl = backendUrl;
    }

    /**
     * Determines if this handler can process the execution request.
     *
     * Checks:
     * - The execution data is a valid object
     * - The modelPath field is present and is a string
     * - The file has a .mt extension (model transformation file)
     *
     * @param context The execution context
     * @returns Promise resolving to whether this handler can process the request
     */
    async canHandle(context: ExecutionContext): Promise<CanHandleResult> {
        const { data, filePath } = context;

        if (typeof data !== "object" || data === null) {
            return {
                canHandle: false,
                reason: "Execution data must be an object"
            };
        }

        const typedData = data as Partial<ModelTransformationExecutionData>;

        if (!typedData.modelPath || typeof typedData.modelPath !== "string") {
            return {
                canHandle: false,
                reason: "Execution data must contain a 'modelPath' string property"
            };
        }

        if (!filePath.endsWith(".mt")) {
            return {
                canHandle: false,
                reason: "File must have .mt extension"
            };
        }

        return {
            canHandle: true
        };
    }

    /**
     * Executes the model transformation by forwarding the request to the backend service.
     *
     * @param context The execution context
     * @returns Promise resolving to the execution response
     * @throws Error if the backend request fails or returns an error
     */
    async execute(context: ExecutionContext): Promise<ExecuteResponse> {
        const { executionId, project, filePath, data, jwt } = context;

        const requestBody = {
            executionId,
            project,
            filePath,
            data
        };

        const response = await this.fetchWithErrorHandling(`${this.backendUrl}/api/executions`, {
            method: "POST",
            headers: this.buildHeaders(jwt),
            body: JSON.stringify(requestBody)
        });

        const result = await response.json();

        if (!result.name) {
            throw new Error("Backend did not return an execution name");
        }

        return {
            name: result.name
        };
    }

    /**
     * Gets a markdown summary of the execution results.
     *
     * Forwards the request to the model-transformation-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise resolving to a markdown-formatted summary
     */
    async getSummary(executionId: string, jwt: string): Promise<string> {
        const response = await this.fetchWithErrorHandling(`${this.backendUrl}/api/executions/${executionId}/summary`, {
            method: "GET",
            headers: this.buildHeaders(jwt)
        });

        const result = await response.json();
        return result.summary || "";
    }

    /**
     * Gets the file tree of execution results.
     *
     * Forwards the request to the model-transformation-execution backend service.
     * Model transformations can produce output files, so this returns the actual file tree.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise resolving to the list of files and directories
     */
    async getFileTree(executionId: string, jwt: string): Promise<FileEntry[]> {
        const response = await this.fetchWithErrorHandling(
            `${this.backendUrl}/api/executions/${executionId}/file-tree`,
            {
                method: "GET",
                headers: this.buildHeaders(jwt)
            }
        );

        const result = await response.json();
        return result.files || [];
    }

    /**
     * Reads a specific file from the execution results.
     *
     * Forwards the request to the model-transformation-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param path Path to the file to read
     * @param jwt JWT token for authentication
     * @returns Promise resolving to the file contents as a Buffer
     */
    async getFile(executionId: string, path: string, jwt: string): Promise<Buffer> {
        const response = await this.fetchWithErrorHandling(
            `${this.backendUrl}/api/executions/${executionId}/files/${path}`,
            {
                method: "GET",
                headers: this.buildHeaders(jwt)
            }
        );

        const text = await response.text();
        return Buffer.from(text, "utf-8");
    }

    /**
     * Cancels a running execution.
     *
     * Forwards the request to the model-transformation-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise that resolves when the execution is cancelled
     */
    async cancel(executionId: string, jwt: string): Promise<void> {
        await this.fetchWithErrorHandling(`${this.backendUrl}/api/executions/${executionId}/cancel`, {
            method: "POST",
            headers: this.buildHeaders(jwt)
        });
    }

    /**
     * Deletes an execution and its results.
     *
     * Forwards the request to the model-transformation-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise that resolves when the execution is deleted
     */
    async delete(executionId: string, jwt: string): Promise<void> {
        await this.fetchWithErrorHandling(`${this.backendUrl}/api/executions/${executionId}`, {
            method: "DELETE",
            headers: {
                Authorization: `Bearer ${jwt}`
            }
        });
    }

    /**
     * Builds the standard headers for API requests.
     *
     * @param jwt JWT token for authentication
     * @returns Headers object with Content-Type and Authorization
     */
    private buildHeaders(jwt: string): Record<string, string> {
        return {
            "Content-Type": "application/json",
            Authorization: `Bearer ${jwt}`
        };
    }

    /**
     * Performs a fetch request with standard error handling.
     *
     * @param url URL to fetch
     * @param options Fetch options
     * @returns Promise resolving to the Response
     * @throws Error if the request fails or returns a non-OK status
     */
    private async fetchWithErrorHandling(url: string, options: RequestInit): Promise<Response> {
        try {
            const response = await fetch(url, options);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(
                    `Model transformation execution backend returned error: ` +
                        `${response.status} ${response.statusText}. ${errorText}`
                );
            }

            return response;
        } catch (error) {
            if (error instanceof TypeError && error.message.includes("fetch")) {
                throw new Error(
                    `Failed to connect to model-transformation-execution backend at ${this.backendUrl}. ` +
                        `Please ensure the service is running and accessible. Original error: ${(error as Error).message}`
                );
            }
            throw error;
        }
    }
}

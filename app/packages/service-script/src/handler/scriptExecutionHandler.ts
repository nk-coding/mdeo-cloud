import type {
    ExecutionHandler,
    ExecutionContext,
    CanHandleResult,
    ExecuteResponse,
    FileEntry
} from "@mdeo/service-common";

/**
 * Expected structure of execution data for script executions
 */
interface ScriptExecutionData {
    /**
     * Name of the method to execute
     */
    methodName: string;
}

/**
 * Execution handler for script files.
 *
 * This handler processes execution requests for script files by forwarding
 * them to the script-execution backend service. It does not execute scripts
 * locally but acts as a proxy to the specialized backend service.
 */
export class ScriptExecutionHandler implements ExecutionHandler<ExecuteResponse> {
    /**
     * URL of the script-execution backend service
     */
    private readonly backendUrl: string;

    /**
     * Creates a new script execution handler.
     *
     * @param backendUrl URL of the script-execution backend service
     */
    constructor(backendUrl: string) {
        this.backendUrl = backendUrl;
    }

    /**
     * Determines if this handler can process the execution request.
     *
     * Checks:
     * - The execution data is a valid object
     * - The methodName field is present and is a string
     * - The file has a .fn extension (script file)
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

        const typedData = data as Partial<ScriptExecutionData>;

        if (!typedData.methodName || typeof typedData.methodName !== "string") {
            return {
                canHandle: false,
                reason: "Execution data must contain a 'methodName' string property"
            };
        }

        if (!filePath.endsWith(".fn")) {
            return {
                canHandle: false,
                reason: "File must have .fn extension"
            };
        }

        return {
            canHandle: true
        };
    }

    /**
     * Executes the script by forwarding the request to the backend service.
     *
     * This method:
     * 1. Extracts execution parameters from the context
     * 2. Constructs a request to the script-execution backend
     * 3. Submits the execution request via HTTP POST
     * 4. Returns the execution response
     *
     * @param context The execution context
     * @returns Promise resolving to the execution response
     * @throws Error if the backend request fails or returns an error
     */
    async execute(context: ExecutionContext): Promise<ExecuteResponse> {
        const { executionId, project, filePath, data, jwt } = context;
        const typedData = data as ScriptExecutionData;

        const requestBody = {
            executionId,
            project,
            filePath,
            data: typedData.methodName
        };

        try {
            const response = await fetch(`${this.backendUrl}/api/executions`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${jwt}`
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(
                    `Script execution backend returned error: ${response.status} ${response.statusText}. ${errorText}`
                );
            }

            const result = await response.json();

            if (!result.name) {
                throw new Error("Backend did not return an execution name");
            }

            return {
                name: result.name
            };
        } catch (error) {
            if (error instanceof TypeError && error.message.includes("fetch")) {
                throw new Error(
                    `Failed to connect to script-execution backend at ${this.backendUrl}. ` +
                        `Please ensure the service is running and accessible. Original error: ${error.message}`,
                    { cause: error }
                );
            }
            throw error;
        }
    }

    /**
     * Gets a markdown summary of the execution results.
     *
     * Forwards the request to the script-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise resolving to a markdown-formatted summary
     */
    async getSummary(executionId: string, jwt: string): Promise<string> {
        try {
            const response = await fetch(`${this.backendUrl}/api/executions/${executionId}/summary`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${jwt}`
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(
                    `Script execution backend returned error: ${response.status} ${response.statusText}. ${errorText}`
                );
            }

            const result = await response.json();
            return result.summary || "";
        } catch (error) {
            if (error instanceof TypeError && error.message.includes("fetch")) {
                throw new Error(
                    `Failed to connect to script-execution backend at ${this.backendUrl}. ` +
                        `Please ensure the service is running and accessible. Original error: ${error.message}`,
                    { cause: error }
                );
            }
            throw error;
        }
    }

    /**
     * Gets the file tree of execution results.
     *
     * Script executions never produce any files, so this always returns an empty array.
     *
     * @returns Promise resolving to an empty list
     */
    async getFileTree(): Promise<FileEntry[]> {
        return [];
    }

    /**
     * Reads a specific file from the execution results.
     *
     * Script executions never produce any files, so this always throws an error.
     *
     * @returns Promise that rejects with an error
     */
    async getFile(): Promise<Buffer> {
        throw new Error(`File not found: Script executions do not produce files`);
    }

    /**
     * Cancels a running execution.
     *
     * Forwards the request to the script-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise that resolves when the execution is cancelled
     */
    async cancel(executionId: string, jwt: string): Promise<void> {
        try {
            const response = await fetch(`${this.backendUrl}/api/executions/${executionId}/cancel`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${jwt}`
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(
                    `Script execution backend returned error: ${response.status} ${response.statusText}. ${errorText}`
                );
            }
        } catch (error) {
            if (error instanceof TypeError && error.message.includes("fetch")) {
                throw new Error(
                    `Failed to connect to script-execution backend at ${this.backendUrl}. ` +
                        `Please ensure the service is running and accessible. Original error: ${error.message}`,
                    { cause: error }
                );
            }
            throw error;
        }
    }

    /**
     * Deletes an execution and its results.
     *
     * Forwards the request to the script-execution backend service.
     *
     * @param executionId Unique identifier for the execution
     * @param jwt JWT token for authentication
     * @returns Promise that resolves when the execution is deleted
     */
    async delete(executionId: string, jwt: string): Promise<void> {
        try {
            const response = await fetch(`${this.backendUrl}/api/executions/${executionId}`, {
                method: "DELETE",
                headers: {
                    Authorization: `Bearer ${jwt}`
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(
                    `Script execution backend returned error: ${response.status} ${response.statusText}. ${errorText}`
                );
            }
        } catch (error) {
            if (error instanceof TypeError && error.message.includes("fetch")) {
                throw new Error(
                    `Failed to connect to script-execution backend at ${this.backendUrl}. ` +
                        `Please ensure the service is running and accessible. Original error: ${error.message}`,
                    { cause: error }
                );
            }
            throw error;
        }
    }
}

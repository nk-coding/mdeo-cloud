import type { ServerApi, DirectoryEntry } from "./types.js";

/**
 * Implementation of the ServerApi that communicates with the backend via HTTP.
 * The JWT token is set per-request to ensure proper authorization.
 */
export class HttpServerApi implements ServerApi {
    private readonly backendUrl: string;
    private jwt: string | null = null;

    constructor(backendUrl: string) {
        this.backendUrl = backendUrl.endsWith("/") ? backendUrl.slice(0, -1) : backendUrl;
    }

    /**
     * Sets the JWT token for subsequent API calls.
     * This should be called before handling each request.
     *
     * @param jwt - The JWT token to use for authorization
     */
    setJwt(jwt: string): void {
        this.jwt = jwt;
    }

    /**
     * Clears the JWT token after request handling.
     * Should be called after the request is complete to prevent token leakage.
     */
    clearJwt(): void {
        this.jwt = null;
    }

    private getAuthHeaders(): HeadersInit {
        if (!this.jwt) {
            throw new Error("JWT not set. Call setJwt() before making API requests.");
        }
        return {
            Authorization: `Bearer ${this.jwt}`,
            "Content-Type": "application/json"
        };
    }

    /**
     * Reads a file from the backend.
     *
     * @param path - The path of the file to read
     * @returns Promise resolving to the file content and version
     * @throws Error if the file cannot be read or JWT is not set
     */
    async readFile(path: string): Promise<{ content: string; version: number }> {
        const encodedPath = encodeURIComponent(path);
        const response = await fetch(`${this.backendUrl}/api/files/${encodedPath}`, {
            method: "GET",
            headers: this.getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to read file ${path}: ${response.status} ${response.statusText}`);
        }

        const result = await response.json();
        return {
            content: result.content,
            version: result.version
        };
    }

    /**
     * Gets computed file data from the backend.
     *
     * @param path - The path of the file
     * @param key - The data key (e.g., "ast", "diagram")
     * @returns Promise resolving to the computed file data as a string
     * @throws Error if the data cannot be retrieved or JWT is not set
     */
    async getFileData(path: string, key: string): Promise<string> {
        const encodedPath = encodeURIComponent(path);
        const encodedKey = encodeURIComponent(key);
        const response = await fetch(`${this.backendUrl}/api/file-data/${encodedPath}/${encodedKey}`, {
            method: "GET",
            headers: this.getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to get file data ${path}:${key}: ${response.status} ${response.statusText}`);
        }

        const result = await response.json();
        return result.data;
    }

    /**
     * Lists files in a directory.
     *
     * @param path - The directory path to list
     * @returns Promise resolving to array of directory entries
     * @throws Error if the directory cannot be listed or JWT is not set
     */
    async listDirectory(path: string): Promise<DirectoryEntry[]> {
        const encodedPath = encodeURIComponent(path);
        const response = await fetch(`${this.backendUrl}/api/files/${encodedPath}/list`, {
            method: "GET",
            headers: this.getAuthHeaders()
        });

        if (!response.ok) {
            throw new Error(`Failed to list directory ${path}: ${response.status} ${response.statusText}`);
        }

        const result = await response.json();
        return result.entries.map((entry: { name: string; isFile: boolean; isDirectory: boolean }) => ({
            name: entry.name,
            isFile: entry.isFile,
            isDirectory: entry.isDirectory
        }));
    }

    /**
     * Checks if a file exists.
     *
     * @param path - The path to check
     * @returns Promise resolving to true if the file exists, false otherwise
     */
    async fileExists(path: string): Promise<boolean> {
        const encodedPath = encodeURIComponent(path);
        const response = await fetch(`${this.backendUrl}/api/files/${encodedPath}/exists`, {
            method: "GET",
            headers: this.getAuthHeaders()
        });

        if (!response.ok) {
            return false;
        }

        const result = await response.json();
        return result.exists;
    }
}

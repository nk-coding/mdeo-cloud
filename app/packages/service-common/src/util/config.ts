/**
 * Parses configuration from environment variables with defaults.
 *
 * @returns Configuration object with port, host, backendApiUrl, and maxLangiumInstances
 */
export function parseServiceConfigFromEnv(): {
    port: number;
    host: string;
    backendApiUrl: string;
    maxLangiumInstances: number;
} {
    const port = parseInt(process.env.PORT ?? "3000", 10);
    const host = process.env.HOST ?? "0.0.0.0";
    const backendApiUrl = process.env.BACKEND_API_URL ?? "http://localhost:8080/api";
    const maxLangiumInstances = parseInt(process.env.MAX_LANGIUM_INSTANCES ?? "5", 10);

    return {
        port,
        host,
        backendApiUrl,
        maxLangiumInstances
    };
}

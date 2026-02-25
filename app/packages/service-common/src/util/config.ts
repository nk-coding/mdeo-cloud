/**
 * Parses configuration from environment variables with defaults.
 *
 * @returns Configuration object with port, host, backendApiUrl, jwtIssuer, and maxLangiumInstances
 */
export function parseServiceConfigFromEnv(): {
    port: number;
    host: string;
    backendApiUrl: string;
    jwtIssuer: string;
    maxLangiumInstances: number;
    version?: string;
} {
    const port = parseInt(process.env.PORT ?? "3000", 10);
    const host = process.env.HOST ?? "0.0.0.0";
    const backendApiUrl = process.env.BACKEND_API_URL ?? "http://localhost:8080/api";
    const jwtIssuer = process.env.JWT_ISSUER ?? "mdeo-platform";
    const maxLangiumInstances = parseInt(process.env.MAX_LANGIUM_INSTANCES ?? "5", 10);
    const version = process.env.SERVICE_VERSION?.trim();

    return {
        port,
        host,
        backendApiUrl,
        jwtIssuer,
        maxLangiumInstances,
        version: version && version.length > 0 ? version : undefined
    };
}

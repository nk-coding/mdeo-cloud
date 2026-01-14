import Fastify, { type FastifyInstance, type FastifyRequest, type FastifyReply } from "fastify";
import cors from "@fastify/cors";
import fastifyJwt from "@fastify/jwt";
import fastifyStatic from "@fastify/static";
import { resolve } from "path";
import type {
    ServiceConfig,
    FileDataComputeRequest,
    FileDataComputeResponse,
    ServicePlugin,
    ServicePluginDefinition
} from "./types.js";
import { HttpServerApi } from "./serverApi.js";
import { LangiumInstancePool } from "./langiumPool.js";
import type { ServerContributionPlugin } from "@mdeo/plugin";

/**
 * Builds the plugin manifest for the GET / endpoint.
 * Relative URLs are kept relative - the backend will resolve them.
 *
 * @param plugin - The service plugin definition
 * @returns The plugin manifest object
 */
function buildManifest(plugin: ServicePluginDefinition): ServicePlugin {
    return {
        id: plugin.id,
        name: plugin.name,
        description: plugin.description,
        icon: plugin.icon,
        languagePlugins: [plugin.languagePlugin],
        contributionPlugins: plugin.contributionPlugins ?? []
    };
}

/**
 * Creates and configures a Fastify-based language service.
 *
 * @param config - The service configuration including plugin definition and handlers
 * @returns Promise resolving to a configured Fastify instance ready to be started
 */
export async function createLanguageService(config: ServiceConfig): Promise<FastifyInstance> {
    const fastify = Fastify({
        logger: true
    });

    // Register CORS
    await fastify.register(cors, {
        origin: true
    });

    // Register JWT plugin for token verification
    await fastify.register(fastifyJwt, {
        // The secret doesn't matter for verification - we trust tokens from the backend
        secret: "service-internal-secret",
        decode: { complete: true }
    });

    // Register static file serving from the dist directory
    // This allows serving the built JS and CSS files
    if (config.serveStatic !== false) {
        const staticPath = config.staticPath ?? resolve(process.cwd(), "static");
        await fastify.register(fastifyStatic, {
            root: staticPath,
            prefix: "/static",
            decorateReply: false
        });
    }

    const serverApi = new HttpServerApi(config.backendApiUrl);

    const instancePool = new LangiumInstancePool({
        maxInstances: config.maxLangiumInstances ?? 5,
        languagePlugin: config.languagePlugin,
        languageId: config.plugin.languagePlugin.id,
        extension: config.plugin.languagePlugin.extension,
        serverApi
    });

    const manifest = buildManifest(config.plugin);

    // Store references for route handlers
    fastify.decorate("instancePool", instancePool);
    fastify.decorate("serverApi", serverApi);
    fastify.decorate("handlers", config.handlers);
    fastify.decorate("pluginDef", manifest);

    // GET / - Plugin manifest endpoint
    fastify.get("/", async (request: FastifyRequest, reply: FastifyReply) => {
        return reply.send(manifest);
    });

    // POST /data/:key - File data computation endpoint
    fastify.post<{
        Params: { key: string };
        Body: FileDataComputeRequest;
    }>("/data/:key", async (request, reply) => {
        const { key } = request.params;
        const { path, version, content, contributionPlugins } = request.body;

        // Extract JWT from Authorization header
        const authHeader = request.headers.authorization;
        if (!authHeader || !authHeader.startsWith("Bearer ")) {
            return reply.status(401).send({ error: "Missing or invalid Authorization header" });
        }
        const jwt = authHeader.substring(7);

        // Get handler for this key
        const handler = config.handlers[key];
        if (!handler) {
            return reply.status(404).send({ error: `No handler registered for key: ${key}` });
        }

        // Acquire Langium instance
        const serverContributionPlugins = (contributionPlugins ?? []) as unknown as ServerContributionPlugin[];
        const instance = await instancePool.acquire(serverContributionPlugins, jwt);

        try {
            // Execute handler
            const result = await handler({
                path,
                version,
                content,
                services: instance.services,
                serverApi
            });

            // Build response
            const response: FileDataComputeResponse = {
                data: typeof result.data === "string" ? result.data : JSON.stringify(result.data),
                fileDependencies: result.fileDependencies ?? [],
                dataDependencies: result.dataDependencies ?? [],
                additionalFileData: result.additionalFileData ?? []
            };

            return reply.send(response);
        } finally {
            // Release instance
            instancePool.release(instance);
        }
    });

    return fastify;
}

/**
 * Starts a language service with the given configuration.
 * The service will listen on the configured host and port.
 *
 * @param config - The service configuration
 * @returns Promise that resolves when the service has started
 */
export async function startLanguageService(config: ServiceConfig): Promise<void> {
    const fastify = await createLanguageService(config);

    try {
        const host = config.host ?? "0.0.0.0";
        await fastify.listen({ port: config.port, host });
        console.log(`Language service started on ${host}:${config.port}`);
    } catch (err) {
        fastify.log.error(err);
        process.exit(1);
    }
}

// Type augmentation for Fastify
declare module "fastify" {
    interface FastifyInstance {
        instancePool: LangiumInstancePool;
        serverApi: HttpServerApi;
        handlers: Record<string, unknown>;
        pluginDef: ServicePlugin;
    }
}

import Fastify, { type FastifyInstance, type FastifyRequest, type FastifyReply } from "fastify";
import cors from "@fastify/cors";
import fastifyStatic from "@fastify/static";
import { resolve } from "path";
import type { ServiceConfig, FileDataComputeRequest, FileDataComputeResponse } from "./types.js";
import { LangiumInstancePool } from "../langium/langiumPool.js";
import type { ServerContributionPlugin } from "@mdeo/plugin";
import { URI } from "vscode-uri";
import { buildManifest } from "./util.js";

/**
 * Creates and configures a Fastify-based language service.
 *
 * @param config The service configuration including plugin definition and handlers
 * @returns Promise resolving to a configured Fastify instance ready to be started
 */
export async function createLanguageService<T>(config: ServiceConfig<T>): Promise<FastifyInstance> {
    const fastify = Fastify({
        logger: {
            transport: {
                target: "pino-pretty"
            },
            level: "warn"
        }
    });

    await fastify.register(cors, {
        origin: true
    });

    if (config.serveStatic !== false) {
        const staticPath = config.staticPath ?? resolve(process.cwd(), "static");
        await fastify.register(fastifyStatic, {
            root: staticPath,
            prefix: "/static",
            decorateReply: false
        });
    }

    const instancePool = new LangiumInstancePool<T>({
        maxInstances: config.maxLangiumInstances ?? 5,
        languagePluginProvider: config.languagePluginProvider,
        languageId: config.plugin.languagePlugin.id,
        extension: config.plugin.languagePlugin.extension,
        backendUrl: config.backendApiUrl
    });

    const manifest = buildManifest(config.plugin);

    fastify.get("/", async (request: FastifyRequest, reply: FastifyReply) => {
        return reply.send(manifest);
    });

    fastify.post<{
        Params: { key: string };
        Body: FileDataComputeRequest;
    }>("/data/:key", async (request, reply) => {
        const { key } = request.params;
        const { path, project, version, content, contributionPlugins } = request.body;

        const authHeader = request.headers.authorization;
        if (!authHeader || !authHeader.startsWith("Bearer ")) {
            return reply.status(401).send({ error: "Missing or invalid Authorization header" });
        }
        const jwt = authHeader.substring(7);

        const handler = config.handlers[key];
        if (handler == undefined) {
            return reply.status(404).send({ error: `No handler registered for key: ${key}` });
        }

        const serverContributionPlugins = (contributionPlugins ?? []) as unknown as ServerContributionPlugin[];
        const instance = await instancePool.acquire(serverContributionPlugins, jwt, project);
        const uri = URI.file(path);
        instance.services.shared.workspace.LangiumDocuments.createDocument(uri, content);

        try {
            const result = await handler({
                uri,
                version,
                instance,
                services: instance.services,
                serverApi: instance.services.shared.ServerApi
            });

            const response: FileDataComputeResponse = {
                ...result,
                additionalFileData: result.additionalFileData ?? []
            };

            return reply.send(response);
        } finally {
            instancePool.release(instance);
        }
    });

    return fastify;
}

/**
 * Starts a language service with the given configuration.
 * The service will listen on the configured host and port.
 *
 * @param config The service configuration
 * @returns Promise that resolves when the service has started
 */
export async function startLanguageService<T>(config: ServiceConfig<T>): Promise<void> {
    const fastify = await createLanguageService(config);

    try {
        const host = config.host ?? "0.0.0.0";
        await fastify.listen({ port: config.port, host });
        // eslint-disable-next-line no-console
        console.log(`Language service started on ${host}:${config.port}`);
    } catch (err) {
        fastify.log.error(err);
        process.exit(1);
    }
}

import Fastify, { type FastifyInstance, type FastifyRequest, type FastifyReply } from "fastify";
import cors from "@fastify/cors";
import fastifyStatic from "@fastify/static";
import { resolve } from "path";
import type { ServiceConfig, FileDataComputeRequest, FileDataComputeResponse } from "./types.js";
import { LangiumInstancePool } from "../langium/langiumPool.js";
import type { ServerContributionPlugin } from "@mdeo/plugin";
import { URI } from "vscode-uri";
import { buildManifest } from "./util.js";
import type { FileInfo } from "../handler/types.js";
import type { ExecutionContext } from "../execution/types.js";
import { JwtAuthMiddleware } from "../auth/jwtAuth.js";

/**
 * Extracts JWT token from the Authorization header of a request.
 * Assumes the request has already passed authentication middleware.
 *
 * @param request - The Fastify request object
 * @returns The JWT token string
 * @throws Error if Authorization header is missing or malformed
 */
function extractJwtFromRequest(request: FastifyRequest): string {
    const authHeader = request.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
        throw new Error("Missing or invalid Authorization header");
    }
    return authHeader.substring(7);
}

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

    const jwtAuth = new JwtAuthMiddleware(config.backendApiUrl);

    const manifest = buildManifest(config.plugin);

    fastify.get("/", async (request: FastifyRequest, reply: FastifyReply) => {
        return reply.send(manifest);
    });

    fastify.post<{
        Params: { key: string };
        Body: FileDataComputeRequest;
    }>(
        "/data/:key",
        {
            preHandler: jwtAuth.authenticate.bind(jwtAuth)
        },
        async (request, reply) => {
            const { key } = request.params;
            const { project, source, contributionPlugins } = request.body;

            if (!JwtAuthMiddleware.hasScope(request, "file-data:read")) {
                return reply.status(403).send({ error: "Insufficient permissions: file-data:read scope required" });
            }

            const jwt = extractJwtFromRequest(request);

            const handler = config.handlers[key];
            if (handler == undefined) {
                return reply.status(404).send({ error: `No handler registered for key: ${key}` });
            }

            const serverContributionPlugins = (contributionPlugins ?? []) as unknown as ServerContributionPlugin[];
            const instance = await instancePool.acquire(serverContributionPlugins, jwt, project);

            let fileInfo: FileInfo | undefined = undefined;
            if (source != undefined) {
                const uri = URI.parse(source.path);
                fileInfo = {
                    uri,
                    version: source.version
                };
                instance.services.shared.workspace.LangiumDocuments.createDocument(uri, source.content);
            }

            try {
                const result = await handler({
                    fileInfo,
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
        }
    );

    if (config.executionHandlers && config.executionHandlers.length > 0) {
        /**
         * Creates a new execution
         * POST /executions
         * Body: { executionId, project, filePath, data }
         */
        fastify.post<{
            Body: { executionId: string; project: string; filePath: string; data: object };
        }>(
            "/executions",
            {
                preHandler: jwtAuth.authenticate.bind(jwtAuth)
            },
            async (request, reply) => {
                const { executionId, project, filePath, data } = request.body;

                if (!JwtAuthMiddleware.hasScope(request, "execution:write")) {
                    return reply
                        .status(403)
                        .send({ error: "Insufficient permissions: execution:write scope required" });
                }

                const jwt = extractJwtFromRequest(request);

                if (!config.executionHandlers || config.executionHandlers.length === 0) {
                    return reply.status(503).send({ error: "Execution service not available" });
                }

                const executionContext: ExecutionContext = {
                    executionId,
                    project,
                    filePath,
                    data: data ?? {},
                    jwt
                };

                let selectedHandler = null;
                for (const handler of config.executionHandlers) {
                    const canHandleResult = await handler.canHandle(executionContext);
                    if (canHandleResult.canHandle) {
                        selectedHandler = handler;
                        break;
                    }
                }

                if (!selectedHandler) {
                    return reply.status(400).send({
                        error: "No handler available for this execution request"
                    });
                }

                try {
                    const result = await selectedHandler.execute(executionContext);
                    return reply.send(result);
                } catch (error) {
                    fastify.log.error(error);
                    return reply.status(500).send({
                        error: error instanceof Error ? error.message : "Execution failed"
                    });
                }
            }
        );

        /**
         * Gets the summary for an execution
         * GET /executions/:executionId/summary
         */
        fastify.get<{
            Params: { executionId: string };
        }>(
            "/executions/:executionId/summary",
            {
                preHandler: jwtAuth.authenticate.bind(jwtAuth)
            },
            async (request, reply) => {
                const { executionId } = request.params;

                if (!JwtAuthMiddleware.hasScope(request, "plugin:execution:read")) {
                    return reply
                        .status(403)
                        .send({ error: "Insufficient permissions: plugin:execution:read scope required" });
                }

                const jwt = extractJwtFromRequest(request);

                if (!config.executionHandlers || config.executionHandlers.length === 0) {
                    return reply.status(503).send({ error: "Execution service not available" });
                }

                const handler = config.executionHandlers[0];

                try {
                    const summary = await handler.getSummary(executionId, jwt);
                    return reply.send({ summary });
                } catch (error) {
                    fastify.log.error(error);
                    return reply.status(500).send({
                        error: error instanceof Error ? error.message : "Failed to get summary"
                    });
                }
            }
        );

        /**
         * Gets the file tree for an execution
         * GET /executions/:executionId/files
         */
        fastify.get<{
            Params: { executionId: string };
        }>(
            "/executions/:executionId/files",
            {
                preHandler: jwtAuth.authenticate.bind(jwtAuth)
            },
            async (request, reply) => {
                const { executionId } = request.params;

                if (!JwtAuthMiddleware.hasScope(request, "plugin:execution:read")) {
                    return reply
                        .status(403)
                        .send({ error: "Insufficient permissions: plugin:execution:read scope required" });
                }
                const jwt = extractJwtFromRequest(request);

                if (!config.executionHandlers || config.executionHandlers.length === 0) {
                    return reply.status(503).send({ error: "Execution service not available" });
                }

                const handler = config.executionHandlers[0];

                try {
                    const files = await handler.getFileTree(executionId, jwt);
                    return reply.send({ files });
                } catch (error) {
                    fastify.log.error(error);
                    return reply.status(500).send({
                        error: error instanceof Error ? error.message : "Failed to get file tree"
                    });
                }
            }
        );

        /**
         * Gets a specific file from an execution
         * GET /executions/:executionId/files/:path
         */
        fastify.get<{
            Params: { executionId: string; "*": string };
        }>(
            "/executions/:executionId/files/*",
            {
                preHandler: jwtAuth.authenticate.bind(jwtAuth)
            },
            async (request, reply) => {
                const { executionId } = request.params;
                const path = request.params["*"];

                if (!JwtAuthMiddleware.hasScope(request, "plugin:execution:read")) {
                    return reply
                        .status(403)
                        .send({ error: "Insufficient permissions: plugin:execution:read scope required" });
                }

                const jwt = extractJwtFromRequest(request);

                if (!config.executionHandlers || config.executionHandlers.length === 0) {
                    return reply.status(503).send({ error: "Execution service not available" });
                }

                const handler = config.executionHandlers[0];

                try {
                    const fileContent = await handler.getFile(executionId, path, jwt);
                    return reply.type("application/octet-stream").send(fileContent);
                } catch (error) {
                    fastify.log.error(error);
                    return reply.status(500).send({
                        error: error instanceof Error ? error.message : "Failed to get file"
                    });
                }
            }
        );

        /**
         * Cancels an execution
         * POST /executions/:executionId/cancel
         */
        fastify.post<{
            Params: { executionId: string };
        }>(
            "/executions/:executionId/cancel",
            {
                preHandler: jwtAuth.authenticate.bind(jwtAuth)
            },
            async (request, reply) => {
                const { executionId } = request.params;

                if (!JwtAuthMiddleware.hasScope(request, "plugin:execution:cancel")) {
                    return reply
                        .status(403)
                        .send({ error: "Insufficient permissions: plugin:execution:cancel scope required" });
                }

                const jwt = extractJwtFromRequest(request);

                if (!config.executionHandlers || config.executionHandlers.length === 0) {
                    return reply.status(503).send({ error: "Execution service not available" });
                }

                const handler = config.executionHandlers[0];

                try {
                    await handler.cancel(executionId, jwt);
                    return reply.status(204).send();
                } catch (error) {
                    fastify.log.error(error);
                    return reply.status(500).send({
                        error: error instanceof Error ? error.message : "Failed to cancel execution"
                    });
                }
            }
        );

        /**
         * Deletes an execution
         * DELETE /executions/:executionId
         */
        fastify.delete<{
            Params: { executionId: string };
        }>(
            "/executions/:executionId",
            {
                preHandler: jwtAuth.authenticate.bind(jwtAuth)
            },
            async (request, reply) => {
                const { executionId } = request.params;

                if (!JwtAuthMiddleware.hasScope(request, "plugin:execution:delete")) {
                    return reply
                        .status(403)
                        .send({ error: "Insufficient permissions: plugin:execution:write scope required" });
                }

                const jwt = extractJwtFromRequest(request);

                if (!config.executionHandlers || config.executionHandlers.length === 0) {
                    return reply.status(503).send({ error: "Execution service not available" });
                }

                const handler = config.executionHandlers[0];

                try {
                    await handler.delete(executionId, jwt);
                    return reply.status(204).send();
                } catch (error) {
                    fastify.log.error(error);
                    return reply.status(500).send({
                        error: error instanceof Error ? error.message : "Failed to delete execution"
                    });
                }
            }
        );
    }

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

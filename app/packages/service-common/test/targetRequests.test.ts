import { after, before, describe, it } from "node:test";
import assert from "node:assert/strict";
import type { FastifyInstance, FastifyRequest } from "fastify";
import { JwtAuthMiddleware } from "../dist/auth/jwtAuth.js";
import { createLanguageService } from "../dist/service/service.js";

/**
 * Claims a real token would carry, by token value; anything else fails authentication.
 */
const tokens: Record<string, { scope: string[]; projectId: string }> = {
    good: { scope: ["plugin:request:send"], projectId: "p" },
    otherProject: { scope: ["plugin:request:send"], projectId: "q" },
    unscoped: { scope: ["session:open"], projectId: "p" }
};

const realAuthenticate = JwtAuthMiddleware.prototype.authenticate;

/**
 * A service with no languages at all, serving one contribution's request key. Nothing here needs a
 * Langium instance, which is the point: a contribution has none.
 */
async function contributionService(): Promise<FastifyInstance> {
    return createLanguageService({
        port: 0,
        backendApiUrl: "http://backend/api",
        jwtIssuer: "mdeo-platform",
        serveStatic: false,
        plugin: {
            id: "echo-service",
            name: "Echo",
            description: "Echoes",
            icon: [],
            languagePlugins: [],
            contributionPlugins: []
        },
        languages: [],
        requests: {
            "contrib:echoes": {
                echo: async ({ body, target, key, projectId }) => ({ said: body, target, key, projectId })
            }
        }
    });
}

async function post(
    service: FastifyInstance,
    path: string,
    token: string | undefined,
    payload: object | null = { project: "p", body: { hello: "world" } }
) {
    return service.inject({
        method: "POST",
        url: path,
        headers: token == undefined ? {} : { authorization: `Bearer ${token}` },
        ...(payload == null ? {} : { payload })
    });
}

describe("one-shot requests addressed at a target", () => {
    let service: FastifyInstance;

    before(async () => {
        JwtAuthMiddleware.prototype.authenticate = async function (request: FastifyRequest, reply: any) {
            const header = request.headers.authorization;
            const claims = header?.startsWith("Bearer ") ? tokens[header.substring(7)] : undefined;
            if (claims == undefined) {
                return reply.status(401).send({ error: { code: "Unauthenticated", message: "no" } });
            }
            (request as any).jwtClaims = claims;
        };
        service = await contributionService();
    });

    after(async () => {
        JwtAuthMiddleware.prototype.authenticate = realAuthenticate;
        await service.close();
    });

    it("reaches a contribution's handler, which is told what was addressed", async () => {
        const response = await post(service, "/request/contrib/echoes/echo", "good");

        assert.equal(response.statusCode, 200);
        assert.deepEqual(JSON.parse(response.body).data, {
            said: { hello: "world" },
            target: { kind: "contrib", id: "echoes" },
            key: "echo",
            projectId: "p"
        });
    });

    it("keeps the two id spaces apart, so the same id as a language reaches nothing", async () => {
        const response = await post(service, "/request/lang/echoes/echo", "good");

        assert.equal(response.statusCode, 404);
        assert.match(JSON.parse(response.body).error.message, /echoes/);
    });

    it("refuses an unusable address, an unserved key, and a token without the scope", async () => {
        assert.equal((await post(service, "/request/nonsense/echoes/echo", "good")).statusCode, 404);
        assert.equal((await post(service, "/request/contrib/echoes/nope", "good")).statusCode, 404);
        assert.equal((await post(service, "/request/contrib/echoes/echo", "unscoped")).statusCode, 403);
        assert.equal((await post(service, "/request/contrib/echoes/echo", undefined)).statusCode, 401);
    });

    it("refuses a body for another project than the token's, and a body naming none", async () => {
        assert.equal((await post(service, "/request/contrib/echoes/echo", "otherProject")).statusCode, 403);
        assert.equal((await post(service, "/request/contrib/echoes/echo", "good", { body: {} })).statusCode, 400);
        assert.equal((await post(service, "/request/contrib/echoes/echo", "good", null)).statusCode, 400);
    });
});

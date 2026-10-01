import { describe, it, before, after } from "node:test";
import assert from "node:assert/strict";
import { createServer, type Server } from "node:http";
import type { AddressInfo } from "node:net";
import { exportJWK, generateKeyPair, SignJWT, type JWK } from "jose";
import { JwtAuthMiddleware } from "../dist/auth/jwtAuth.js";

const ISSUER = "mdeo-platform";
const KEY_ID = "mdeo-key-1";

type KeyPair = Awaited<ReturnType<typeof generateKeyPair>>;

async function sign(keys: KeyPair, issuer = ISSUER): Promise<string> {
    return new SignJWT({ projectId: "p" })
        .setProtectedHeader({ alg: "RS256", kid: KEY_ID })
        .setIssuer(issuer)
        .setExpirationTime("5m")
        .sign(keys.privateKey);
}

/**
 * A backend that restarts with a new signing key under the same key id, as the dev backend does
 * when it is not configured with a key.
 */
describe("JWT verification", () => {
    let server: Server;
    let backendUrl: string;
    let published: JWK;
    let fetches = 0;
    let first: KeyPair;

    before(async () => {
        first = await generateKeyPair("RS256");
        published = { ...(await exportJWK(first.publicKey)), kid: KEY_ID, alg: "RS256", use: "sig" };
        server = createServer((_request, response) => {
            fetches++;
            response.setHeader("Content-Type", "application/json");
            response.end(JSON.stringify({ keys: [published] }));
        });
        await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
        backendUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    });

    after(() => server.close());

    it("accepts tokens signed with a key the backend published after a restart", async () => {
        const auth = new JwtAuthMiddleware(backendUrl, ISSUER, 0);
        assert.equal((await auth.verifyToken(await sign(first))).projectId, "p");

        const restarted = await generateKeyPair("RS256");
        published = { ...(await exportJWK(restarted.publicKey)), kid: KEY_ID, alg: "RS256", use: "sig" };
        const fetchesBefore = fetches;
        assert.equal((await auth.verifyToken(await sign(restarted))).projectId, "p");
        assert.equal(fetches, fetchesBefore + 1);
    });

    it("rejects a forged token without fetching the keys again while cooling down", async () => {
        const auth = new JwtAuthMiddleware(backendUrl, ISSUER, 60_000);
        const current = await generateKeyPair("RS256");
        published = { ...(await exportJWK(current.publicKey)), kid: KEY_ID, alg: "RS256", use: "sig" };
        await auth.verifyToken(await sign(current));

        const fetchesBefore = fetches;
        const forger = await generateKeyPair("RS256");
        await assert.rejects(auth.verifyToken(await sign(forger)), /signature verification failed/);
        assert.equal(fetches, fetchesBefore);
    });

    it("rejects a token of another issuer", async () => {
        const auth = new JwtAuthMiddleware(backendUrl, ISSUER);
        const current = await generateKeyPair("RS256");
        published = { ...(await exportJWK(current.publicKey)), kid: KEY_ID, alg: "RS256", use: "sig" };
        await assert.rejects(auth.verifyToken(await sign(current, "someone-else")), /"iss" claim/);
    });
});

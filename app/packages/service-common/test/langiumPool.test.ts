import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { LangiumInstancePool, LangiumPoolExhaustedError } from "../dist/langium/langiumPool.js";

/**
 * A pool that can never create an instance, so every acquisition has to wait. What is tested is
 * how waiting ends, which does not need a language.
 */
function fullPool(acquireTimeoutMs: number, maxSessionInstances = 2): LangiumInstancePool<unknown> {
    return new LangiumInstancePool<unknown>({
        maxInstances: 0,
        maxSessionInstances,
        acquireTimeoutMs,
        languagePluginProvider: {
            create: () => {
                throw new Error("no instance may be created in this test");
            }
        }
    } as unknown as ConstructorParameters<typeof LangiumInstancePool<unknown>>[0]);
}

describe("Langium instance pool", () => {
    it("gives up waiting after the acquire timeout", async () => {
        const pool = fullPool(20);
        await assert.rejects(pool.acquire([], "jwt", "project"), LangiumPoolExhaustedError);
    });

    it("stops waiting as soon as the caller's signal aborts", async () => {
        const pool = fullPool(60_000);
        const controller = new AbortController();
        const started = Date.now();
        const waiting = pool.acquire([], "jwt", "project", undefined, controller.signal);
        setTimeout(() => controller.abort(new Error("caller left")), 10);
        await assert.rejects(waiting, /caller left/);
        assert.ok(Date.now() - started < 5_000);
    });

    it("does not wait at all for a signal that already aborted", async () => {
        const pool = fullPool(60_000);
        await assert.rejects(pool.acquire([], "jwt", "project", undefined, AbortSignal.abort()));
    });

    it("refuses a session once the session budget is used up", () => {
        const pool = fullPool(20, 0);
        assert.throws(() => pool.acquireForSession([], "jwt", "project"), LangiumPoolExhaustedError);
    });

    it("fails a waiter whose instance cannot be built instead of leaving it waiting", async () => {
        const pool = new LangiumInstancePool<unknown>({
            maxInstances: 1,
            maxSessionInstances: 1,
            acquireTimeoutMs: 60_000,
            languagePluginProvider: { create: () => ({}) }
        } as unknown as ConstructorParameters<typeof LangiumInstancePool<unknown>>[0]);
        // The first instance is a stand-in; building the second one fails.
        let built = 0;
        (pool as unknown as { createInstance: (plugins: unknown, key: string) => unknown }).createInstance = (
            _plugins,
            key
        ) => {
            if (built++ > 0) {
                throw new Error("contributions do not build");
            }
            const instance = {
                id: "first",
                contributionPluginKey: key,
                busy: false,
                lastUsed: 0,
                configure: () => {
                    instance.busy = true;
                },
                reset: () => {
                    instance.busy = false;
                }
            };
            return instance;
        };

        const first = await pool.acquire([], "jwt", "project", "one");
        const waiting = pool.acquire([], "jwt", "project", "two");
        pool.release(first);
        await assert.rejects(waiting, /contributions do not build/);
    });
});

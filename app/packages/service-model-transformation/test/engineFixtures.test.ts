import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { TestWorkspace } from "./testWorkspace.ts";
import { layeredProject, SCENARIOS, COLLECTION_SCENARIOS, TRAP_SCENARIOS } from "./fixtures.ts";

const { typedAstHandler } = await import("../dist/handler/typedAstHandler.js");
const { metamodelAstDataHandler } = await import("../../service-metamodel/dist/handler/metamodelAstDataHandler.js");

/**
 * The engine tests of callable transformations (platform/model-transformation,
 * CallStatementExecutionTest) run the typed ASTs this converter produces for the fixtures.
 * This test checks that the committed JSON files are what the converter produces today; run it
 * with UPDATE_CALL_FIXTURES=1 to rewrite them.
 */
const FIXTURE_DIR = new URL("../../../../platform/model-transformation/src/test/resources/calls/", import.meta.url)
    .pathname;

const TRANSFORMATIONS = [
    "/optimize/move-with-stores.mt",
    "/optimize/generic/move-component.mt",
    "/optimize/sync/follow-reader.mt",
    ...Object.keys(SCENARIOS).filter((path) => path.includes("/call-")),
    ...Object.keys(COLLECTION_SCENARIOS).filter((path) => path.includes("/call-")),
    ...Object.keys(TRAP_SCENARIOS).filter((path) => path.includes("/call-")),
    "/optimize/scenarios/move-placement.mt"
];

const METAMODELS = ["/metamodel/shop.mm", "/metamodel/base.mm"];

function fixtureName(path: string): string {
    return path.replace(/^\//, "").replaceAll("/", "__") + ".json";
}

function check(name: string, data: unknown): void {
    const file = FIXTURE_DIR + name;
    const text = JSON.stringify(data, null, 1) + "\n";
    if (process.env.UPDATE_CALL_FIXTURES === "1") {
        mkdirSync(FIXTURE_DIR, { recursive: true });
        writeFileSync(file, text);
        return;
    }
    assert.ok(existsSync(file), `${file} is missing; run with UPDATE_CALL_FIXTURES=1`);
    assert.equal(readFileSync(file, "utf8"), text, `${name} is out of date; run with UPDATE_CALL_FIXTURES=1`);
}

describe("typed AST fixtures of the engine tests", () => {
    const workspace = new TestWorkspace(layeredProject({ ...SCENARIOS, ...COLLECTION_SCENARIOS, ...TRAP_SCENARIOS }));

    for (const path of TRANSFORMATIONS) {
        it(`are up to date for ${path}`, async () => {
            const run = await workspace.run(typedAstHandler, path);
            assert.deepEqual(run.errors, []);
            assert.notEqual(run.data, null);
            check(fixtureName(path), run.data);
        });
    }

    for (const path of METAMODELS) {
        it(`are up to date for ${path}`, async () => {
            const run = await workspace.run(metamodelAstDataHandler, path, ".mm");
            assert.deepEqual(run.errors, []);
            check(fixtureName(path), run.data);
        });
    }
});

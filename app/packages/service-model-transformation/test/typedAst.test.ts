import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TestWorkspace } from "./testWorkspace.ts";
import { layeredProject } from "./fixtures.ts";

const { typedAstHandler } = await import("../dist/handler/typedAstHandler.js");

describe("typed AST of callable transformations", () => {
    it("bundles every callee under its absolute path, without nested bundles", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { data, errors } = await workspace.run(typedAstHandler, "/optimize/move-with-stores.mt");
        assert.deepEqual(errors, []);
        assert.deepEqual(Object.keys(data.callees).sort(), [
            "/optimize/generic/move-component.mt",
            "/optimize/sync/follow-reader.mt"
        ]);
        for (const callee of Object.values<any>(data.callees)) {
            assert.equal(callee.callees, undefined);
        }
        assert.equal(data.metamodelPath, "/metamodel/shop.mm");
        assert.equal(data.callees["/optimize/generic/move-component.mt"].metamodelPath, "/metamodel/base.mm");
        assert.equal(data.signature, undefined);
    });

    it("describes calls by callee path, arguments and outputs", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { data } = await workspace.run(typedAstHandler, "/optimize/move-with-stores.mt");
        const [move, follow] = data.statements;
        assert.equal(move.kind, "call");
        assert.equal(move.callee, "/optimize/generic/move-component.mt");
        assert.equal(move.name, "moveComponent");
        assert.deepEqual(
            move.arguments.map((a: any) => [a.name, a.value.kind, a.value.value]),
            [["maxCandidates", "intLiteral", "4"]]
        );
        assert.deepEqual(
            move.outputs.map((o: any) => [o.parameter, o.name]),
            [
                ["moved", "m"],
                ["source", "a"],
                ["target", "b"],
                ["choices", "n"]
            ]
        );
        assert.deepEqual(
            follow.arguments.map((a: any) => [a.name, a.value.kind, a.value.name, a.value.scope]),
            [
                ["moved", "identifier", "m", 1],
                ["target", "identifier", "b", 1]
            ]
        );
        // the outputs are bound in the caller's scope (level 1) and typed by the callee parameter
        const memberAccess = data.statements[2].pattern.elements.find((e: any) => e.kind === "whereClause");
        assert.ok(memberAccess);
    });

    it("emits the signature with parameter kinds, types and defaults", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { data } = await workspace.run(typedAstHandler, "/optimize/generic/move-component.mt");
        assert.equal(data.signature.name, "moveComponent");
        const parameters = data.signature.parameters.map((p: any) => [
            p.name,
            p.direction,
            p.isElement,
            data.types[p.type].type,
            p.defaultValue?.value
        ]);
        assert.deepEqual(parameters, [
            ["maxCandidates", "in", false, "int", "0"],
            ["moved", "out", true, "Component", undefined],
            ["source", "out", true, "Container", undefined],
            ["target", "out", true, "Container", undefined],
            ["choices", "out", false, "int", undefined]
        ]);
        assert.equal(data.types[data.signature.parameters[1].type].package, "class/metamodel/base.mm");

        // an element input is referenced like an object of an earlier match: without a class
        const follow = await workspace.run(typedAstHandler, "/optimize/sync/follow-reader.mt");
        const elements = JSON.parse(JSON.stringify(follow.data.statements[0].pattern.elements));
        assert.deepEqual(elements[0], { kind: "objectInstance", objectInstance: { name: "moved", properties: [] } });
        const readerLink = elements.find((e: any) => e.kind === "link" && e.link.source.objectName === "feed");
        assert.deepEqual(readerLink.link.target, { objectName: "moved" });
    });

    it("bundles the callees of callees", async () => {
        const workspace = new TestWorkspace(
            layeredProject({
                "/optimize/outer.mt": `using "../metamodel/shop.mm"\nimport { middle } from "./middle.mt"\n\ncall middle()\n`,
                "/optimize/middle.mt": `using "../metamodel/shop.mm"\nimport { moveComponent } from "./generic/move-component.mt"\n\ntransformation middle()\n\ncall moveComponent()\n`
            })
        );
        const { data, errors } = await workspace.run(typedAstHandler, "/optimize/outer.mt");
        assert.deepEqual(errors, []);
        assert.deepEqual(Object.keys(data.callees).sort(), [
            "/optimize/generic/move-component.mt",
            "/optimize/middle.mt"
        ]);
    });

    it("has no typed AST when a callee has errors", async () => {
        const workspace = new TestWorkspace(
            layeredProject({
                "/optimize/generic/move-component.mt": `using "../../metamodel/base.mm"\n\ntransformation moveComponent()\n\nmatch {\n    x: NoSuchClass { }\n}\n`
            })
        );
        const { data } = await workspace.run(typedAstHandler, "/optimize/move-with-stores.mt");
        assert.equal(data, null);
    });
});

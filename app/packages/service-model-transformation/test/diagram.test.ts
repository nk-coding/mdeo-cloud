import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TestWorkspace } from "./testWorkspace.ts";
import { TextDocument } from "vscode-languageserver-textdocument";
import { layeredProject, SCENARIOS, COLLECTION_SCENARIOS } from "./fixtures.ts";

const {
    ModelTransformationGModelFactory,
    ModelTransformationModelIdProvider,
    ModelTransformationMetadataManager,
    ModelTransformationControlFlowConverter,
    InsertControlFlowStatementOperationHandler,
    ModelTransformationDeleteElementOperationHandler,
    ModelTransformationApplyLabelEditOperationHandler
} = await import("@mdeo/language-model-transformation");
const { DefaultModelIdRegistry, GModelIndex } = await import("@mdeo/language-shared");
const { ModelTransformationElementType } = await import("@mdeo/protocol-model-transformation");

/**
 * Builds the graphical model of a transformation the way the diagram server does, with an
 * empty layout.
 */
async function graphOf(path: string, extra: Record<string, string> = {}) {
    const workspace = new TestWorkspace(layeredProject({ ...SCENARIOS, ...COLLECTION_SCENARIOS, ...extra }));
    const { document, instance } = await workspace.build(path);
    const root = document.parseResult.value;
    const services = instance.services;

    const idProvider: any = new ModelTransformationModelIdProvider();
    idProvider.reflection = services.shared.AstReflection;
    const factory: any = new ModelTransformationGModelFactory();
    factory.modelState = {
        languageServices: services,
        sourceModel: root,
        getValidatedMetadata: async () => ({ nodes: {}, edges: {} })
    };
    factory.modelIdProvider = idProvider;
    const graph = await factory.createModelInternal(root, new DefaultModelIdRegistry(root, idProvider));

    const metadataManager: any = new ModelTransformationMetadataManager();
    metadataManager.modelIdProvider = idProvider;
    metadataManager.reflection = services.shared.AstReflection;
    return { graph, root, services, idProvider, metadataManager, document };
}

/**
 * Sets up an operation handler of the diagram server on a transformation: its model state and
 * the index from diagram elements to AST nodes.
 */
async function handlerOn<T>(handler: T, path: string, extra: Record<string, string> = {}) {
    const built = await graphOf(path, extra);
    const index: any = new GModelIndex();
    index.indexRoot(built.graph);
    index.indexSourceModelRoot(built.root, new DefaultModelIdRegistry(built.root, built.idProvider));
    const modelState = {
        sourceModel: built.root,
        languageServices: built.services,
        index,
        metadata: { nodes: {}, edges: {} },
        sourceUri: built.document.uri.toString()
    };
    Object.assign(handler as any, {
        modelState,
        index,
        reflection: built.services.shared.AstReflection,
        idProvider: built.idProvider
    });
    return { ...built, handler, index };
}

/**
 * Applies a workspace edit to the text of a document.
 */
function applied(document: any, edit: any): string {
    const edits = Object.values<any[]>(edit?.changes ?? {}).flat();
    return TextDocument.applyEdits(document.textDocument as any, edits);
}

/**
 * Lists the elements of a graphical model, depth first.
 */
function elements(element: any): any[] {
    return [element, ...(element.children ?? []).flatMap((child: any) => elements(child))];
}

describe("diagram of callable transformations", () => {
    it("shows a call as a node on the control flow", async () => {
        const { graph } = await graphOf("/optimize/move-with-stores.mt");
        const all = elements(graph);
        const calls = all.filter((e) => e.type === ModelTransformationElementType.NODE_CALL);
        assert.deepEqual(
            calls.map((c) => c.text),
            [
                "call moveComponent(maxCandidates = 4) -> (moved: m, source: a, target: b, choices: n)",
                "call followReader(moved = m, target = b)"
            ]
        );
        const edges = all.filter((e) => e.type === ModelTransformationElementType.EDGE_CONTROL_FLOW);
        const from = (id: string) => edges.filter((e) => e.sourceId === id).map((e) => e.targetId);
        assert.deepEqual(from("start"), [calls[0].id]);
        assert.deepEqual(from(calls[0].id), [calls[1].id]);
        // the match after the calls references the outputs m and b as bound objects
        const match = all.find((e) => e.type === ModelTransformationElementType.NODE_MATCH);
        assert.deepEqual(from(calls[1].id), [match.id]);
        const instances = all.filter((e) => e.type === ModelTransformationElementType.NODE_PATTERN_INSTANCE);
        assert.deepEqual(instances.map((i) => i.name).sort(), ["b", "m"]);
    });

    it("shows a callee whose patterns use its parameters", async () => {
        for (const path of [
            "/optimize/sync/follow-reader.mt",
            "/optimize/generic/move-component.mt",
            ...Object.keys(SCENARIOS)
        ]) {
            const { graph } = await graphOf(path);
            assert.ok(elements(graph).length > 2, path);
        }
        const { graph } = await graphOf("/optimize/sync/follow-reader.mt");
        const all = elements(graph);
        const instances = all.filter((e) => e.type === ModelTransformationElementType.NODE_PATTERN_INSTANCE);
        assert.deepEqual(instances.map((i) => i.name).sort(), ["feed", "moved", "old", "store", "target"]);
        const links = all.filter((e) => e.type === ModelTransformationElementType.EDGE_PATTERN_LINK);
        assert.ok(links.length >= 4);
    });

    it("lays out calls with the other nodes", async () => {
        const { root, metadataManager } = await graphOf("/optimize/move-with-stores.mt");
        const metadata = metadataManager.extractGraphMetadata(root);
        const callNodes = Object.values<any>(metadata.nodes).filter(
            (n) => n.type === ModelTransformationElementType.NODE_CALL
        );
        assert.equal(callNodes.length, 2);
    });

    it("keeps the control flow of a converter without calls unchanged", async () => {
        const { root, services, idProvider } = await graphOf("/optimize/generic/move-component.mt");
        const cfg = new ModelTransformationControlFlowConverter(
            root,
            new DefaultModelIdRegistry(root, idProvider),
            services.shared.AstReflection
        ).convert();
        assert.deepEqual(
            cfg.nodes.map((n: any) => n.kind),
            ["start", "match", "end"]
        );
    });

    it("draws if call with success and failure branches", async () => {
        const { graph } = await graphOf("/optimize/scenarios/call-if.mt");
        const all = elements(graph);
        const calls = all.filter((e) => e.type === ModelTransformationElementType.NODE_CALL);
        assert.deepEqual(
            calls.map((c) => [c.text, c.variant]),
            [
                ['call noPlacement(wanted = "nobody")', "call"],
                ["call moveComponent() -> (moved: m, target: t)", "call"]
            ]
        );
        const edges = all.filter((e) => e.type === ModelTransformationElementType.EDGE_CONTROL_FLOW);
        const labels = (id: string) =>
            edges
                .filter((e) => e.sourceId === id)
                .map((e) => elements(e).find((c) => c.type === ModelTransformationElementType.LABEL_CONTROL_FLOW)?.text)
                .sort();
        assert.deepEqual(labels(calls[0].id), ["failure", "success"]);
        assert.deepEqual(labels(calls[1].id), ["failure", "success"]);
        // the text of a call is an editable label
        const text = elements(calls[0]).find((e) => e.type === ModelTransformationElementType.LABEL_CALL);
        assert.equal(text.text, 'call noPlacement(wanted = "nobody")');
        assert.notEqual(text.readonly, true);
    });

    it("draws loops over collections and assignments as nodes", async () => {
        const { graph } = await graphOf("/optimize/scenarios/rename-all.mt");
        const all = elements(graph);
        const nodes = all.filter((e) => e.type === ModelTransformationElementType.NODE_CALL);
        assert.deepEqual(
            nodes.map((n) => [n.text, n.variant]),
            [
                ["renamed = 0", "assignment"],
                ["for c in components", "loop"],
                ["renamed = renamed + 1", "assignment"]
            ]
        );
        const edges = all.filter((e) => e.type === ModelTransformationElementType.EDGE_CONTROL_FLOW);
        const loop = nodes[1];
        const each = edges.find((e) => e.sourceId === loop.id && elements(e).some((c) => c.text === "each"));
        assert.ok(each, "the body hangs on an each edge of the loop");
        assert.ok(
            edges.some((e) => e.sourceId === nodes[2].id && e.targetId === loop.id),
            "the body loops back"
        );
        for (const path of Object.keys(COLLECTION_SCENARIOS)) {
            const { graph: other } = await graphOf(path);
            assert.ok(elements(other).length > 2, path);
        }
    });

    it("offers calls of the imported transformations and inserts them", async () => {
        const { handler, graph, document } = await handlerOn(
            new (InsertControlFlowStatementOperationHandler as any)(),
            "/optimize/move-with-stores.mt"
        );
        const edge = elements(graph).find(
            (e) => e.type === ModelTransformationElementType.EDGE_CONTROL_FLOW && e.sourceId === "start"
        );
        const items = (handler as any).getContextItems(edge, {})[0].children;
        const labels = items.map((item: any) => item.label);
        for (const label of ["Call moveComponent", "If Call moveComponent", "Call followReader"]) {
            assert.ok(labels.includes(label), labels.join(", "));
        }
        for (const kind of ["call", "if-call"]) {
            const item = items.find((i: any) => i.action.statementKind === kind && i.action.callee === "followReader");
            const command = await (handler as any).createCommand(item.action);
            const text = applied(document, command.workspaceEdit);
            const expected = kind === "call" ? "call followReader()" : "if call followReader() then {";
            assert.ok(text.includes(expected), text);
            // the statement goes after the imports, before the first call
            assert.ok(text.indexOf(expected) > text.indexOf("import { followReader }"), text);
            assert.ok(text.indexOf(expected) < text.indexOf("call moveComponent"), text);
        }
    });

    it("deletes a call, and the whole if call with its call", async () => {
        const { handler, graph, document } = await handlerOn(
            new (ModelTransformationDeleteElementOperationHandler as any)(),
            "/optimize/scenarios/call-if.mt"
        );
        const calls = elements(graph).filter((e) => e.type === ModelTransformationElementType.NODE_CALL);
        const result = await (handler as any).executeDelete({ elementIds: [calls[0].id] });
        const text = applied(document, result.workspaceEdit);
        assert.ok(!text.includes("noPlacement(wanted"), text);
        assert.ok(!text.includes("else ran"), "the blocks of the if call went with it");
        assert.ok(text.includes("if call moveComponent()"), text);

        const plain = await handlerOn(
            new (ModelTransformationDeleteElementOperationHandler as any)(),
            "/optimize/move-with-stores.mt"
        );
        const plainCalls = elements(plain.graph).filter((e) => e.type === ModelTransformationElementType.NODE_CALL);
        const plainResult = await (plain.handler as any).executeDelete({ elementIds: [plainCalls[1].id] });
        const plainText = applied(plain.document, plainResult.workspaceEdit);
        assert.ok(!plainText.includes("call followReader("), plainText);
        assert.ok(plainText.includes("call moveComponent("), plainText);
    });

    it("edits the arguments and outputs of a call in its text", async () => {
        const { handler, root, document } = await handlerOn(
            new (ModelTransformationApplyLabelEditOperationHandler as any)(),
            "/optimize/scenarios/call-if.mt"
        );
        const call = root.statements[1].ifBlock.call;
        const edit = await (handler as any).createLabelEdit(call, {
            text: "call moveComponent(maxCandidates = 2) -> (moved: m, target: t)",
            labelId: "x__text"
        });
        const text = applied(document, edit);
        assert.ok(text.includes("if call moveComponent(maxCandidates = 2) -> (moved: m, target: t) then {"), text);
        assert.equal(
            await (handler as any).createLabelEdit(call, { text: "moveComponent()", labelId: "x" }),
            undefined
        );
    });
});

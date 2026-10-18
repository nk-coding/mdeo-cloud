import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TestWorkspace } from "./testWorkspace.ts";
import { layeredProject, SCENARIOS } from "./fixtures.ts";

const {
    ModelTransformationGModelFactory,
    ModelTransformationModelIdProvider,
    ModelTransformationMetadataManager,
    ModelTransformationControlFlowConverter
} = await import("@mdeo/language-model-transformation");
const { DefaultModelIdRegistry } = await import("@mdeo/language-shared");
const { ModelTransformationElementType } = await import("@mdeo/protocol-model-transformation");

/**
 * Builds the graphical model of a transformation the way the diagram server does, with an
 * empty layout.
 */
async function graphOf(path: string) {
    const workspace = new TestWorkspace(layeredProject(SCENARIOS));
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
    return { graph, root, services, idProvider, metadataManager };
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
});

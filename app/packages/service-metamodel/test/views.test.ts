import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TextDocument, URI, type LangiumDocument } from "langium";
import type { TextEdit, WorkspaceEdit } from "vscode-languageserver-types";
import { initializePluginContext, LangiumInstancePool } from "@mdeo/service-common";
import type { GraphMetadata, ModelState } from "@mdeo/language-shared";

initializePluginContext();

const {
    metamodelPluginProvider,
    MetamodelMetadataManager,
    MetamodelModelIdProvider,
    MetamodelViewEdits,
    ViewScopedModelIdRegistry,
    isExtensionInView,
    partitionMetadata,
    resolveViews,
    restrictToView,
    scopeIdToView,
    viewKeyOfId
} = await import("@mdeo/language-metamodel");
const { DefaultModelIdRegistry, GModelIndex } = await import("@mdeo/language-shared");
const { readMetamodelViewInfo, METAMODEL_VIEWS_ARG, ACTIVE_METAMODEL_VIEW_ARG } =
    await import("@mdeo/protocol-metamodel");

type Services = any;

/**
 * One metamodel language instance, shared by all tests; every test parses its own documents.
 */
const pool = new LangiumInstancePool({
    maxInstances: 1,
    languagePluginProvider: metamodelPluginProvider,
    serviceModule: undefined,
    languageId: "metamodel",
    extension: ".mm",
    backendUrl: "http://localhost:1"
} as unknown as ConstructorParameters<typeof LangiumInstancePool>[0]);
const services: Services = (
    pool as unknown as { createInstance: (plugins: unknown[], key: string) => { services: Services } }
).createInstance([], "[]").services;

let documentCounter = 0;

/**
 * Parses, links and validates metamodel text.
 */
async function parse(text: string): Promise<LangiumDocument<any>> {
    const uri = URI.parse(`file:///project/test${++documentCounter}.mm`);
    const document = services.shared.workspace.LangiumDocumentFactory.fromString(text, uri);
    services.shared.workspace.LangiumDocuments.addDocument(document);
    await services.shared.workspace.DocumentBuilder.build([document], { validation: true });
    return document;
}

/**
 * Returns the diagnostics of a document as `severity: message` strings (1 = error, 2 = warning).
 */
function diagnostics(document: LangiumDocument): string[] {
    return (document.diagnostics ?? []).map((diagnostic) => `${diagnostic.severity}: ${diagnostic.message}`);
}

/**
 * Applies workspace edits to the text of a document.
 */
function applyEdits(document: LangiumDocument, edits: (WorkspaceEdit | undefined)[]): string {
    const textEdits: TextEdit[] = edits.flatMap((edit) => edit?.changes?.[document.uri.toString()] ?? []);
    return TextDocument.applyEdits(document.textDocument, textEdits);
}

/**
 * Creates the view edit builder for a document, the way a diagram handler does.
 */
function viewEdits(document: LangiumDocument<any>): InstanceType<typeof MetamodelViewEdits> {
    return new MetamodelViewEdits({
        sourceModel: document.parseResult.value,
        languageServices: services
    } as ModelState);
}

/**
 * Finds a class or enum of a metamodel by name.
 */
function element(document: LangiumDocument<any>, name: string): any {
    return document.parseResult.value.elements.find((e: any) => e.name === name);
}

const SHOP = `class Customer {
    name: string
}

class Order {
}

class Item {
}

class Special extends Item {
}

enum Status {
    OPEN
}

Customer.orders[*] <--> Order.customer
Order.items[*] *--> Item.order

view Sales {
    Customer
    Order
    Status
}

view Catalog {
    Item
    Special
}
`;

describe("view grammar", () => {
    it("parses views and links their entries to classes and enums", async () => {
        const document = await parse(SHOP);
        assert.deepEqual(document.parseResult.parserErrors, []);
        assert.deepEqual(diagnostics(document), []);

        const views = document.parseResult.value.views;
        assert.deepEqual(
            views.map((view: any) => [view.name, view.entries.map((entry: any) => entry.element.ref?.name)]),
            [
                ["Sales", ["Customer", "Order", "Status"]],
                ["Catalog", ["Item", "Special"]]
            ]
        );
        assert.equal(views[0].entries[2].element.ref.$type, "Enum");
    });

    it("keeps views out of the elements, so they are no symbols of the metamodel", async () => {
        const document = await parse(`class Sales {\n}\n\nview Sales {\n    Sales\n}\n`);
        assert.deepEqual(diagnostics(document), []);
        assert.deepEqual(
            document.parseResult.value.elements.map((e: any) => e.$type),
            ["Class"]
        );
    });

    it("accepts empty views and views written on one line", async () => {
        const document = await parse(`class A {\n}\nview Empty {}\nview One { A }\n`);
        assert.deepEqual(document.parseResult.parserErrors, []);
        assert.deepEqual(
            document.parseResult.value.views.map((view: any) => view.entries.length),
            [0, 1]
        );
    });

    it("needs `view` escaped where it is used as a name", async () => {
        const escaped = await parse("class Screen {\n    `view`: string\n}\n");
        assert.deepEqual(escaped.parseResult.parserErrors, []);
        assert.equal(escaped.parseResult.value.elements[0].properties[0].name, "view");

        const plain = await parse("class Screen {\n    view: string\n}\n");
        assert.notDeepEqual(plain.parseResult.parserErrors, []);
    });

    it("reports unknown entries, duplicate entries and duplicate view names", async () => {
        const document = await parse(`class A {\n}\nview V {\n    A\n    A\n    Missing\n}\nview V {\n}\n`);
        const found = diagnostics(document);
        assert.ok(
            found.some((d) => /Missing/.test(d) && d.startsWith("1:")),
            found.join("\n")
        );
        assert.ok(found.includes("2: 'A' is already part of view 'V'."), found.join("\n"));
        assert.equal(found.filter((d) => d === "1: Duplicate view name: 'V'.").length, 2);
    });

    it("serializes views after the other elements", async () => {
        const document = await parse(`view V {\nA\n    E\n}\nclass A {\n}\nenum E {\n    X\n}\n`);
        const text = await services.workspace.WorkspaceEdit.serializeNode(document.parseResult.value, document);
        assert.equal(text, "class A {}\nenum E {\n    X\n}\nview V {\n    A\n    E\n}");
    });
});

describe("view contents", () => {
    it("shows the listed elements and the relations between them", async () => {
        const document = await parse(SHOP);
        const metamodel = document.parseResult.value;
        const [sales, catalog] = resolveViews(metamodel);
        const all = {
            classes: metamodel.elements.filter((e: any) => e.$type === "Class"),
            enums: metamodel.elements.filter((e: any) => e.$type === "Enum"),
            associations: metamodel.elements.filter((e: any) => e.$type === "Association")
        };

        const salesContents = restrictToView(all, sales);
        assert.deepEqual(
            salesContents.classes.map((c: any) => c.name),
            ["Customer", "Order"]
        );
        assert.deepEqual(
            salesContents.enums.map((e: any) => e.name),
            ["Status"]
        );
        // Customer <--> Order is shown, Order *--> Item is not: Item is not in the view.
        assert.equal(salesContents.associations.length, 1);
        assert.equal(salesContents.associations[0].target?.class?.ref?.name, "Order");

        assert.equal(restrictToView(all, catalog).associations.length, 0);
        const special = element(document, "Special");
        assert.equal(isExtensionInView(special, special.extensions.extensions[0], catalog), true);
        assert.equal(isExtensionInView(special, special.extensions.extensions[0], sales), false);
        assert.equal(isExtensionInView(special, special.extensions.extensions[0], undefined), true);
        assert.equal(restrictToView(all, undefined), all);
    });

    it("counts the first of several views of one name, and gives every view its own key", async () => {
        const document = await parse(`class A {\n}\nview \`a b\` {\n    A\n}\nview a_b {\n}\nview a_b {\n    A\n}\n`);
        const views = resolveViews(document.parseResult.value);
        assert.deepEqual(
            views.map((view: any) => [view.name, view.key, view.members.size]),
            [
                ["a b", "a_b", 1],
                ["a_b", "a_b_2", 0]
            ]
        );
    });
});

describe("view ids", () => {
    it("scopes ids to a view and reads the view back, also from derived ids", () => {
        const id = scopeIdToView("AssociationEnd_A_b", "Sales");
        assert.equal(id, "AssociationEnd_A_b__view_Sales");
        assert.equal(viewKeyOfId(id), "Sales");
        assert.equal(viewKeyOfId(`${id}__multiplicity`), "Sales");
        assert.equal(viewKeyOfId("Class_A__name"), undefined);
        assert.equal(viewKeyOfId("Class_A"), undefined);
    });

    it("lets the model index find the node behind a view-scoped element id", async () => {
        const document = await parse(SHOP);
        const metamodel = document.parseResult.value;
        const idProvider = createIdProvider();
        const registry = new ViewScopedModelIdRegistry(new DefaultModelIdRegistry(metamodel, idProvider), "Sales");
        const index = new GModelIndex();
        index.indexSourceModelRoot(metamodel, registry);

        const customer = element(document, "Customer");
        assert.equal(index.getElementId(customer), "Class_Customer__view_Sales");
        assert.equal(index.getAstNodeById("Class_Customer__view_Sales"), customer);
        assert.equal(index.getAstNodeById("Class_Customer__view_Sales__name"), customer);
        assert.equal(index.getAstNodeById("Class_Customer"), customer);
    });

    it("publishes the views on the graph and reads them back defensively", () => {
        assert.deepEqual(
            readMetamodelViewInfo({ [METAMODEL_VIEWS_ARG]: '["A","B"]', [ACTIVE_METAMODEL_VIEW_ARG]: "B" }),
            { views: ["A", "B"], activeView: "B" }
        );
        assert.deepEqual(
            readMetamodelViewInfo({ [METAMODEL_VIEWS_ARG]: '["A"]', [ACTIVE_METAMODEL_VIEW_ARG]: "Gone" }),
            { views: ["A"], activeView: undefined }
        );
        assert.deepEqual(readMetamodelViewInfo({ [METAMODEL_VIEWS_ARG]: "not json" }), {
            views: [],
            activeView: undefined
        });
        assert.deepEqual(readMetamodelViewInfo(undefined), { views: [], activeView: undefined });
    });
});

describe("view edits", () => {
    it("adds classes and enums to a view, skipping those it shows already", async () => {
        const document = await parse(SHOP);
        const sales = document.parseResult.value.views[0];
        const text = applyEdits(document, [
            viewEdits(document).addToView(sales, [element(document, "Item"), element(document, "Customer")])
        ]);
        assert.match(text, /view Sales \{\n {4}Customer\n {4}Order\n {4}Status\n {4}Item\n\}/);
        assert.equal(viewEdits(document).addToView(sales, [element(document, "Order")]), undefined);
    });

    it("adds to an empty view written on one line", async () => {
        const document = await parse(`class A {\n}\n\nview V {}\n`);
        const text = applyEdits(document, [
            viewEdits(document).addToView(document.parseResult.value.views[0], [element(document, "A")])
        ]);
        assert.equal(text, `class A {\n}\n\nview V {\n    A\n}\n`);
        assert.deepEqual((await parse(text)).parseResult.parserErrors, []);
    });

    it("escapes names that are keywords", async () => {
        const document = await parse("class `view` {\n}\n\nview V {\n}\n");
        const text = applyEdits(document, [
            viewEdits(document).addToView(document.parseResult.value.views[0], [element(document, "view")])
        ]);
        assert.match(text, /view V \{\n {4}`view`\n\}/);
    });

    it("removes classes from one view or from all of them", async () => {
        const document = await parse(
            `class A {\n}\nclass B {\n}\nview V {\n    A\n    B\n    A\n}\nview W {\n    A\n}\n`
        );
        const [v] = document.parseResult.value.views;
        const a = new Set([element(document, "A")]);

        const fromV = await parse(applyEdits(document, viewEdits(document).removeFromView(v, a)));
        assert.deepEqual(
            fromV.parseResult.value.views.map((view: any) => view.entries.map((e: any) => e.element.$refText)),
            [["B"], ["A"]]
        );

        const fromAll = await parse(applyEdits(document, viewEdits(document).removeFromAllViews(a)));
        assert.deepEqual(
            fromAll.parseResult.value.views.map((view: any) => view.entries.map((e: any) => e.element.$refText)),
            [["B"], []]
        );
        assert.deepEqual(diagnostics(fromAll), []);
    });

    it("creates, renames and deletes views", async () => {
        const document = await parse(`class A {\n}\nenum E {\n    X\n}\n\nview NewView {\n}\n`);
        assert.equal(MetamodelViewEdits.uniqueViewName(document.parseResult.value), "NewView1");

        const created = await parse(
            applyEdits(document, [
                await viewEdits(document).createView("NewView1", [element(document, "A"), element(document, "E")])
            ])
        );
        assert.deepEqual(diagnostics(created), []);
        assert.deepEqual(
            created.parseResult.value.views.map((view: any) => [
                view.name,
                view.entries.map((e: any) => e.element.ref?.name)
            ]),
            [
                ["NewView", []],
                ["NewView1", ["A", "E"]]
            ]
        );

        const renamed = await parse(
            applyEdits(created, [viewEdits(created).renameView(created.parseResult.value.views[1], "Two words")])
        );
        assert.deepEqual(
            renamed.parseResult.value.views.map((view: any) => view.name),
            ["NewView", "Two words"]
        );

        const deleted = await parse(
            applyEdits(renamed, [viewEdits(renamed).deleteView(renamed.parseResult.value.views[0])])
        );
        assert.deepEqual(
            deleted.parseResult.value.views.map((view: any) => view.name),
            ["Two words"]
        );
        assert.equal(deleted.parseResult.value.elements.length, 2);
    });

    it("creates the first view of an empty file", async () => {
        const document = await parse("");
        const text = applyEdits(document, [await viewEdits(document).createView("NewView", [])]);
        const created = await parse(text);
        assert.deepEqual(created.parseResult.parserErrors, []);
        assert.deepEqual(
            created.parseResult.value.views.map((view: any) => view.name),
            ["NewView"]
        );
    });
});

/**
 * Creates the model id provider of the metamodel diagram outside a diagram session.
 */
function createIdProvider(): any {
    const provider = new MetamodelModelIdProvider() as any;
    provider.reflection = services.shared.AstReflection;
    provider.languageServices = services;
    return provider;
}

/**
 * Creates the metadata manager of the metamodel diagram outside a diagram session.
 */
function createMetadataManager(): any {
    const manager = new MetamodelMetadataManager() as any;
    manager.reflection = services.shared.AstReflection;
    manager.languageServices = services;
    manager.modelIdProvider = createIdProvider();
    return manager;
}

/**
 * Returns the position stored for a node.
 */
function position(metadata: GraphMetadata, id: string): { x: number; y: number } | undefined {
    return (metadata.nodes[id]?.meta as { position?: { x: number; y: number } } | undefined)?.position;
}

/**
 * Builds metadata in which every class and enum sits at the given position.
 */
async function laidOut(
    document: LangiumDocument<any>,
    positions: Record<string, { x: number; y: number; prefWidth?: number }>
): Promise<GraphMetadata> {
    const manager = createMetadataManager();
    const empty: GraphMetadata = { nodes: {}, edges: {} };
    const metadata: GraphMetadata = (await manager.validateMetadata(document.parseResult.value, empty, empty)) ?? empty;
    for (const [id, { x, y, prefWidth }] of Object.entries(positions)) {
        assert.ok(metadata.nodes[id] != undefined, `no metadata for ${id}`);
        metadata.nodes[id] = { ...metadata.nodes[id], meta: { position: { x, y }, prefWidth: prefWidth ?? 250 } };
    }
    return metadata;
}

describe("view layout", () => {
    it("gives every view a layout of its own", async () => {
        const document = await parse(SHOP);
        const manager = createMetadataManager();
        const empty: GraphMetadata = { nodes: {}, edges: {} };
        const metadata: GraphMetadata = await manager.validateMetadata(document.parseResult.value, empty, empty);

        const parts = partitionMetadata(metadata);
        assert.deepEqual([...parts.keys()].sort(), ["", "Catalog", "Sales"]);
        assert.deepEqual(
            Object.keys(parts.get("Sales")!.nodes)
                .filter((id) => id.startsWith("Class_") || id.startsWith("Enum_"))
                .sort(),
            ["Class_Customer__view_Sales", "Class_Order__view_Sales", "Enum_Status__view_Sales"]
        );
        // The association between Customer and Order is part of Sales, the one to Item is not.
        assert.deepEqual(
            Object.values(parts.get("Sales")!.edges).map((edge) => [edge.from, edge.to]),
            [["Class_Customer__view_Sales", "Class_Order__view_Sales"]]
        );
        assert.deepEqual(
            Object.values(parts.get("Catalog")!.edges).map((edge) => [edge.type, edge.from, edge.to]),
            [["edge:inheritance", "Class_Special__view_Catalog", "Class_Item__view_Catalog"]]
        );
        assert.equal(Object.keys(parts.get("")!.edges).length, 3);
    });

    it("keeps the layout of each view when the metamodel changes", async () => {
        const before = await parse(SHOP);
        const current = await laidOut(before, {
            Class_Customer: { x: 100, y: 100 },
            Class_Order: { x: 400, y: 100 },
            Class_Customer__view_Sales: { x: 10, y: 20 },
            Class_Order__view_Sales: { x: 30, y: 40 }
        });

        const after = await parse(SHOP.replace("    name: string", "    name: string\n    email: string"));
        const result =
            (await createMetadataManager().validateMetadata(after.parseResult.value, current, current)) ?? current;

        assert.deepEqual(position(result, "Class_Customer"), { x: 100, y: 100 });
        assert.deepEqual(position(result, "Class_Customer__view_Sales"), { x: 10, y: 20 });
        assert.deepEqual(position(result, "Class_Order__view_Sales"), { x: 30, y: 40 });
    });

    it("drops the layout of a view that is gone", async () => {
        const before = await parse(SHOP);
        const current = await laidOut(before, { Class_Item__view_Catalog: { x: 10, y: 10 } });
        const after = await parse(SHOP.replace(/view Catalog \{[^}]*\}\n/, ""));
        const result: GraphMetadata = await createMetadataManager().validateMetadata(
            after.parseResult.value,
            current,
            current
        );
        assert.deepEqual(
            Object.keys(result.nodes).filter((id) => viewKeyOfId(id) === "Catalog"),
            []
        );
        assert.ok(result.nodes["Class_Item"] != undefined);
    });

    it("places a class added to a view next to the ones laid out already", async () => {
        const before = await parse(SHOP);
        const current = await laidOut(before, {
            Class_Customer__view_Sales: { x: 100, y: 80, prefWidth: 200 },
            Class_Order__view_Sales: { x: 0, y: 300 },
            Enum_Status__view_Sales: { x: 50, y: 500 }
        });
        const after = await parse(SHOP.replace("    Status\n}", "    Status\n    Item\n}"));
        const result: GraphMetadata = await createMetadataManager().validateMetadata(
            after.parseResult.value,
            current,
            current
        );

        // Right of the rightmost node (Order: 0 + 250, Customer: 100 + 200, Status: 50 + 250), level with the topmost.
        assert.deepEqual(position(result, "Class_Item__view_Sales"), { x: 360, y: 80 });
        assert.deepEqual(position(result, "Class_Customer__view_Sales"), { x: 100, y: 80 });
    });

    it("leaves a view that has never been laid out to the editor", async () => {
        const before = await parse(`class A {\n}\nclass B {\n}\nview V {\n    A\n}\n`);
        const current = await laidOut(before, { Class_A: { x: 300, y: 300 } });
        const after = await parse(`class A {\n}\nclass B {\n}\nview V {\n    A\n    B\n}\n`);
        const result: GraphMetadata = await createMetadataManager().validateMetadata(
            after.parseResult.value,
            current,
            current
        );
        assert.deepEqual(position(result, "Class_A__view_V"), { x: 0, y: 0 });
        assert.deepEqual(position(result, "Class_B__view_V"), { x: 0, y: 0 });
    });
});

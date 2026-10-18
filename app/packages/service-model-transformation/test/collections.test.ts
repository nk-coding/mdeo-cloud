import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TextDocument } from "vscode-languageserver-textdocument";
import { TestWorkspace } from "./testWorkspace.ts";
import { layeredProject, SCENARIOS, COLLECTION_SCENARIOS } from "./fixtures.ts";

const { typedAstHandler } = await import("../dist/handler/typedAstHandler.js");
const { mdeoDiagnosticsRequestHandler } = await import("../../service-config-mdeo/dist/handler/mdeoRequestHandler.js");
const { ServiceMdeoMetamodelResolver } = await import("../../service-config-mdeo/dist/serviceMdeoMetamodelResolver.js");

const ALL = { ...SCENARIOS, ...COLLECTION_SCENARIOS };

/**
 * Builds one extra transformation next to the fixtures and returns its errors.
 */
async function errorsFor(text: string, path = "/optimize/case.mt", extra: Record<string, string> = {}) {
    const workspace = new TestWorkspace(layeredProject({ ...ALL, ...extra, [path]: text }));
    return (await workspace.build(path)).errors;
}

/**
 * Asserts that every expected fragment occurs in one of the errors, and that there are no
 * other errors.
 */
function assertErrors(errors: string[], expected: string[]): void {
    for (const fragment of expected) {
        assert.ok(
            errors.some((error) => error.includes(fragment)),
            `expected an error containing "${fragment}", got:\n${errors.join("\n")}`
        );
    }
    const unexpected = errors.filter((error) => !expected.some((fragment) => error.includes(fragment)));
    assert.deepEqual(unexpected, [], "unexpected errors");
}

/**
 * A layer-2 transformation (`base.mm`) with the given signature and body.
 */
const BASE = (body: string, metamodel = "../metamodel/base.mm") => `using "${metamodel}"

${body}
`;

/**
 * A layer-2 transformation in `/optimize/lib/`.
 */
const LIB = (body: string) => BASE(body, "../../metamodel/base.mm");

describe("collections and loops: parsing and validation", () => {
    it("parses and validates the scenarios without errors", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        for (const path of Object.keys(COLLECTION_SCENARIOS)) {
            const { errors, document } = await workspace.build(path);
            assert.deepEqual(
                document.parseResult.parserErrors.map((e: any) => e.message),
                [],
                path
            );
            assert.deepEqual(errors, [], path);
        }
    });

    it("links appends, loop variables and assignments", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        const componentsOn = (await workspace.build("/optimize/scenarios/components-on.mt")).document.parseResult.value;
        const [, components, names] = componentsOn.signature.parameters;
        const forMatch = componentsOn.statements[0];
        const append = forMatch.pattern.elements.find((e: any) => e.$type === "PatternCollectionAppend");
        assert.equal(append.variable.ref, components);
        const assignment = forMatch.doBlock.statements[0];
        assert.equal(assignment.$type, "AssignmentStatement");
        assert.equal(assignment.operator, "+=");
        assert.equal(assignment.variable.ref, names);

        const renameAll = (await workspace.build("/optimize/scenarios/rename-all.mt")).document.parseResult.value;
        const [first, loop] = renameAll.statements;
        assert.equal(first.$type, "AssignmentStatement");
        assert.equal(first.variable.ref, renameAll.signature.parameters[2]);
        assert.equal(loop.$type, "ForInStatement");
        assert.equal(loop.variable.name, "c");
        const reference = loop.doBlock.statements[0].pattern.elements[0];
        assert.equal(reference.instance.ref, loop.variable, "the loop variable is a bound node of the body's patterns");
    });

    it("checks collection parameters", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(
    in nested: List<List<int>>,
    in maybe: List<Component>?,
    in withDefault: List<int> = 1,
    in bag: Bag<int>,
    in kinds: Set<Kind>,
    out components: List<Component>,
    out labels: Set<string>
)`)
        );
        assertErrors(errors, [
            "The type of 'nested' must be List<T> or Set<T>",
            "The type of 'maybe' must be List<T> or Set<T>",
            "The collection input 'withDefault' cannot have a default value.",
            "Default value of type 'int' is not assignable to parameter 'withDefault'",
            "The type of 'bag' must be List<T> or Set<T>"
        ]);
    });

    it("needs no assignment of a collection output, which starts empty", async () => {
        const errors = await errorsFor(BASE(`transformation t(out components: List<Component>)`));
        assert.deepEqual(errors, []);
    });

    it("adds only to collection outputs, and only values of the element type", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in host: Container, in limit: int = 0, out count: int, out components: List<Component>)

match {
    var n = 0
    count = n
}
for match {
    host { }
    c: Component { }
    c.host -- host
    count += c
    limit += 1
    n += 1
    components += host
} do {
    components += c
    components += c.name
}`)
        );
        assertErrors(errors, [
            "'+=' adds to a collection output; 'count' is not an 'out' parameter of type List<…> or Set<…>.",
            "'+=' adds to a collection; 'count' is of type 'int'.",
            "Cannot assign the input parameter 'limit'",
            "'+=' adds to a collection; 'limit' is of type 'int'.",
            "'+=' adds to a collection output; 'n' is not an 'out' parameter",
            "'+=' adds to a collection; 'n' is of type 'int'.",
            "A value of type 'Container' cannot be added to 'components' of type 'List<Component>'.",
            "A value of type 'string' cannot be added to 'components' of type 'List<Component>'."
        ]);
    });

    it("adds all elements of a collection of the element type", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in more: List<Component>, out components: List<Component>)

components += more
match {
    p: Placement { }
    c: Component { }
    p.component -- c
    components += more
}`)
        );
        assert.deepEqual(errors, []);
    });

    it("rejects a collection as a node of a pattern", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in components: List<Component>)

match {
    components { }
}`)
        );
        assertErrors(errors, [
            "'components' is a collection, not a model element, so it cannot be a node of a pattern."
        ]);
    });

    it("loops only over collections, and binds a loop variable of scalars as a value", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in host: Container, in names: Set<string>)

for x in host do {
}
for n in names do {
    match {
        n { }
    }
    match {
        c: Container {
            name == n
        }
    }
}`)
        );
        assertErrors(errors, [
            "'for … in' runs over a collection (a List or Set); this expression is of type 'Container'.",
            "'n' runs over a collection of scalars"
        ]);
    });

    it("loops over a many-valued reference and over a filtered collection", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in placement: Placement, in components: List<Component>)

for candidate in placement.candidates do {
    match {
        candidate {
            name = "candidate"
        }
    }
}
for named in components.filter((c) => c.name != "") do {
    match {
        named { }
        h: Container { }
        named.host -- h
    }
}`)
        );
        assert.deepEqual(errors, []);
    });

    it("scopes a loop variable to its do block", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in components: List<Component>)

for c in components do {
}
match {
    c { }
}`)
        );
        assertErrors(errors, ["Could not resolve reference to ObjectInstanceDeclaration named 'c'"]);
    });

    it("checks assignments outside patterns", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in limit: int = 0, out count: int, out total: double?)

limit = 1
count = "many"
count = limit
total = 0.5`)
        );
        assertErrors(errors, [
            "Cannot assign the input parameter 'limit'",
            "Value type 'string' is not assignable to variable type 'int'."
        ]);
    });

    it("counts assignments outside patterns as definite assignments, but not those in loops", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in components: List<Component>, out first: int, out second: int)

first = 1
for c in components do {
    second = 2
}`)
        );
        assertErrors(errors, ["The output 'second' is not assigned on every path"]);
    });

    it("rejects loop variables, call outputs and objects of the same name", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in components: List<Component>)

for c in components do {
    match {
        c: Component { }
    }
}`)
        );
        assertErrors(errors, ["Duplicate name 'c'"]);
    });
});

describe("if call: validation", () => {
    /**
     * Callees of the if-call tests.
     */
    const CALLEES: Record<string, string> = {
        // fails only in its first match, before any change
        "/optimize/lib/safe.mt": LIB(`transformation safe(out found: Container)

match {
    c: Container {
        name == "x"
    }
    found = c
}
match {
    found {
        name = "seen"
    }
}`),
        // changes the model, then may fail
        "/optimize/lib/unsafe.mt": LIB(`transformation unsafe()

match {
    create c: Container {
        name = "made"
    }
}
match {
    p: Placement { }
}`),
        // calls the unsafe one after a change of its own
        "/optimize/lib/calls-unsafe.mt": LIB(`import { unsafe } from "./unsafe.mt"

transformation callsUnsafe()

call unsafe()`),
        // a loop whose later iterations may fail after earlier changes
        "/optimize/lib/loop-unsafe.mt": LIB(`transformation loopUnsafe(in components: List<Component>)

for c in components do {
    match {
        c {
            name = "touched"
        }
        h: Container { }
        c.host -- h
    }
}`),
        // its changes come after every possible failure, even through a safe callee
        "/optimize/lib/calls-safe.mt": LIB(`import { safe } from "./safe.mt"

transformation callsSafe()

call safe() -> (found: f)
if match {
    p: Placement { }
} then {
    match {
        create c: Container {
            name = "made"
        }
    }
}`)
    };

    const CALLER = (body: string) => `using "../metamodel/base.mm"
import { safe } from "./lib/safe.mt"
import { unsafe } from "./lib/unsafe.mt"
import { callsUnsafe } from "./lib/calls-unsafe.mt"
import { loopUnsafe } from "./lib/loop-unsafe.mt"
import { callsSafe } from "./lib/calls-safe.mt"

${body}
`;

    it("accepts callees that fail before they change the model", async () => {
        const errors = await errorsFor(
            CALLER(`if call safe() -> (found: f) then {
    match {
        f { }
    }
} else {
    stop
}
if call callsSafe() then {
}`),
            "/optimize/case.mt",
            CALLEES
        );
        assert.deepEqual(errors, []);
    });

    it("rejects callees that may fail after a change, directly, through a call or in a loop", async () => {
        const errors = await errorsFor(
            CALLER(`if call unsafe() then {
}
if call callsUnsafe() then {
}
if call loopUnsafe(components = []) then {
}`),
            "/optimize/case.mt",
            CALLEES
        );
        assertErrors(errors, [
            "'if call' needs a transformation that fails before it changes the model, but 'unsafe' can fail at line 10 of unsafe.mt (a match that may find nothing) after the change at line 5 of unsafe.mt.",
            "but 'callsUnsafe' calls 'unsafe' at line 7 of calls-unsafe.mt, which can fail at line 10 of unsafe.mt",
            "but 'loopUnsafe' can fail at line 6 of loop-unsafe.mt (a match that may find nothing) after the change at line 5 of loop-unsafe.mt",
            "Argument of type 'List<"
        ]);
    });

    it("binds the outputs of if call in the then block only", async () => {
        const errors = await errorsFor(
            CALLER(`if call safe() -> (found: f) then {
} else {
    match {
        f { }
    }
}
match {
    f { }
}`),
            "/optimize/case.mt",
            CALLEES
        );
        assertErrors(errors, ["Could not resolve reference to ObjectInstanceDeclaration named 'f'"]);
        assert.equal(errors.length, 2);
    });

    it("checks the arguments and outputs of if call like those of call", async () => {
        const errors = await errorsFor(
            CALLER(`if call safe(found = 1) -> (nothing: n) then {
}`),
            "/optimize/case.mt",
            CALLEES
        );
        assertErrors(errors, [
            "'found' is an output of 'safe'; bind it after '->'",
            "'nothing' is not a parameter of 'safe'",
            "Argument of type 'int' is not assignable to parameter 'found'"
        ]);
    });

    it("treats if call with terminating branches as terminating", async () => {
        const errors = await errorsFor(
            CALLER(`if call safe() then {
    stop
} else {
    kill
}
match {
    p: Placement { }
}`),
            "/optimize/case.mt",
            CALLEES
        );
        assertErrors(errors, ["Unreachable statement"]);
    });
});

describe("optional element inputs that pin an object of the first pattern", () => {
    it("pins the object of the same name and links references to the object", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        const { document, errors } = await workspace.build("/optimize/scenarios/move-placement.mt");
        assert.deepEqual(errors, []);
        const root = document.parseResult.value;
        const pattern = root.statements[0].pattern.elements;
        const link = pattern.find((e: any) => e.$type === "PatternLink");
        assert.equal(link.source.object.ref, pattern[0], "references resolve to the pattern object, not the input");
    });

    it("is optional: callers and configurations may leave it out", async () => {
        const errors = await errorsFor(`using "../metamodel/shop.mm"
import { movePlacement } from "./scenarios/move-placement.mt"

call movePlacement() -> (moved: m)
`);
        assert.deepEqual(errors, []);

        const workspace = new TestWorkspace(layeredProject(ALL));
        const configErrors = await workspace.configDiagnostics(
            mdeoDiagnosticsRequestHandler,
            { MdeoMetamodelResolver: () => new ServiceMdeoMetamodelResolver() },
            "/optimize/shop.config",
            `search {
    mutations {
        using "./scenarios/move-placement.mt"
    }
}
`,
            "/metamodel/shop.mm"
        );
        assert.deepEqual(configErrors, []);
    });

    it("accepts a pinned object of the input's class or a superclass of it", async () => {
        const errors = await errorsFor(`using "../metamodel/shop.mm"

transformation t(in service: Service?, in other: Component?)

match {
    service: Component { }
    other: Service { }
}
`);
        assertErrors(errors, [
            "The input 'other' pins the object 'other' of class 'Service', so its type must be 'Service' or a subclass of it ('Component' is not)"
        ]);
    });

    it("rejects a nullable element input without an object of the first pattern", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in lonely: Placement?, in late: Placement?)

match {
    c: Container { }
}
match {
    late: Placement { }
}`)
        );
        assertErrors(errors, [
            "The element input 'lonely' is nullable, so it must pin an object of the first pattern",
            "The nullable element input 'late' can only pin an object of the first pattern",
            "Duplicate name 'late'"
        ]);
    });

    it("rejects pinning an object the pattern creates", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in made: Container?)

match {
    create made: Container {
        name = "x"
    }
}`)
        );
        assertErrors(errors, ["The input 'made' pins the object 'made', which the pattern creates"]);
    });

    it("explains that a required element input is already bound", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in placement: Placement)

match {
    placement: Placement { }
}`)
        );
        assertErrors(errors, [
            "The element input 'placement' is required, so it is bound when the transformation starts; reference it as 'placement { }'",
            "Duplicate name 'placement'"
        ]);
    });

    it("reports references to a pinning input where the object is not visible", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in placement: Placement?)

for match {
    placement: Placement { }
} do {
}
match {
    placement { }
}`)
        );
        assertErrors(errors, [
            "'placement' is only bound where the object 'placement' of the first pattern is visible"
        ]);
    });

    it("explains the pinning on hover", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        const { document, instance } = await workspace.build("/optimize/scenarios/move-placement.mt");
        const hover = async (needle: string) => {
            const text = document.textDocument.getText();
            const result = await instance.services.lsp.HoverProvider.getHoverContent(document, {
                textDocument: { uri: document.uri.toString() },
                position: document.textDocument.positionAt(text.indexOf(needle) + 1)
            });
            return (result?.contents as any)?.value ?? "";
        };
        assert.match(
            await hover("placement: Placement { }"),
            /pinned by the optional input `in placement: Placement\?`/
        );
        assert.match(await hover("placement: Placement?"), /optional input that pins the object `placement`/);
    });

    it("pins the object of a first if match, for match or while match too", async () => {
        for (const statement of ["if match", "for match", "while match"]) {
            const tail = statement === "if match" ? "then {\n}" : "do {\n}";
            const errors = await errorsFor(
                BASE(`transformation t(in placement: Placement?)

${statement} {
    placement: Placement { }
    c: Component { }
    placement.component -- c
} ${tail}`)
            );
            assert.deepEqual(errors, [], statement);
        }
    });
});

describe("collections, loops, if call and pinning: typed AST and language server", () => {
    it("converts the scenarios", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        for (const path of Object.keys(COLLECTION_SCENARIOS)) {
            const run = await workspace.run(typedAstHandler, path);
            assert.deepEqual(run.errors, [], path);
            assert.notEqual(run.data, null, path);
        }
    });

    it("describes collection, optional and pinning parameters", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        const collecting = (await workspace.run(typedAstHandler, "/optimize/scenarios/call-collecting.mt")).data;
        const componentsOn = collecting.callees["/optimize/scenarios/components-on.mt"];
        assert.deepEqual(
            componentsOn.signature.parameters.map((p: any) => [
                p.name,
                p.collection,
                p.elementsAreElements,
                p.isElement
            ]),
            [
                ["host", undefined, undefined, true],
                ["components", "List", true, false],
                ["names", "Set", false, false]
            ]
        );
        const [forMatch] = componentsOn.statements;
        assert.deepEqual(
            forMatch.pattern.elements.map((e: any) => e.kind),
            ["objectInstance", "objectInstance", "link", "collectionAppend"]
        );
        assert.equal(forMatch.doBlock[0].kind, "assignment");
        assert.equal(forMatch.doBlock[0].operator, "+=");
        assert.deepEqual(
            collecting.statements.map((s: any) => s.kind),
            ["call", "call", "forIn", "forIn", "call", "call", "call", "forIn", "match"]
        );
        const loop = collecting.statements[2];
        assert.equal(loop.variable, "x");
        assert.equal(loop.doBlock[0].arguments[0].value.scope, 2, "the loop variable is bound one level below");

        const placement = (await workspace.run(typedAstHandler, "/optimize/scenarios/move-placement.mt")).data;
        assert.deepEqual(placement.signature.parameters[0], {
            name: "placement",
            direction: "in",
            type: placement.signature.parameters[0].type,
            isElement: true,
            optional: true,
            pinsNode: true
        });

        const ifCall = (await workspace.run(typedAstHandler, "/optimize/scenarios/call-if.mt")).data;
        assert.deepEqual(
            ifCall.statements.map((s: any) => [s.kind, s.call.name, s.thenBlock.length, s.elseBlock?.length]),
            [
                ["ifCall", "noPlacement", 1, 1],
                ["ifCall", "moveComponent", 1, 1]
            ]
        );
        assert.equal(ifCall.statements[1].thenBlock[0].pattern.elements[0].objectInstance.name, "t");
    });

    it("formats the new statements back to text that parses the same", async () => {
        const workspace = new TestWorkspace(layeredProject(ALL));
        for (const path of [
            "/optimize/scenarios/components-on.mt",
            "/optimize/scenarios/rename-all.mt",
            "/optimize/scenarios/call-collecting.mt",
            "/optimize/scenarios/call-if.mt",
            "/optimize/scenarios/move-placement.mt"
        ]) {
            const { document, instance } = await workspace.build(path);
            const edits = await instance.services.lsp.Formatter.formatDocument(document, {
                textDocument: { uri: document.uri.toString() },
                options: { tabSize: 4, insertSpaces: true }
            });
            const formatted = TextDocument.applyEdits(document.textDocument as any, edits);
            const reparsed = new TestWorkspace(layeredProject({ ...ALL, [path]: formatted }));
            const result = await reparsed.build(path);
            assert.deepEqual(result.errors, [], formatted);
            const flat = formatted.replace(/\s+/g, " ");
            if (path.endsWith("components-on.mt")) {
                assert.match(flat, /components \+= c/);
                assert.match(flat, /names \+= c\.name/);
            }
            if (path.endsWith("call-collecting.mt")) {
                assert.match(flat, /for x in here do \{ call markComponent\(component = x, label = "on b"\) \}/);
            }
            if (path.endsWith("call-if.mt")) {
                assert.match(flat, /if call moveComponent\(\) -> \(moved: m, target: t\) then \{/);
                assert.match(flat, /\} else \{ kill \}/);
            }
        }
    });

    it("offers only inputs as arguments and only outputs after the arrow", async () => {
        const text = `using "../metamodel/shop.mm"
import { moveComponent } from "./generic/move-component.mt"

call moveComponent() -> ()
`;
        const workspace = new TestWorkspace(layeredProject({ "/optimize/probe.mt": text }));
        const { document, instance } = await workspace.build("/optimize/probe.mt");
        const complete = async (offset: number) => {
            const result = await instance.services.lsp.CompletionProvider.getCompletion(document, {
                textDocument: { uri: document.uri.toString() },
                position: document.textDocument.positionAt(offset)
            });
            return (result?.items ?? []).map((item: any) => item.label);
        };
        const call = text.indexOf("call moveComponent()");
        const inputs = await complete(call + "call moveComponent(".length);
        assert.ok(inputs.includes("maxCandidates"), inputs.join(", "));
        for (const output of ["moved", "source", "target", "choices"]) {
            assert.ok(!inputs.includes(output), `${output} offered as an argument: ${inputs.join(", ")}`);
        }
        const outputs = await complete(call + "call moveComponent() -> (".length);
        assert.ok(outputs.includes("moved") && outputs.includes("choices"), outputs.join(", "));
        assert.ok(!outputs.includes("maxCandidates"), outputs.join(", "));
    });

    it("infers the element type of a loop variable for expressions", async () => {
        const errors = await errorsFor(
            BASE(`transformation t(in components: List<Component>, out names: List<string>)

for c in components do {
    names += c.name
    match {
        c { }
        where c.name != ""
    }
}`)
        );
        assert.deepEqual(errors, []);
    });
});

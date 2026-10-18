import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TextDocument } from "vscode-languageserver-textdocument";
import { TestWorkspace, errorsOf } from "./testWorkspace.ts";
import { layeredProject, SCENARIOS } from "./fixtures.ts";

/**
 * Builds one extra transformation next to the fixtures and returns its errors.
 */
async function errorsFor(text: string, path = "/optimize/case.mt", extra: Record<string, string> = {}) {
    const workspace = new TestWorkspace(layeredProject({ ...extra, [path]: text }));
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
 * Returns the position of the n-th occurrence of a text in a document.
 */
function positionOf(document: any, text: string, occurrence = 1, offset = 0) {
    const content: string = document.textDocument.getText();
    let index = -1;
    for (let i = 0; i < occurrence; i++) {
        index = content.indexOf(text, index + 1);
        assert.ok(index >= 0, `"${text}" not found`);
    }
    return document.textDocument.positionAt(index + offset);
}

const SHOP_CALLER = (body: string) => `using "../metamodel/shop.mm"
import { moveComponent } from "./generic/move-component.mt"
import { followReader } from "./sync/follow-reader.mt"

${body}
`;

describe("callable transformations: parsing and linking", () => {
    it("parses and validates the worked example and the scenarios without errors", async () => {
        const workspace = new TestWorkspace(layeredProject(SCENARIOS));
        for (const path of [
            "/optimize/generic/move-component.mt",
            "/optimize/sync/follow-reader.mt",
            "/optimize/move-with-stores.mt",
            ...Object.keys(SCENARIOS)
        ]) {
            const { errors, document } = await workspace.build(path);
            assert.deepEqual(
                document.parseResult.parserErrors.map((e: any) => e.message),
                [],
                path
            );
            assert.deepEqual(errors, [], path);
        }
    });

    it("parses the parts of a signature", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document } = await workspace.build("/optimize/generic/move-component.mt");
        const signature = document.parseResult.value.signature;
        assert.equal(signature.name, "moveComponent");
        assert.deepEqual(
            signature.parameters.map((p: any) => [p.direction, p.name, p.type.name, p.defaultValue != undefined]),
            [
                ["in", "maxCandidates", "int", true],
                ["out", "moved", "Component", false],
                ["out", "source", "Container", false],
                ["out", "target", "Container", false],
                ["out", "choices", "int", false]
            ]
        );
    });

    it("links callees, arguments, outputs and references to outputs and parameters", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document } = await workspace.build("/optimize/move-with-stores.mt");
        const root = document.parseResult.value;
        const [move, follow] = root.statements;

        assert.equal(root.imports[0].imports[0].entity.ref.$type, "TransformationSignature");
        assert.equal(move.callee.ref.name, "moveComponent");
        assert.equal(move.callee.ref.$container.$document.uri.path, "/optimize/generic/move-component.mt");
        assert.equal(move.arguments[0].parameter.ref.$type, "TransformationParameter");
        assert.equal(move.arguments[0].parameter.ref.name, "maxCandidates");
        assert.deepEqual(
            move.outputs.map((o: any) => [o.parameter.ref.name, o.name]),
            [
                ["moved", "m"],
                ["source", "a"],
                ["target", "b"],
                ["choices", "n"]
            ]
        );
        assert.equal(
            follow.arguments[0].parameter.ref.$container.$container.$document.uri.path,
            "/optimize/sync/follow-reader.mt"
        );

        // the objects of the last match are the outputs of the first call
        const pattern = root.statements[2].pattern.elements;
        assert.equal(pattern[0].instance.ref, move.outputs[0]);
        assert.equal(pattern[1].instance.ref, move.outputs[2]);
    });

    it("resolves element inputs as the bound nodes of the callee's patterns", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document } = await workspace.build("/optimize/sync/follow-reader.mt");
        const root = document.parseResult.value;
        const pattern = root.statements[0].pattern.elements;
        const moved = root.signature.parameters[0];
        assert.equal(pattern[0].instance.ref, moved);
        const readerLink = pattern.find((e: any) => e.$type === "PatternLink" && e.source.object.$refText === "feed");
        assert.equal(readerLink.target.object.ref, moved);
    });

    it("assigns an out parameter with a reassignment", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document } = await workspace.build("/optimize/generic/move-component.mt");
        const root = document.parseResult.value;
        const assignment = root.statements[0].pattern.elements.find(
            (e: any) => e.$type === "PatternVariableReassignment" && e.variable.$refText === "moved"
        );
        assert.equal(assignment.variable.ref, root.signature.parameters[1]);
    });

    it("formats signatures, imports and calls back to text that parses the same", async () => {
        const workspace = new TestWorkspace(layeredProject());
        for (const path of ["/optimize/generic/move-component.mt", "/optimize/move-with-stores.mt"]) {
            const { document, instance } = await workspace.build(path);
            const edits = await instance.services.lsp.Formatter.formatDocument(document, {
                textDocument: { uri: document.uri.toString() },
                options: { tabSize: 4, insertSpaces: true }
            });
            const formatted = TextDocument.applyEdits(document.textDocument as any, edits);
            const reparsed = new TestWorkspace(layeredProject({ [path]: formatted }));
            const result = await reparsed.build(path);
            assert.deepEqual(result.errors, [], formatted);
            // lists that do not fit on the line break one item per line
            const flat = formatted.replace(/\(\s+/g, "(").replace(/,\s+/g, ", ").replace(/\s+\)/g, ")");
            if (path.endsWith("move-with-stores.mt")) {
                assert.match(flat, /import \{ moveComponent \} from "\.\/generic\/move-component\.mt"/);
                assert.match(flat, /call followReader\(moved = m, target = b\)/);
                assert.match(
                    flat,
                    /call moveComponent\(maxCandidates = 4\) -> \(moved: m, source: a, target: b, choices: n\)/
                );
            } else {
                assert.match(
                    flat,
                    /transformation moveComponent\(in maxCandidates: int = 0, out moved: Component, out source: Container, out target: Container, out choices: int\)/
                );
            }
        }
    });
});

describe("callable transformations: validation", () => {
    it("reports unknown, misdirected and repeated arguments and outputs", async () => {
        const errors = await errorsFor(
            SHOP_CALLER(
                `call moveComponent(maxCandidates = 1, nope = 1, moved = 2, maxCandidates = 3) -> (maxCandidates: q, moved: m, moved: m2)`
            )
        );
        assertErrors(errors, [
            "'nope' is not a parameter of 'moveComponent' (its parameters: in maxCandidates, out moved, out source, out target, out choices).",
            "'moved' is an output of 'moveComponent'; bind it after '->'",
            "The input 'maxCandidates' of 'moveComponent' is passed twice.",
            "'maxCandidates' is an input of 'moveComponent'; pass it as an argument",
            "The output 'moved' of 'moveComponent' is bound twice.",
            "Argument of type 'int' is not assignable to parameter 'moved' of type 'Component'."
        ]);
    });

    it("checks the types of arguments", async () => {
        const errors = await errorsFor(
            SHOP_CALLER(`match {
    c: Container { }
}
call moveComponent(maxCandidates = "four")
call followReader(moved = c, target = c)`)
        );
        assertErrors(errors, [
            "Argument of type 'string' is not assignable to parameter 'maxCandidates' of type 'int'.",
            "Argument of type 'Container' is not assignable to parameter 'moved' of type 'Component'."
        ]);
    });

    it("accepts an argument of a subclass of the parameter's class", async () => {
        const errors = await errorsFor(
            SHOP_CALLER(`match {
    s: Service { }
    c: Container { }
}
call followReader(moved = s, target = c)`)
        );
        assert.deepEqual(errors, []);
    });

    it("reports missing required inputs", async () => {
        const errors = await errorsFor(SHOP_CALLER(`match {\n    c: Container { }\n}\ncall followReader(target = c)`));
        assertErrors(errors, ["Missing required input 'moved' of 'followReader'."]);
    });

    it("reports a call of a transformation that is not imported", async () => {
        const errors = await errorsFor(`using "../metamodel/shop.mm"\n\ncall moveComponent()\n`);
        assertErrors(errors, [
            `Unknown transformation 'moveComponent'. Import it first, e.g. import { moveComponent } from "./moveComponent.mt".`
        ]);
    });

    it("reports imports of missing files, files without a signature and wrong names", async () => {
        const errors = await errorsFor(
            `using "../metamodel/shop.mm"
import { a } from "./missing.mt"
import { plain } from "./plain.mt"
import { moveIt } from "./generic/move-component.mt"
`,
            "/optimize/case.mt",
            { "/optimize/plain.mt": `using "../metamodel/shop.mm"\n\nmatch {\n    c: Container { }\n}\n` }
        );
        assertErrors(errors, [
            "Cannot resolve import path './missing.mt'.",
            "'./plain.mt' declares no signature ('transformation name(…)'), so it cannot be called.",
            "'./generic/move-component.mt' declares the transformation 'moveComponent', not 'moveIt'.",
            "Could not resolve reference to TransformationSignature named 'a'.",
            "Could not resolve reference to TransformationSignature named 'plain'.",
            "Could not resolve reference to TransformationSignature named 'moveIt'."
        ]);
    });

    it("accepts a callee of the caller's metamodel or of one it imports, and rejects any other", async () => {
        // shop.mm imports base.mm: the layer-2 moveComponent may be called from a layer-3 caller
        assert.deepEqual(await errorsFor(SHOP_CALLER("call moveComponent()")), []);

        // a base.mm transformation may not call a shop.mm one
        const upwards = await errorsFor(
            `using "../metamodel/base.mm"
import { followReader } from "./sync/follow-reader.mt"

match {
    c: Component { }
    d: Container { }
}
call followReader(moved = c, target = d)
`
        );
        assertErrors(upwards, [
            "'./sync/follow-reader.mt' uses the metamodel '../../metamodel/shop.mm', which is neither"
        ]);

        // an unrelated metamodel
        const unrelated = await errorsFor(
            `using "../metamodel/other.mm"
import { moveComponent } from "./generic/move-component.mt"

call moveComponent()
`
        );
        assertErrors(unrelated, ["'./generic/move-component.mt' uses the metamodel '../../metamodel/base.mm'"]);
    });

    it("rejects import cycles and self-imports", async () => {
        const workspace = new TestWorkspace(
            layeredProject({
                "/optimize/c1.mt": `using "../metamodel/base.mm"\nimport { c2 } from "./c2.mt"\n\ntransformation c1()\n\ncall c2()\n`,
                "/optimize/c2.mt": `using "../metamodel/base.mm"\nimport { c3 } from "./c3.mt"\n\ntransformation c2()\n\ncall c3()\n`,
                "/optimize/c3.mt": `using "../metamodel/base.mm"\nimport { c1 } from "./c1.mt"\n\ntransformation c3()\n\ncall c1()\n`,
                "/optimize/self.mt": `using "../metamodel/base.mm"\nimport { self } from "./self.mt"\n\ntransformation self()\n\ncall self()\n`
            })
        );
        assertErrors((await workspace.build("/optimize/c1.mt")).errors, [
            "Import cycle: c1.mt -> c2.mt -> c3.mt -> c1.mt. Recursive calls are not supported."
        ]);
        assertErrors((await workspace.build("/optimize/self.mt")).errors, [
            "A transformation cannot import itself; recursive calls are not supported."
        ]);
    });

    it("checks the parameters of a signature", async () => {
        const errors = await errorsFor(`using "../metamodel/shop.mm"

transformation t(
    in e: Component?,
    in f: Component = null,
    in many: List<Component>,
    in what: Unknown,
    out o: int = 1,
    in kind: Kind = Kind.SMALL,
    in ratio: double = 0.5,
    in label: string = "x",
    in flag: boolean = true,
    in big: long = 1
)
`);
        assertErrors(errors, [
            "The element input 'e' must not be nullable",
            "The element input 'f' cannot have a default value.",
            "Default value of type 'null' is not assignable to parameter 'f' of type 'Component'.",
            "Collection parameters are not supported yet; 'many' must be a single model element or a scalar.",
            "The type of 'what' must be a class or enum of the metamodel",
            "Only 'in' parameters can have a default value.",
            "The output 'o' is not assigned on every path",
            "Type 'Unknown' is not defined."
        ]);
    });

    it("rejects parameters, outputs and pattern objects of the same name", async () => {
        const errors = await errorsFor(`using "../metamodel/shop.mm"
import { moveComponent } from "./generic/move-component.mt"

transformation t(in x: int, out moved: Component?)

call moveComponent() -> (moved: x)
match {
    moved: Component { }
}
`);
        assertErrors(errors, [
            "Duplicate name 'x': parameter names must be globally unique.",
            "Duplicate name 'x': call output names must be globally unique.",
            "Duplicate name 'moved': parameter names must be globally unique.",
            "Duplicate name 'moved': object names must be globally unique."
        ]);
    });

    it("allows only bound model elements as nodes of patterns", async () => {
        const errors = await errorsFor(`using "../metamodel/shop.mm"
import { moveComponent } from "./generic/move-component.mt"

transformation t(in count: int, in host: Container, out made: Component?, out maybe: Container?)

call moveComponent() -> (moved: m, choices: n)
match {
    count { }
    made { }
    n { }
    m { }
    host { }
    m.host -- host
    maybe = host
}
`);
        assertErrors(errors, [
            "'count' is a scalar parameter, not a model element, so it cannot be a node of a pattern.",
            "'made' is an output parameter: it is not bound when the transformation starts.",
            "'n' is a scalar output, not a model element, so it cannot be a node of a pattern."
        ]);
    });

    it("rejects a nullable output as a node of a pattern and an assignment to an input", async () => {
        const errors = await errorsFor(
            `using "../metamodel/base.mm"
import { findNamed } from "./scenarios/find-named.mt"

transformation t(in host: Container)

call findNamed() -> (found: f, count: n)
match {
    f { }
    host = host
}
`,
            "/optimize/case.mt",
            SCENARIOS
        );
        assertErrors(errors, [
            "'f' may be null (its type is nullable), so it cannot be a node of a pattern.",
            "Cannot assign the input parameter 'host'; only 'out' parameters and variables can be assigned."
        ]);
    });

    it("reports outputs that are not assigned on every successful path", async () => {
        const errors = await errorsFor(`using "../metamodel/base.mm"

transformation t(out a: Container, out b: Container, out c: Container, out d: Container?)

match {
    x: Container { }
    a = x
}
if match {
    y: Container { }
} then {
    match {
        y { }
        b = y
        c = y
    }
} else {
    match {
        z: Container { }
        b = z
    }
}
`);
        assertErrors(errors, ["The output 'c' is not assigned on every path on which the transformation succeeds."]);
    });

    it("checks the outputs at every stop, but not after kill", async () => {
        const errors = await errorsFor(`using "../metamodel/base.mm"

transformation t(out a: Container)

if match {
    x: Container {
        name == "early"
    }
} then {
    stop
}
if match {
    y: Container {
        name == "bad"
    }
} then {
    kill
}
match {
    z: Container { }
    a = z
}
`);
        assertErrors(errors, ["The transformation returns here without assigning the output 'a'."]);
    });

    it("counts the assignments of until-match patterns and both branches of if-expressions", async () => {
        const errors = await errorsFor(`using "../metamodel/base.mm"

transformation t(in k: int = 0, out a: Container, out b: Container, out n: int)

until match {
    x: Container { }
    a = x
} do {
}
if (k > 1) {
    match {
        y: Container { }
        b = y
        var one = 1
        n = one
    }
} else if (k > 0) {
    match {
        y2: Container { }
        b = y2
        var two = 2
        n = two
    }
} else {
    match {
        y3: Container { }
        b = y3
    }
}
while match {
    w: Container {
        name == "never"
    }
} do {
}
`);
        assertErrors(errors, ["The output 'n' is not assigned on every path on which the transformation succeeds."]);
    });
});

describe("callable transformations: language server", () => {
    it("goes to the definition of a callee, its parameters and the outputs bound", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document, instance } = await workspace.build("/optimize/move-with-stores.mt");
        const definition = async (text: string, occurrence = 1) => {
            const links = await instance.services.lsp.DefinitionProvider.getDefinition(document, {
                textDocument: { uri: document.uri.toString() },
                position: positionOf(document, text, occurrence, 1)
            });
            return links?.[0];
        };

        const callee = await definition("moveComponent(");
        assert.ok(callee.targetUri.endsWith("/optimize/generic/move-component.mt"), callee.targetUri);
        assert.equal(callee.targetSelectionRange.start.line, 2);

        const argument = await definition("maxCandidates = 4");
        assert.ok(argument.targetUri.endsWith("/optimize/generic/move-component.mt"));
        assert.equal(argument.targetSelectionRange.start.line, 3);

        const followArgument = await definition("moved = m");
        assert.ok(followArgument.targetUri.endsWith("/optimize/sync/follow-reader.mt"));

        const output = await definition("m { }");
        assert.ok(output.targetUri.endsWith("/optimize/move-with-stores.mt"));
        assert.equal(output.targetSelectionRange.start.line, 4);
    });

    it("shows the signature of callees and the origin of outputs on hover", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document, instance } = await workspace.build("/optimize/move-with-stores.mt");
        const hover = async (text: string, occurrence = 1) => {
            const result = await instance.services.lsp.HoverProvider.getHoverContent(document, {
                textDocument: { uri: document.uri.toString() },
                position: positionOf(document, text, occurrence, 1)
            });
            return result?.contents?.value as string | undefined;
        };

        const callee = await hover("moveComponent(");
        assert.ok(
            callee?.includes(
                "transformation moveComponent(in maxCandidates: int = 0, out moved: Component, out source: Container, out target: Container, out choices: int)"
            ),
            callee
        );
        assert.ok(callee?.includes("/optimize/generic/move-component.mt"), callee);

        const imported = await hover("followReader }");
        assert.ok(
            imported?.includes("transformation followReader(in moved: Component, in target: Container)"),
            imported
        );

        const argument = await hover("maxCandidates = 4");
        assert.ok(argument?.includes("in maxCandidates: int = 0"), argument);
        assert.ok(argument?.includes("optional input of `moveComponent`"), argument);

        const output = await hover("m { }");
        assert.ok(output?.includes("m: Component"), output);
        assert.ok(output?.includes("output `moved` of `moveComponent`"), output);
    });

    it("offers the type of parameters and outputs to expressions", async () => {
        const workspace = new TestWorkspace(layeredProject());
        const { document, instance } = await workspace.build("/optimize/move-with-stores.mt");
        const result = await instance.services.lsp.HoverProvider.getHoverContent(document, {
            textDocument: { uri: document.uri.toString() },
            position: positionOf(document, "n >= 1", 1, 0)
        });
        assert.ok((result?.contents?.value as string).includes("n: int"), result?.contents?.value);
    });
});

describe("callable transformations: completion and run action", () => {
    it("completes imported callees and the callee's parameters", async () => {
        const text = `using "../metamodel/shop.mm"
import { moveComponent } from "./generic/move-component.mt"

call moveComponent()
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
        assert.ok((await complete(call + "call ".length)).includes("moveComponent"));
        const parameters = await complete(call + "call moveComponent(".length);
        assert.ok(parameters.includes("maxCandidates"), parameters.join(", "));
    });

    it("refuses to run a transformation with required inputs on its own", async () => {
        const { RunModelTransformationActionHandler } = await import("@mdeo/language-model-transformation");
        const workspace = new TestWorkspace(layeredProject());
        const { document, instance } = await workspace.build("/optimize/sync/follow-reader.mt");
        // the workbench's document builder knows the dependencies of a document; this one has none
        instance.services.shared.workspace.DocumentBuilder.getTransitiveDependencies = () => [];
        const handler = new RunModelTransformationActionHandler(instance.services.shared);
        const response: any = await handler.startAction({ data: { uri: document.uri.toString() } } as any);
        assert.equal(response.kind, "page");
        assert.equal(
            response.page.description,
            "'followReader' has required inputs 'moved', 'target', so it cannot run on its own. Call it from another transformation with 'call followReader(…)', or give these inputs defaults."
        );
    });
});

describe("callable transformations: helpers", () => {
    it("reports errors of documents built through the workspace", async () => {
        const workspace = new TestWorkspace(
            layeredProject({ "/optimize/broken.mt": `using "../metamodel/base.mm"\ncall (` })
        );
        const { document } = await workspace.build("/optimize/broken.mt");
        assert.ok(errorsOf(document).length > 0);
    });
});

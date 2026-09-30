import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { URI, type LangiumDocument } from "langium";
import type { Diagnostic } from "vscode-languageserver-types";
import { collectDiagnostics, describeValidationErrors, hasErrors } from "@mdeo/language-common";
import { diagnosticsHandler } from "../dist/handler/diagnosticsHandler.js";
import type { AstData } from "../dist/langium/jsonAstSerializer.js";
import type { FileDataContext } from "../dist/handler/types.js";

function diagnostic(line: number, message: string, severity?: 1 | 2 | 3 | 4, dataCode?: string): Diagnostic {
    return {
        range: { start: { line, character: 0 }, end: { line, character: 1 } },
        message,
        ...(severity != undefined && { severity }),
        ...(dataCode != undefined && { data: { code: dataCode } })
    };
}

function document(path: string, diagnostics: Diagnostic[] | undefined, parserErrors: unknown[] = []): LangiumDocument {
    return {
        uri: URI.file(path),
        parseResult: { lexerErrors: [], parserErrors, value: {} },
        diagnostics
    } as unknown as LangiumDocument;
}

function astData(path: string, hasErrors: boolean, hasTransitiveErrors: boolean, linkDependencies: string[] = []) {
    return { path, hasErrors, hasTransitiveErrors, linkDependencies } as AstData;
}

describe("document diagnostics", () => {
    it("counts errors but not warnings, information or hints", () => {
        const warnings = document("/a.script", [diagnostic(0, "w", 2), diagnostic(1, "i", 3), diagnostic(2, "h", 4)]);
        assert.equal(hasErrors(warnings), false);
        assert.equal(hasErrors(document("/a.script", [diagnostic(0, "e", 1)])), true);
        assert.equal(hasErrors(document("/a.script", [diagnostic(0, "no severity")])), true);
        assert.equal(hasErrors(document("/a.script", undefined, [{}])), true);
    });

    it("tells lexer, parser, linking and validation diagnostics apart", () => {
        const diagnostics = collectDiagnostics(
            document("/a.script", [
                diagnostic(0, "lex", 1, "lexing-error"),
                diagnostic(1, "parse", 1, "parsing-error"),
                diagnostic(2, "link", 1, "linking-error"),
                { ...diagnostic(3, "check", 2), code: "unused" }
            ])
        );
        assert.deepEqual(
            diagnostics.map(({ source, severity, message, code }) => ({ source, severity, message, code })),
            [
                { source: "lexer", severity: 1, message: "lex", code: undefined },
                { source: "parser", severity: 1, message: "parse", code: undefined },
                { source: "linking", severity: 1, message: "link", code: undefined },
                { source: "validation", severity: 2, message: "check", code: "unused" }
            ]
        );
    });

    it("names the first error of each file with errors", () => {
        const message = describeValidationErrors([
            document("/p/a.script", [
                diagnostic(2, "warn", 2),
                diagnostic(4, "Unknown type 'Bar'"),
                diagnostic(9, "x")
            ]),
            document("/p/b.script", [diagnostic(0, "only a warning", 2)]),
            document("/p/c.script", undefined, [{}])
        ]);
        assert.equal(
            message,
            "Cannot start execution. The following files have validation errors:\n" +
                "• a.script:5: Unknown type 'Bar' (+1 more)\n" +
                "• c.script"
        );
        assert.equal(describeValidationErrors([document("/p/b.script", [diagnostic(0, "w", 2)])]), undefined);
    });
});

describe("diagnostics handler", () => {
    it("reports the file's diagnostics and every dependency with errors", async () => {
        const main = document("/a.script", [diagnostic(0, "just a warning", 2)]);
        const documents = new Map<string, LangiumDocument>([
            ["/b.script", document("/b.script", [diagnostic(3, "broken")])],
            ["/c.script", document("/c.script", [])]
        ]);
        const remote = new Map<string, AstData>([
            ["/n.mm", astData("/n.mm", true, true)],
            ["/o.mm", astData("/o.mm", false, false)]
        ]);
        const cachedAsts = new Map([
            ["/m.mm", { data: astData("/m.mm", false, true, ["/n.mm", "/o.mm", "/gone.mm"]), version: 1 }]
        ]);
        const dataDependencies = [{ path: "/m.mm", key: "ast", version: 1 }];
        const serverApi = {
            getTrackedRequests: () => ({
                fileDependencies: [
                    { path: "/b.script", version: 1 },
                    { path: "/c.script", version: 1 }
                ],
                dataDependencies: [...dataDependencies]
            }),
            getFileDataByKey: (key: string) => (key === "ast" ? cachedAsts : new Map()),
            getFileData: async (path: string, key: string) => {
                const data = remote.get(path);
                if (data == undefined) {
                    throw new Error(`No file ${path}`);
                }
                dataDependencies.push({ path, key, version: 1 });
                return { data, version: 1 };
            }
        };
        const context = {
            fileInfo: { uri: main.uri, version: 1 },
            instance: { buildDocument: async () => main },
            services: {
                shared: { workspace: { LangiumDocuments: { getDocument: (uri: URI) => documents.get(uri.path) } } }
            },
            serverApi
        } as unknown as FileDataContext;

        const result = await diagnosticsHandler(context);

        assert.equal(result.data.path, "/a.script");
        assert.deepEqual(
            result.data.diagnostics.map((d) => d.message),
            ["just a warning"]
        );
        assert.deepEqual(result.data.dependenciesWithErrors, ["/b.script", "/n.mm"]);
        assert.deepEqual(
            result.dataDependencies.map((d) => d.path),
            ["/m.mm", "/n.mm", "/o.mm"]
        );
    });

    it("refuses directory requests", async () => {
        await assert.rejects(diagnosticsHandler({} as FileDataContext), /directory/);
    });
});

import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { TextDocument, URI, type LangiumDocument } from "langium";
import type { Diagnostic } from "vscode-languageserver-types";
import { initializePluginContext, type DocumentDiagnostic, type FileDataContext } from "@mdeo/service-common";
import type { ConfigPluginRequestBody } from "@mdeo/service-config-common";
import type { ConfigAdditionalServices } from "@mdeo/language-config";

initializePluginContext();
const { configDiagnosticsHandler } = await import("../dist/handler/configDiagnosticsHandler.js");

/**
 * A config with an optimization `problem` and `goal` section and an MDEO `search` section,
 * which depends on the problem section. The goal section is written with its qualified keyword.
 */
const CONFIG = `problem {
    metamodel = "./all.mm"
}

goal.optimization {
    import { weights } from "./w.fn"

    minimize weightz
}

search {
    mutations {
        create Boxx
    }
}
`;

const OPTIMIZATION = {
    type: "config-language-contribution",
    name: "optimization",
    languageKey: "config-optimization",
    sections: [{ name: "problem" }, { name: "goal" }],
    sectionDependencies: []
};

const MDEO = {
    type: "config-language-contribution",
    name: "mdeo",
    languageKey: "config-mdeo",
    sections: [{ name: "search" }, { name: "solver" }],
    sectionDependencies: [{ pluginName: "optimization", sectionName: "problem" }]
};

type Range = DocumentDiagnostic["range"];

/**
 * The range of the first occurrence of a word in a text.
 */
function rangeOf(text: string, word: string): Range {
    const document = TextDocument.create("file:///any", "any", 0, text);
    const offset = text.indexOf(word);
    assert.notEqual(offset, -1, `'${word}' is not in the text`);
    return { start: document.positionAt(offset), end: document.positionAt(offset + word.length) };
}

/**
 * A section of {@link CONFIG} as the config service parses it, named by its keyword there.
 */
function section(keyword: string, sectionName: string, pluginName: string) {
    const offset = CONFIG.indexOf(`${keyword} {`);
    const contentOffset = offset + keyword.length + 1;
    const contentEnd = CONFIG.indexOf("\n}", contentOffset) + 2;
    const type = `Config${sectionName.charAt(0).toUpperCase()}${sectionName.slice(1)}Section_${pluginName}`;
    return {
        $type: type,
        $cstNode: { offset },
        content: { $cstNode: { offset: contentOffset, text: CONFIG.slice(contentOffset, contentEnd) } }
    };
}

function configDocument(diagnostics: Diagnostic[], parserErrors: unknown[] = []): LangiumDocument {
    return {
        uri: URI.file("/d.config"),
        textDocument: TextDocument.create("file:///d.config", "config", 1, CONFIG),
        parseResult: {
            lexerErrors: [],
            parserErrors,
            value: {
                sections: [
                    section("problem", "problem", "optimization"),
                    section("goal.optimization", "goal", "optimization"),
                    section("search", "search", "mdeo")
                ]
            }
        },
        diagnostics
    } as unknown as LangiumDocument;
}

interface PluginRequest {
    languageKey: string;
    key: string;
    body: ConfigPluginRequestBody;
}

/**
 * Runs the handler on a config document, with plugins that answer as the given functions do.
 */
async function run(
    document: LangiumDocument,
    answer: (request: PluginRequest) => unknown
): Promise<{ result: Awaited<ReturnType<typeof configDiagnosticsHandler>>; requests: PluginRequest[] }> {
    const requests: PluginRequest[] = [];
    const context = {
        fileInfo: { uri: document.uri, version: 1 },
        instance: { buildDocument: async () => document },
        contributionPlugins: [MDEO, OPTIMIZATION],
        serverApi: {
            getTrackedRequests: () => ({ fileDependencies: [], dataDependencies: [] }),
            sendPluginRequest: async (languageKey: string, key: string, body: ConfigPluginRequestBody) => {
                const request = { languageKey, key, body };
                requests.push(request);
                return answer(request);
            }
        }
    } as unknown as FileDataContext<ConfigAdditionalServices>;
    return { result: await configDiagnosticsHandler(context), requests };
}

function pluginDiagnostic(message: string, range: Range): DocumentDiagnostic {
    return { source: "linking", severity: 1, message, range };
}

describe("config diagnostics handler", () => {
    const ownDiagnostics: Diagnostic[] = [
        { ...rangeOfDiagnostic("weightz"), message: "unresolved without the plugin", data: { code: "linking-error" } },
        { ...rangeOfDiagnostic("problem"), message: "a config check", severity: 2 }
    ];

    function rangeOfDiagnostic(word: string): Pick<Diagnostic, "range"> {
        return { range: rangeOf(CONFIG, word) };
    }

    it("reports the plugins' diagnostics of their sections instead of unresolved references", async () => {
        const { result, requests } = await run(configDocument(ownDiagnostics), ({ languageKey, key, body }) => {
            if (key === "config") {
                return {
                    data: { problem: { metamodelPath: "/all.mm" } },
                    fileDependencies: [{ path: "/all.mm", version: 1 }],
                    dataDependencies: []
                };
            }
            if (languageKey === "config-optimization") {
                return {
                    diagnostics: [
                        pluginDiagnostic("no function 'weightz'", rangeOf(body.text, "weightz")),
                        pluginDiagnostic("on a renamed keyword", rangeOf(body.text, "goal"))
                    ],
                    dependenciesWithErrors: ["/w.fn"],
                    fileDependencies: [],
                    dataDependencies: [{ path: "/w.fn", key: "ast", version: 2 }]
                };
            }
            return {
                diagnostics: [
                    pluginDiagnostic("no class 'Boxx'", rangeOf(body.text, "Boxx")),
                    pluginDiagnostic("on the keyword", rangeOf(body.text, "search"))
                ],
                dependenciesWithErrors: ["/all.mm"],
                fileDependencies: [],
                dataDependencies: []
            };
        });

        assert.deepEqual(
            requests.map(({ languageKey, key, body }) => [languageKey, key, body.text.split("\n")[0]]),
            [
                ["config-optimization", "config", "problem {"],
                ["config-optimization", "config-diagnostics", "problem {"],
                ["config-mdeo", "config-diagnostics", "search {"]
            ]
        );
        assert.equal(requests[0].body.text.includes("goal"), false);
        assert.deepEqual(requests[2].body.dependencyData, { optimization: { problem: { metamodelPath: "/all.mm" } } });

        assert.deepEqual(
            result.data.diagnostics.map(({ message, range }) => [message, range]),
            [
                ["a config check", rangeOf(CONFIG, "problem")],
                ["on a renamed keyword", rangeOf(CONFIG, "goal.optimization")],
                ["no function 'weightz'", rangeOf(CONFIG, "weightz")],
                ["on the keyword", rangeOf(CONFIG, "search")],
                ["no class 'Boxx'", rangeOf(CONFIG, "Boxx")]
            ]
        );
        assert.deepEqual(result.data.dependenciesWithErrors, ["/all.mm", "/w.fn"]);
        assert.deepEqual(
            [...result.fileDependencies.map((d) => d.path), ...result.dataDependencies.map((d) => d.path)],
            ["/all.mm", "/w.fn"]
        );
    });

    it("does not ask a plugin whose section dependencies have errors", async () => {
        const { result, requests } = await run(configDocument([]), ({ key, body }) =>
            key === "config"
                ? { data: null, fileDependencies: [], dataDependencies: [] }
                : {
                      diagnostics: [pluginDiagnostic("broken problem", rangeOf(body.text, "metamodel"))],
                      dependenciesWithErrors: [],
                      fileDependencies: [],
                      dataDependencies: []
                  }
        );

        assert.deepEqual(
            requests.map(({ languageKey, key }) => [languageKey, key]),
            [
                ["config-optimization", "config"],
                ["config-optimization", "config-diagnostics"]
            ]
        );
        assert.deepEqual(
            result.data.diagnostics.map(({ message, range }) => [message, range]),
            [["broken problem", rangeOf(CONFIG, "metamodel")]]
        );
    });

    it("asks no plugin about a config with parser errors", async () => {
        const { result, requests } = await run(configDocument(ownDiagnostics, [{}]), () => {
            throw new Error("not to be asked");
        });

        assert.equal(requests.length, 0);
        assert.deepEqual(
            result.data.diagnostics.map(({ message }) => message),
            ["a config check"]
        );
    });
});

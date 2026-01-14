import { initializePluginContext as initializePluginContextInternal } from "@mdeo/language-common";
import * as langium from "langium";
import * as langiumLsp from "langium/lsp";
import * as langiumGrammar from "langium/grammar";
import * as typirLangium from "typir-langium";
import * as typir from "typir";
import * as prettier from "prettier";
import * as glspServer from "@eclipse-glsp/server";
import * as glspServerBrowser from "@eclipse-glsp/server/browser.js";
import * as glspProtocol from "@eclipse-glsp/protocol";
import * as glspGraph from "@eclipse-glsp/graph";
import * as inversify from "inversify";
import * as vscodeJsonrpc from "vscode-jsonrpc";
import * as vscodeLanguageserverTypes from "vscode-languageserver-types";

/**
 * Initializes the plugin context with required dependencies.
 */
export function initializePluginContext(): void {
    initializePluginContextInternal({
        langium,
        "langium/lsp": langiumLsp,
        "langium/grammar": langiumGrammar,
        "typir-langium": typirLangium,
        typir,
        prettier,
        "@eclipse-glsp/server": glspServer,
        "@eclipse-glsp/server/browser.js": glspServerBrowser,
        "@eclipse-glsp/protocol": glspProtocol,
        "@eclipse-glsp/graph": glspGraph,
        inversify,
        "vscode-jsonrpc": vscodeJsonrpc,
        "vscode-languageserver-types": vscodeLanguageserverTypes
    });
}

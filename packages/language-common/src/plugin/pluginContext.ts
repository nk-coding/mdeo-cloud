import type * as langium from "langium";
import type * as langiumLsp from "langium/lsp";
import type * as langiumGrammar from "langium/grammar";
import type * as typirLangium from "typir-langium";
import type * as typir from "typir";
import type * as prettier from "prettier";
import type * as glspServer from "@eclipse-glsp/server";

/**
 * Context provided to plugins when they are initialized
 */
export interface PluginContext {
    langium: typeof langium;
    "langium/lsp": typeof langiumLsp;
    "langium/grammar": typeof langiumGrammar;
    "typir-langium": typeof typirLangium;
    typir: typeof typir;
    prettier: typeof prettier;
    "@eclipse-glsp/server": typeof glspServer;
}

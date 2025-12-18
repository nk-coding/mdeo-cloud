import type * as langium from "langium";
import type * as langiumLsp from "langium/lsp";
import type * as langiumGrammar from "langium/grammar";

/**
 * Context provided to plugins when they are initialized
 */
export interface PluginContext {
    langium: typeof langium;
    "langium/lsp": typeof langiumLsp;
    "langium/grammar": typeof langiumGrammar;
}

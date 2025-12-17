import type * as langium from "langium";
import type * as langiumLsp from "langium/lsp";
import type * as mdeoLanguageCommon from "../index.js";

/**
 * Context provided to plugins when they are initialized
 */
export interface PluginContext {
    langium: typeof langium;
    "langium/lsp": typeof langiumLsp;
    "@mdeo/language-common": typeof mdeoLanguageCommon;
}

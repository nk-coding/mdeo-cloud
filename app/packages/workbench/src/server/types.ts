import type { ServerPlugin } from "@/data/plugin/serverPlugin";
import type { LangiumLanguagePlugin } from "@mdeo/language-common";
import type { LangiumServices } from "langium/lsp";

/**
 * Resolved server language plugin with its language plugin and generated services
 */
export interface ResolvedServerLanguagePlugin extends ServerPlugin {
    /**
     * The language plugin definition
     */
    languagePlugin: LangiumLanguagePlugin<any>;
    /**
     * The langium servivces, when generated
     */
    services?: LangiumServices;
}

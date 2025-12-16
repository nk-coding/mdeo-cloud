import type { ServerLanguagePlugin } from "@/data/plugin/serverPlugin";
import type { LanguagePlugin } from "@mdeo/language-common";
import type { LangiumServices } from "langium/lsp";

export interface ResolvedServerLanguagePlugin extends ServerLanguagePlugin {
    /**
     * The language plugin definition
     */
    languagePlugin: LanguagePlugin<any>;
    /**
     * The langium servivces, when generated
     */
    services?: LangiumServices;
}

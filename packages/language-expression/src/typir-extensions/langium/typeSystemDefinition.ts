import type { LangiumTypeSystemDefinition, TypirLangiumSpecifics } from "typir-langium";
import type { Scope } from "../scope/scope.js";

/**
 * Extended TypeSystemDefinition interface that includes a global scope.
 */
export interface ExtendedTypeSystemDefinition<Specifics extends TypirLangiumSpecifics>
    extends LangiumTypeSystemDefinition<Specifics> {
    /**
     * The global scope containing all top-level declarations.
     */
    globalScope: Scope<Specifics>;
}

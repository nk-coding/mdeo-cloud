import {
    DefaultScope,
    type BoundScope,
    type ControlFlowEntry,
    type Scope,
    type ScopeEntry,
    type ScopeLocalInitialization
} from "@mdeo/language-expression";
import type { TypirLangiumSpecifics } from "typir-langium";

/**
 * A scope for the elements of a `forbid` / `require` block.
 *
 * A block declares names — its own nodes — that are visible inside the block and nowhere
 * else, which is what a scope of its own gives us. It is however not a scope at *run time*:
 * the block is matched as part of the enclosing match, and its nodes are bound by the same
 * traversal, only inside a nested sub-traversal of it.
 *
 * The distinction matters because the level of the scope an identifier resolves in is
 * recorded in the typed AST and used by the execution engine to look up the variable scope
 * that holds the binding. Reporting a level of its own would send the engine looking for a
 * run-time scope that a condition block never opens, so the block reports the level of its
 * parent instead.
 */
export class ModelTransformationApplicationConditionScope extends DefaultScope<TypirLangiumSpecifics> {
    /**
     * The parent scope, kept because {@link DefaultScope} holds it privately.
     */
    private readonly parentScope: BoundScope<TypirLangiumSpecifics> | undefined;

    /**
     * Creates a new application condition scope.
     *
     * @param parent The scope enclosing the condition block.
     * @param entriesProvider Function that provides the scope entries (the block's nodes).
     * @param controlFlowEntriesProvider Function that provides control flow entries.
     * @param localInitializations Initial values for locally initialized entries.
     * @param languageNode The condition block AST node.
     */
    constructor(
        parent: BoundScope<TypirLangiumSpecifics> | undefined,
        entriesProvider: (scope: Scope<TypirLangiumSpecifics>) => ScopeEntry<TypirLangiumSpecifics>[],
        controlFlowEntriesProvider: (scope: Scope<TypirLangiumSpecifics>) => ControlFlowEntry<TypirLangiumSpecifics>[],
        localInitializations: ScopeLocalInitialization[],
        languageNode: TypirLangiumSpecifics["LanguageType"] | undefined
    ) {
        super(parent, entriesProvider, controlFlowEntriesProvider, localInitializations, languageNode);
        this.parentScope = parent;
    }

    /**
     * The level of the enclosing scope: a condition block adds names, not a run-time scope.
     */
    override get level(): number {
        return this.parentScope != undefined ? this.parentScope.scope.level : 0;
    }
}

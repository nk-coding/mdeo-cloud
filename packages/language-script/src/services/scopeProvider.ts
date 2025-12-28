import {
    DefaultScope,
    IterableType,
    StatementsScopeProvider,
    type BoundScope,
    type ExpressionTypirServices,
    type Scope,
    type ScopeEntry
} from "@mdeo/language-expression";
import type { ScriptTypirSpecifics } from "../plugin.js";
import {
    expressionTypes,
    Function,
    Script,
    statementTypes,
    type FunctionType,
    type ScriptType
} from "../grammar/types.js";

/**
 * The scope provider for the Script language.
 * Adds support for function scopes, and the document scope
 */
export class ScriptScopeProvider extends StatementsScopeProvider<ScriptTypirSpecifics> {
    constructor(typir: ExpressionTypirServices<ScriptTypirSpecifics>) {
        super(typir, statementTypes, expressionTypes, IterableType);
    }

    override isScopeRelevantNode(node: ScriptTypirSpecifics["LanguageType"]): boolean {
        return (
            super.isScopeRelevantNode(node) ||
            this.reflection.isInstance(node, Function) ||
            this.reflection.isInstance(node, Script)
        );
    }

    override createScope(
        languageNode: ScriptTypirSpecifics["LanguageType"],
        parentScope: BoundScope<ScriptTypirSpecifics> | undefined
    ): Scope<ScriptTypirSpecifics> {
        if (this.reflection.isInstance(languageNode, Function)) {
            return this.createScopeForFunctionNode(languageNode, parentScope);
        } else if (this.reflection.isInstance(languageNode, Script)) {
            return this.createScopeForScriptNode(languageNode, parentScope);
        }
        return super.createScope(languageNode, parentScope);
    }

    /**
     * Creates a scope for a function node.
     * Function scopes include the function's parameters as scope entries.
     *
     * @param node The function node to create a scope for
     * @param parentScope The parent scope of this function scope
     * @returns A new scope containing the function's parameters
     */
    private createScopeForFunctionNode(
        node: FunctionType,
        parentScope: BoundScope<ScriptTypirSpecifics> | undefined
    ): Scope<ScriptTypirSpecifics> {
        return new DefaultScope<ScriptTypirSpecifics>(
            parentScope,
            (scope) => this.getFunctionScopeEntries(node, scope),
            () => [],
            node.parameters.map((param) => ({
                name: param.name,
                position: -1
            })),
            node
        );
    }

    /**
     * Retrieves the scope entries for a function node.
     * Each parameter of the function becomes a scope entry with its name, type, and defining scope.
     *
     * @param node The function node to get scope entries from
     * @param scope The scope that these entries belong to
     * @returns An array of scope entries, one for each function parameter
     */
    private getFunctionScopeEntries(
        node: FunctionType,
        scope: Scope<ScriptTypirSpecifics>
    ): ScopeEntry<ScriptTypirSpecifics>[] {
        return node.parameters.map((param) => ({
            name: param.name,
            position: -1,
            languageNode: param,
            definingScope: scope,
            inferType: () => this.inference.inferType(param.type)
        }));
    }

    /**
     * Creates a scope for a script node.
     * Script scopes include all top-level functions defined in the script as scope entries.
     *
     * @param node The script node to create a scope for
     * @param parentScope The parent scope of this script scope
     * @returns A new scope containing the script's functions
     */
    private createScopeForScriptNode(
        node: ScriptType,
        parentScope: BoundScope<ScriptTypirSpecifics> | undefined
    ): Scope<ScriptTypirSpecifics> {
        return new DefaultScope<ScriptTypirSpecifics>(
            parentScope,
            (scope) => this.getScriptScopeEntries(node, scope),
            () => [],
            node.functions.map((func) => ({
                name: func.name,
                position: -1
            })),
            node
        );
    }

    /**
     * Retrieves the scope entries for a script node.
     * Each function defined in the script becomes a scope entry with its name, type, and defining scope.
     *
     * @param node The script node to get scope entries from
     * @param scope The scope that these entries belong to
     * @returns An array of scope entries, one for each function in the script
     */
    private getScriptScopeEntries(
        node: ScriptType,
        scope: Scope<ScriptTypirSpecifics>
    ): ScopeEntry<ScriptTypirSpecifics>[] {
        return node.functions.map((func) => ({
            name: func.name,
            position: -1,
            languageNode: func,
            definingScope: scope,
            inferType: () => this.inference.inferType(func)
        }));
    }
}

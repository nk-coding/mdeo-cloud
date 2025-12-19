import type { AstNode, Grammar, AstReflection as LangiumAstReflection } from "langium";
import type { LanguagePlugin } from "../../plugin/languagePlugin.js";
import type { Interface } from "../type/interface/types.js";

/**
 * Provides both the grammar and AstReflection for several langium language, which would
 * typically be handled via code generation.
 */

export interface LanguageModule {
    /**
     * The compiled Langium grammar that defines the language syntax and parsing rules.
     * This grammar can be used to create parsers and other language services.
     */
    grammars: Map<LanguagePlugin<any>, Grammar>;

    /**
     * AST reflection metadata that provides runtime type information about
     * the AST node types defined in the grammar. This enables features like
     * type checking, validation, and code completion.
     */
    reflection: AstReflection;
}

export interface AstReflection extends LangiumAstReflection {
    /**
     * Checks whether the given AST node is an instance of the specified type.
     * 
     * @param node The AST node to check
     * @param type The type to check against, either as a string name or an Interface type
     * @returns True if the node is an instance of the specified type, false otherwise
     */
    isInstance(node: unknown, type: string): boolean;
    isInstance<T extends AstNode>(node: unknown, type: Interface<T>): node is T;
}

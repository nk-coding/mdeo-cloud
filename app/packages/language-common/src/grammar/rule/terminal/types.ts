import type { GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { SerializableExternalReference } from "../../serialization/grammarSerializer.js";

/**
 * Represents a terminal rule in the grammar that matches specific text patterns
 * and produces typed values. Terminal rules are the lowest-level parsing units
 * that match against the input text using regular expressions or literal strings.
 *
 * @template T The TypeScript type that this terminal rule produces
 */
export interface TerminalRule<T> {
    /**
     * Optional TypeScript type information for the value this terminal produces.
     * Used for type inference and validation during compilation.
     */
    tsType?: T;

    /**     * The name of the terminal rule as it appears in the grammar.
     */
    name: string;

    /**     * Converts this terminal rule into a serializable grammar node that can be used
     * in the Langium grammar generation process.
     *
     * @returns A serializable representation of the terminal rule or external reference
     */
    toRule: () => SerializableGrammarNode<GrammarAST.TerminalRule> | SerializableExternalReference;
}

/**
 * Type guard to check if a value is a terminal rule.
 *
 * @param node The value to check
 * @returns True if the node is a terminal rule, false otherwise
 */
export function isTerminalRule(node: unknown): node is TerminalRule<any> {
    return typeof node === "object" && node !== null && "$type" in node && node.$type === "TerminalRule";
}

import type { GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../serialization/types.js";
import type { SerializableExternalReference } from "../serialization/grammarSerializer.js";

/**
 * Represents a parser rule that can generate either a regular parser rule or an infix rule.
 *
 * Parser rules define how to parse specific AST node types from input text. They contain
 * the logic to transform matched tokens into structured AST nodes.
 *
 * @template T The AST node type that this parser rule produces
 */
export type ParserRule<T> = {
    /**
     * TypeScript type information for the AST node this rule produces.
     * Only used for type inference and validation during compilation,
     * never assigned at runtime.
     */
    tsType?: T;

    /**
     * Converts this parser rule into a serializable grammar node that can be used
     * in the Langium grammar generation process.
     *
     * @returns A serializable representation of either a ParserRule, InfixRule, or external reference
     */
    toRule: () =>
        | SerializableGrammarNode<GrammarAST.ParserRule>
        | SerializableGrammarNode<GrammarAST.InfixRule>
        | SerializableExternalReference;
};

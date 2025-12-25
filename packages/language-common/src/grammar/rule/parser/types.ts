import type { AstNode, GrammarAST, Reference } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { TerminalRule } from "../terminal/types.js";
import type { ParserRule } from "../types.js";

/**
 * Union type representing all possible abstract elements that can appear
 * in a grammar rule definition. These elements define the structure and
 * parsing behavior of language constructs.
 */
export type AbstractElement =
    | SerializableGrammarNode<GrammarAST.Group>
    | SerializableGrammarNode<GrammarAST.Assignment>
    | SerializableGrammarNode<GrammarAST.EndOfFile>
    | SerializableGrammarNode<GrammarAST.Keyword>
    | SerializableGrammarNode<GrammarAST.RuleCall>
    | SerializableGrammarNode<GrammarAST.Alternatives>
    | SerializableGrammarNode<GrammarAST.UnorderedGroup>
    | SerializableGrammarNode<GrammarAST.CrossReference>
    | SerializableGrammarNode<GrammarAST.Action>;

/**
 * Union type representing all possible entries that can be used when defining
 * a parser rule. These are converted to AbstractElements during rule compilation.
 */
export type RuleEntry =
    | AbstractElement
    | string
    | TerminalRule<any>
    | ParserRule<any>
    | (() => ParserRule<any>)
    | CrossRef<any>;

/**
 * Represents a cross-reference to another AST node type in the grammar.
 * Cross-references allow one part of the AST to reference another part,
 * enabling relationships between different language constructs.
 *
 * @template T The AST node type being referenced
 */
export type CrossRef<T extends AstNode> = {
    /**
     * Optional TypeScript type information for the AST node being referenced.
     * Used for type inference and validation during compilation.
     */
    tsType?: T;
} & SerializableGrammarNode<GrammarAST.CrossReference>;

/**
 * Utility type that extracts property names from type T that have boolean values.
 * Used for identifying which properties can be used with the flag assignment operator.
 *
 * @template T The type to extract boolean keys from
 */
type BooleanKeys<T> = {
    [K in keyof T]: T[K] extends boolean ? K : never;
}[keyof T];

/**
 * Utility type that extracts property names from type T that have array values.
 * Used for identifying which properties can be used with the add assignment operator (+=).
 *
 * @template T The type to extract array keys from
 */
type ArrayKeys<T> = {
    [K in keyof T]: T[K] extends Array<any> ? K : never;
}[keyof T];

/**
 * Utility type that excludes array properties from type T.
 * Used for identifying which properties can be used with the set assignment operator (=).
 *
 * @template T The type to extract non-array keys from
 */
type NonArrayKeys<T> = Exclude<keyof T, ArrayKeys<T>>;

/**
 * Context object provided to rule definition functions. Provides methods for
 * creating assignments that populate AST node properties during parsing.
 *
 * @template T The AST node type that this rule context is for
 *
 * @example
 * ```typescript
 * const PersonRule = createRule("Person")
 *     .returns(PersonInterface)
 *     .as(({ set, add, flag }) => [
 *         set("name", STRING_TERMINAL),
 *         set("age", NUMBER_TERMINAL),
 *         add("hobbies", HOBBY_TERMINAL),
 *         flag("isActive", "active")
 *     ]);
 * ```
 */
export interface RuleContext<T extends AstNode> {
    /**
     * Creates an assignment that sets a single value to a property.
     * Used for non-array properties that should be assigned exactly once.
     *
     * @template K The property name being assigned
     * @param key The name of the property to assign
     * @param value The terminal, parser rule, cross-reference, or literal values to assign
     * @returns An assignment element for the grammar
     *
     * @example
     * ```typescript
     * set("name", STRING_TERMINAL)        // Assign from terminal
     * set("child", ChildRule)             // Assign from parser rule
     * set("parent", ref(ParentType))      // Assign cross-reference
     * set("type", "A", "B", "C")          // Assign one of several literals
     * ```
     */
    set<K extends NonArrayKeys<Omit<T, keyof AstNode>>>(
        key: K & string,
        ...value:
            | [TerminalRule<T[K]>]
            | (NonNullable<T[K]> extends AstNode
                  ? [ParserRule<NonNullable<T[K]>> | (() => ParserRule<NonNullable<T[K]>>)]
                  : never)
            | (NonNullable<T[K]> extends Reference<infer U extends AstNode> ? [CrossRef<U>] : never)
            | (NonNullable<T[K]> extends string ? T[K][] : never)
    ): AbstractElement;

    /**
     * Creates an assignment that adds values to an array property.
     * Used for array properties that can accumulate multiple values.
     *
     * @template K The array property name being assigned
     * @param key The name of the array property to add to
     * @param value The terminal, parser rule, cross-reference, or literal values to add
     * @returns An assignment element for the grammar
     *
     * @example
     * ```typescript
     * add("tags", TAG_TERMINAL)           // Add to array from terminal
     * add("children", ChildRule)          // Add to array from parser rule
     * add("references", ref(RefType))     // Add cross-reference to array
     * add("types", "option1", "option2")  // Add literals to string array
     * ```
     */
    add<K extends ArrayKeys<Omit<T, keyof AstNode>>>(
        key: K & string,
        ...value:
            | (T[K] extends (infer U)[] ? [TerminalRule<U>] : never)
            | (T[K] extends (infer U extends AstNode)[] ? [ParserRule<U> | (() => ParserRule<U>)] : never)
            | (T[K] extends Reference<infer U extends AstNode>[] ? [CrossRef<U>] : never)
            | (T[K] extends string[] ? T[K] : never)
    ): AbstractElement;

    /**
     * Creates a flag assignment that sets a boolean property to true when
     * specific rule entries are matched. Used for optional boolean flags.
     *
     * @param key The name of the boolean property to flag
     * @param entries The rule entries that, when matched, set the flag to true
     * @returns An assignment element for the grammar
     *
     * @example
     * ```typescript
     * flag("isPublic", "public")          // Set true when "public" keyword found
     * ```
     */
    flag(key: BooleanKeys<Omit<T, keyof AstNode>> & string, ...entries: RuleEntry[]): AbstractElement;
}

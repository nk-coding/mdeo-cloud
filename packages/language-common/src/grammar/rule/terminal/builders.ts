import { GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import { type Primitive } from "../../type/types.js";
import type { MapPrimitive } from "../../type/interface/types.js";
import type { TerminalRule } from "./types.js";
import { primitiveTypeLookup } from "../../type/primitiveTypeLookup.js";

/**
 * Builder for creating terminal rules with fluent interface.
 * Allows configuring various aspects of terminal rules like visibility,
 * return type, and matching pattern.
 *
 * @template T The TypeScript type that this terminal rule will produce
 */
export class TerminalRuleBuilder<T> {
    /**
     * Whether this terminal rule should be hidden from the concrete syntax tree.
     * Hidden terminals are used for things like whitespace and comments that
     * affect parsing but don't appear in the final AST.
     */
    private isHidden: boolean = false;

    /**
     * The primitive type that this terminal rule returns.
     * Used to convert matched text into typed values.
     */
    private type?: GrammarAST.PrimitiveType;

    /**
     * Creates a new terminal rule builder.
     *
     * @param name The unique name for this terminal rule in the grammar
     */
    constructor(private readonly name: string) {}

    /**
     * Marks this terminal rule as hidden. Hidden terminals are not included
     * in the concrete syntax tree but are still used for parsing (e.g., whitespace).
     *
     * @returns The same builder instance for method chaining
     *
     * @example
     * ```typescript
     * const WS = createTerminal("WS")
     *     .hidden()
     *     .as(/\s+/);
     * ```
     */
    hidden(): TerminalRuleBuilder<T> {
        this.isHidden = true;
        return this;
    }

    /**
     * Specifies the TypeScript type that this terminal rule should return.
     * This enables type-safe parsing where matched text is converted to the
     * appropriate TypeScript type.
     *
     * @template T The primitive type to return
     * @param type The primitive type constructor (String, Number, Boolean, BigInt, Date)
     * @returns A new builder with updated type information
     *
     * @example
     * ```typescript
     * const INT = createTerminal("INT")
     *     .returns(Number)  // Convert matched text to number
     *     .as(/[0-9]+/);
     *
     * const BOOL = createTerminal("BOOL")
     *     .returns(Boolean)  // Convert "true"/"false" to boolean
     *     .as(/true|false/);
     * ```
     */
    returns<T extends Primitive>(type: T): TerminalRuleBuilder<MapPrimitive<T>> {
        this.type = primitiveTypeLookup.get(type);
        return this as TerminalRuleBuilder<MapPrimitive<T>>;
    }

    /**
     * Defines the matching pattern for this terminal rule using a regular expression.
     * This completes the terminal rule definition.
     *
     * @param regex The regular expression
     * @returns A complete terminal rule that can be used in grammar definitions
     *
     * @example
     * ```typescript
     * const ID = createTerminal("ID").as(/[a-zA-Z_][a-zA-Z0-9_]*\/);
     * ```
     */
    as(regex: RegExp | string): TerminalRule<T> {
        const token: SerializableGrammarNode<GrammarAST.RegexToken> = {
            $type: "RegexToken",
            parenthesized: false,
            regex: regex.toString()
        };
        return {
            $type: "TerminalRule",
            name: this.name,
            hidden: this.isHidden,
            type:
                this.type != undefined
                    ? {
                          $type: "ReturnType",
                          name: this.type
                      }
                    : undefined,
            fragment: false,
            definition: token
        };
    }
}

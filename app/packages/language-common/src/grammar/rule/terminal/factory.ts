import type { SerializableExternalReference } from "../../../index.js";
import { TerminalRuleBuilder } from "./builders.js";
import type { TerminalRule } from "./types.js";

/**
 * Creates a new terminal rule builder for defining terminal grammar rules.
 *
 * Terminal rules define how to match and parse basic tokens from the input text.
 * They are the foundation of the grammar, matching things like identifiers,
 * numbers, strings, and keywords using regular expressions.
 *
 * @param name The unique name for this terminal rule in the grammar
 * @returns A new TerminalRuleBuilder instance for configuring the terminal rule
 */
export function createTerminal(name: string): TerminalRuleBuilder<string> {
    return new TerminalRuleBuilder(name);
}

/**
 * Creates an external reference to a terminal rule from another grammar.
 * Used during language composition to reference terminal rules defined in other partial languages.
 *
 * @template T The TypeScript type that the referenced terminal rule produces
 * @param name The name of the external terminal rule being referenced
 * @returns A terminal rule external reference
 *
 * @example
 * ```typescript
 * // Reference a STRING terminal from another grammar
 * const ExternalString = createExternalTerminalRule<string>("STRING");
 * ```
 */
export function createExternalTerminalRule<T>(name: string): TerminalRule<T> {
    const externalRef: SerializableExternalReference = {
        $externalRef: true,
        kind: "TerminalRule",
        name
    };

    return {
        name,
        toRule: () => externalRef
    };
}

import { TerminalRuleBuilder } from "./builders.js";

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

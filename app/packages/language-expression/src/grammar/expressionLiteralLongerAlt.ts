import type { TokenType } from "chevrotain";
import { DOUBLE_LITERAL, FLOAT_LITERAL, INT_LITERAL, LONG_LITERAL } from "./expressionRule.js";

/**
 * Checks whether the given terminal token name is an expression literal token
 * that requires a `LONGER_ALT` configuration in the Chevrotain grammar.
 *
 * The affected tokens are:
 * - `INT_LITERAL` – `[0-9]+` is a prefix of LONG, FLOAT, and DOUBLE literals.
 * - `DOUBLE_LITERAL` – `[0-9]+\.[0-9]+` (without mandatory suffix) is a prefix of FLOAT_LITERAL.
 *
 * @param tokenName The Chevrotain token name to check.
 */
export function needsExpressionLiteralLongerAlt(tokenName: string): boolean {
    return tokenName === INT_LITERAL.name || tokenName === DOUBLE_LITERAL.name;
}

/**
 * Computes the longer-alternative tokens that Chevrotain should prefer over
 * `tokenName` whenever both could match at the same input position.
 *
 * - `INT_LITERAL` → `[LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL]`
 * - `DOUBLE_LITERAL` → `[FLOAT_LITERAL]`
 *
 * @param tokenName The name of the token that needs longer alternatives.
 * @param allTokens The full flat list of token types in the vocabulary.
 * @returns The subset of `allTokens` that are longer alternatives, in order.
 */
export function computeExpressionLiteralLongerAlts(tokenName: string, allTokens: readonly TokenType[]): TokenType[] {
    if (tokenName === INT_LITERAL.name) {
        return allTokens.filter(
            (t) => t.name === LONG_LITERAL.name || t.name === FLOAT_LITERAL.name || t.name === DOUBLE_LITERAL.name
        );
    }
    if (tokenName === DOUBLE_LITERAL.name) {
        return allTokens.filter((t) => t.name === FLOAT_LITERAL.name);
    }
    return [];
}

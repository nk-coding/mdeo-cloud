import type { GrammarAST, Stream } from "langium";
import type { TokenType } from "chevrotain";
import { NewlineAwareTokenBuilder } from "@mdeo/language-shared";
import { computeExpressionLiteralLongerAlts, needsExpressionLiteralLongerAlt } from "@mdeo/language-expression";

/**
 * Token builder for the Script language.
 *
 * Extends {@link NewlineAwareTokenBuilder} to add the `LONGER_ALT` relationships
 * required by the numeric literal terminals that are inherited from the expression
 * language (INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL).
 * Without these relationships Chevrotain can incorrectly prefer the shorter token
 * when a longer one is available at the same input position.
 */
export class ScriptTokenBuilder extends NewlineAwareTokenBuilder {
    protected override buildTerminalTokens(rules: Stream<GrammarAST.AbstractRule>): TokenType[] {
        const allTokens = super.buildTerminalTokens(rules);
        for (const token of allTokens) {
            if (needsExpressionLiteralLongerAlt(token.name)) {
                const longerAlts = computeExpressionLiteralLongerAlts(token.name, allTokens);
                if (longerAlts.length > 0) {
                    token.LONGER_ALT = longerAlts;
                }
            }
        }
        return allTokens;
    }
}

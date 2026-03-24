import type { RuleOverride, RuleResult } from "@mdeo/language-shared";
import type { ExpressionConfig } from "../grammar/expressionConfig.js";
import { ID } from "@mdeo/language-common";
import type { GrammarAST } from "langium";

/**
 * Generates the rule override function for expression rules.
 * Handles edge cases regarding generic argument call parsing.
 * In general, a GATE ensures that the call only matches when generic arguments are correct.
 * This resolves ambiguities regarding the '<' and '>' binary operators.
 *
 * @param config The expression configuration
 * @returns the rule override function
 */
export function generateExpressionRuleOverride(config: ExpressionConfig): RuleOverride {
    return (rule, impl, parser) => {
        if (rule.name !== config.postfixExpressionRuleName) {
            return impl;
        }
        if (rule.$type !== "ParserRule") {
            throw new Error(`${rule.name} must be a ParserRule.`);
        }
        const definition = rule.definition;
        if (!("elements" in definition) || !Array.isArray(definition.elements)) {
            throw new Error(`${rule.name} definition must be a Group.`);
        }
        const elements = (definition as GrammarAST.Group).elements;
        let baseExpressionRule: RuleResult;
        let memberAccessPostfixFragmentRule: RuleResult;
        let callPostfixFragmentRule: RuleResult;
        let memberCallPostfixFragmentRule: RuleResult;
        let assertNonNullPostfixFragmentRule: RuleResult;

        /**
         * Returns the token at lookahead position `i` from the Chevrotain parser.
         *
         * @param i The lookahead distance (1 = next token)
         * @returns The token at position `i`
         */
        const lookahead = (i: number) => {
            // @ts-expect-error protected access
            return parser.chevrotainParser.LA(i);
        };

        /**
         * Determines if, starting from offset `baseOffset`, the token sequence matches
         * a generic call pattern: optionally `<id...>` then `(`.
         * Returns true if the next tokens look like a generic or regular call.
         *
         * @param baseOffset The lookahead offset from which to start scanning
         * @returns `true` if the upcoming tokens form a valid call expression
         */
        const isCallAhead = (baseOffset: number): boolean => {
            const first = lookahead(baseOffset);
            if (first.image === "(") {
                return true;
            }
            if (first.image !== "<") {
                return false;
            }
            let depth = 1;
            for (let i = baseOffset + 1; ; i++) {
                const la = lookahead(i);
                if (la.tokenType.name === ID.name) {
                    continue;
                }
                if (la.image === "<") {
                    depth++;
                } else if (la.image === ">") {
                    depth--;
                    if (depth === 0) {
                        const next = lookahead(i + 1);
                        return next.image === "(";
                    }
                } else {
                    if (!/^\(|\)|\?|=>|,$/.test(la.image)) {
                        return false;
                    }
                }
            }
        };
        const fragmentRuleCalls = (elements[1] as GrammarAST.Alternatives).elements as GrammarAST.RuleCall[];

        return (args) => {
            baseExpressionRule ??= parser.getRule(config.primaryExpressionRuleName)!;
            memberAccessPostfixFragmentRule ??= parser.getRule(config.memberAccessPostfixFragmentRuleName)!;
            callPostfixFragmentRule ??= parser.getRule(config.callPostfixFragmentRuleName)!;
            memberCallPostfixFragmentRule ??= parser.getRule(config.memberCallPostfixFragmentRuleName)!;
            assertNonNullPostfixFragmentRule ??= parser.getRule(config.assertNonNullPostfixFragmentRuleName)!;

            parser.subrule(0, baseExpressionRule, false, elements[0], args);
            parser.many(0, {
                GATE: () => {
                    const first = lookahead(1);
                    if (first.image === "(" || first.image === "!!" || first.image === "?." || first.image === ".") {
                        return true;
                    }
                    if (first.image !== "<") {
                        return false;
                    }
                    return isCallAhead(1);
                },
                DEF: () => {
                    parser.alternatives(0, [
                        {
                            ALT: () => {
                                parser.subrule(1, assertNonNullPostfixFragmentRule, true, fragmentRuleCalls[0], args);
                            }
                        },
                        {
                            ALT: () => {
                                parser.subrule(2, memberCallPostfixFragmentRule, true, fragmentRuleCalls[1], args);
                            },
                            GATE: () => {
                                const first = lookahead(1);
                                if (first.image !== "." && first.image !== "?.") {
                                    return false;
                                }
                                const second = lookahead(2);
                                if (second.tokenType.name !== ID.name) {
                                    return false;
                                }
                                return isCallAhead(3);
                            }
                        },
                        {
                            ALT: () => {
                                parser.subrule(3, memberAccessPostfixFragmentRule, true, fragmentRuleCalls[2], args);
                            },
                            GATE: () => {
                                const first = lookahead(1);
                                if (first.image !== "." && first.image !== "?.") {
                                    return false;
                                }
                                const second = lookahead(2);
                                if (second.tokenType.name !== ID.name) {
                                    return false;
                                }
                                return !isCallAhead(3);
                            }
                        },
                        {
                            ALT: () => {
                                parser.subrule(4, callPostfixFragmentRule, true, fragmentRuleCalls[3], args);
                            }
                        }
                    ]);
                }
            });
        };
    };
}

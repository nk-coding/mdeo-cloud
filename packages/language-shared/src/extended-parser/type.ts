import type { BaseParser, LangiumParser } from "langium";
import type { EmbeddedActionsParser } from "chevrotain";

/**
 * Represents the first parameter of the Langium parser's `rule` method.
 * This type defines either a parser rule or an infix rule that can be used
 * to create grammar rules in Langium.
 */
export type ParserOrInfixRule = Parameters<LangiumParser["rule"]>[0];

/**
 * Represents the second parameter of the Langium parser's `rule` method.
 * This is the implementation function that defines how a grammar rule
 * should be parsed, containing the actual parsing logic.
 */
export type RuleImpl = Parameters<LangiumParser["rule"]>[1];

/**
 * Represents the return type of the Langium parser's `rule` method.
 * This is the result obtained after defining a parser rule, typically
 * a rule object that can be used in the grammar definition.
 */
export type RuleResult = ReturnType<LangiumParser["rule"]>;

/**
 * A langium base parser which also exposes the underlying chevrotain parser.
 */
export interface ExtendedParser extends BaseParser {
    /**
     * The underlying Chevrotain parser instance.
     */
    chevrotainParser: EmbeddedActionsParser;
}

/**
 * Type for a function that overrides the implementation of a parser rule.
 *
 * @param rule The parser or infix rule to be overridden.
 * @param impl The original implementation of the rule.
 * @param parser The parser instance where the rule is defined.
 * @returns The new implementation for the rule.
 */
export type RuleOverride = (rule: ParserOrInfixRule, impl: RuleImpl, parser: ExtendedParser) => RuleImpl;

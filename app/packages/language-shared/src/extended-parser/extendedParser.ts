import type { LangiumCompletionParser, LangiumCoreServices, LangiumParser } from "langium";
import type { ExtendedParser, ParserOrInfixRule, RuleImpl, RuleOverride, RuleResult } from "./type.js";
import type { EmbeddedActionsParser } from "chevrotain";
import { sharedImport } from "../sharedImport.js";

const {
    LangiumParser: LangiumParserBase,
    LangiumCompletionParser: LangiumCompletionParserBase,
    createParser
} = sharedImport("langium");

/**
 * Generates extended langium parser and completion parser classes that override the rule method
 * The goal is to allow to dynamically change the implementation of parser rules, as the langium grammar allows for no customization here.
 *
 * @param ruleOverrride function to override rule implementations
 * @returns an object containing the extended LangiumParser and LangiumCompletionParser classes, intended to be used as service providers
 */
export function generateExtendedParser(ruleOverrride: RuleOverride): {
    LangiumParser: (services: LangiumCoreServices) => LangiumParser;
    CompletionParser: (services: LangiumCoreServices) => LangiumCompletionParser;
} {
    class ExtendedLangiumParser extends LangiumParserBase implements ExtendedParser {
        get chevrotainParser(): EmbeddedActionsParser {
            return this.wrapper;
        }

        override rule(rule: ParserOrInfixRule, impl: RuleImpl): RuleResult {
            const newImpl = ruleOverrride(rule, impl, this);
            return super.rule(rule, newImpl);
        }
    }

    class ExtendedCompletionParser extends LangiumCompletionParserBase implements ExtendedParser {
        get chevrotainParser(): EmbeddedActionsParser {
            return this.wrapper;
        }

        override rule(rule: ParserOrInfixRule & { $type: "ParserRule" }, impl: RuleImpl): RuleResult {
            const newImpl = ruleOverrride(rule, impl, this);
            return super.rule(rule, newImpl);
        }
    }

    return {
        LangiumParser: (services) => {
            const grammar = services.Grammar;
            const lexer = services.parser.Lexer;
            const parser = createParser(grammar, new ExtendedLangiumParser(services), lexer.definition);
            parser.finalize();
            return parser;
        },
        CompletionParser: (services) => {
            const grammar = services.Grammar;
            const lexer = services.parser.Lexer;
            const parser = createParser(grammar, new ExtendedCompletionParser(services), lexer.definition);
            parser.finalize();
            return parser;
        }
    };
}

import {
    createRule,
    WS,
    ML_COMMENT,
    SL_COMMENT,
    HIDDEN_NEWLINE,
    many,
    or,
    type ParserRule,
    type RuleEntry,
    NEWLINE
} from "@mdeo/language-common";
import { Config, BaseConfigSection } from "./configTypes.js";
import type { ResolvedConfigContributionPlugins } from "../plugin/resolvePlugins.js";

/**
 * Creates the Config parser rule with dynamic section alternatives.
 * This rule adapts based on the sections contributed by plugins.
 *
 * @param resolvedPlugins The resolved plugin contributions (optional for basic parser)
 * @returns The Config parser rule
 */
export function createConfigRule(resolvedPlugins: ResolvedConfigContributionPlugins): ParserRule<any> {
    if (resolvedPlugins.rules.length === 0) {
        return createRule("ConfigRule")
            .returns(Config)
            .as(() => []);
    }

    const sectionRules = resolvedPlugins.rules;

    return createRule("ConfigRule")
        .returns(Config)
        .as(({ add }) => {
            const sectionAlternatives: RuleEntry[] = sectionRules.map((rule) => add("sections", rule));
            return [many(or(...sectionAlternatives, NEWLINE))];
        });
}

/**
 * Default root rule for Config language (without plugins).
 * Use createConfigRule() with resolved plugins for the full grammar.
 */
export const ConfigRule = createRule("ConfigRule")
    .returns(Config)
    .as(() => []);

/**
 * Base section rule that can be extended by plugins.
 * This rule parses a generic section with braces.
 */
export const BaseSectionRule = createRule("BaseConfigSectionRule")
    .returns(BaseConfigSection)
    .as(() => []);

/**
 * Additional terminals for the Config language.
 */
export const ConfigTerminals = [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT];

/**
 * Generates dynamic keywords based on section names from plugins.
 * Handles disambiguation when multiple plugins contribute the same section name.
 *
 * @param resolvedPlugins The resolved plugin contributions
 * @returns Array of keyword strings for the grammar
 */
export function generateSectionKeywords(resolvedPlugins: ResolvedConfigContributionPlugins): string[] {
    return resolvedPlugins.keywords;
}

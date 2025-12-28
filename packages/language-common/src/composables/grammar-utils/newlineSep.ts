import { atLeastOne, many, optional, or } from "../../grammar/rule/parser/factory.js";
import { groupIfNeeded } from "../../grammar/rule/parser/helpers.js";
import type { RuleEntry } from "../../grammar/rule/parser/types.js";
import { NEWLINE } from "../../language/defaultTokens.js";

/**
 * Creates a rule for a sequence of sections separated by newlines.
 * Each section can have different cardinality (single, optional, multiple, or at least one).
 * The rule handles optional leading and trailing newlines automatically.
 *
 * @param sections Array of sections with their respective cardinalities
 * @returns A grouped rule entry representing the complete newline-separated structure
 */
export function newlineSep(sections: NewlineSepSection[]): RuleEntry {
    let optionalPrefix = 0;
    for (const section of sections) {
        if (isOptionalCardinality(section.cardinality)) {
            optionalPrefix++;
        } else {
            break;
        }
    }

    if (optionalPrefix === 0) {
        return newlineSepInternal(sections);
    }

    const options: RuleEntry[] = [];

    for (let i = 0; i < optionalPrefix; i++) {
        const modifiedSections: NewlineSepSection[] = [
            {
                entry: sections[i].entry,
                cardinality: toNonOptionalCardinality(sections[i].cardinality)
            }
        ];
        for (let j = i + 1; j < sections.length; j++) {
            modifiedSections.push(sections[j]);
        }
        options.push(newlineSepInternal(modifiedSections));
    }

    if (optionalPrefix === sections.length) {
        options.push(many(NEWLINE));
    }

    return or(...options);
}

/**
 * Internal implementation of newlineSep that builds the newline-separated structure.
 * This assumes at least one section is non-optional to avoid ambiguity.
 *
 * @param sections Array of sections with their respective cardinalities
 * @returns A grouped rule entry representing the complete newline-separated structure
 */
function newlineSepInternal(sections: NewlineSepSection[]): RuleEntry {
    const entries: RuleEntry[] = [];
    const manyNewline = many(NEWLINE);
    const atLeastOneNewline = atLeastOne(NEWLINE);

    for (let i = 0; i < sections.length; i++) {
        const section = sections[i];

        switch (section.cardinality) {
            case NewlineSepSectionCardinality.ONE:
                entries.push(i === 0 ? manyNewline : atLeastOneNewline, section.entry);
                break;
            case NewlineSepSectionCardinality.OPTIONAL:
                if (i === 0) {
                    entries.push(manyNewline, optional(section.entry));
                } else {
                    entries.push(optional(atLeastOneNewline, section.entry));
                }
                break;
            case NewlineSepSectionCardinality.MANY:
                if (i === 0) {
                    entries.push(manyNewline, optional(section.entry, many(atLeastOneNewline, section.entry)));
                } else {
                    entries.push(many(atLeastOneNewline, section.entry));
                }
                break;
            case NewlineSepSectionCardinality.AT_LEAST_ONE:
                entries.push(
                    i === 0 ? manyNewline : atLeastOneNewline,
                    section.entry,
                    many(atLeastOneNewline, section.entry)
                );
                break;
        }
    }
    entries.push(manyNewline);

    return groupIfNeeded(entries);
}

/**
 * Checks if a cardinality is optional (allows zero occurrences).
 *
 * @param cardinality The cardinality to check
 * @returns True if the cardinality is OPTIONAL or MANY
 */
function isOptionalCardinality(cardinality: NewlineSepSectionCardinality): boolean {
    return cardinality === NewlineSepSectionCardinality.OPTIONAL || cardinality === NewlineSepSectionCardinality.MANY;
}

/**
 * Converts an optional cardinality to its non-optional equivalent.
 *
 * @param cardinality The cardinality to convert
 * @returns The non-optional equivalent cardinality
 */
function toNonOptionalCardinality(cardinality: NewlineSepSectionCardinality): NewlineSepSectionCardinality {
    switch (cardinality) {
        case NewlineSepSectionCardinality.OPTIONAL:
            return NewlineSepSectionCardinality.ONE;
        case NewlineSepSectionCardinality.MANY:
            return NewlineSepSectionCardinality.AT_LEAST_ONE;
        default:
            return cardinality;
    }
}

/**
 * Cardinality options for sections in a newline-separated rule.
 */
export enum NewlineSepSectionCardinality {
    /**
     *  Exactly one occurrence of the entry
     */
    ONE = "SINGLE",
    /**
     * Zero or one occurrence of the entry
     */
    OPTIONAL = "OPTIONAL",
    /**
     * Zero or more occurrences of the entry, separated by newlines */
    MANY = "MULTIPLE",
    /**
     * One or more occurrences of the entry, separated by newlines
     */
    AT_LEAST_ONE = "AT_LEAST_ONE"
}

/**
 * Represents a section in a newline-separated rule.
 */
export interface NewlineSepSection {
    /**
     * The rule entry for this section
     */
    entry: RuleEntry;
    /**
     * The cardinality specifying how many times this entry can appear
     */
    cardinality: NewlineSepSectionCardinality;
}

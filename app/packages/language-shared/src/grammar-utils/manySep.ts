import { many, optional } from "@mdeo/language-common";
import type { RuleEntry } from "@mdeo/language-common";

/**
 * Options for leading and trailing separators.
 * Leading and trailing separators are always optional.
 */
export interface LeadingTrailingOptions {
    /**
     * Whether to include a leading separator.
     */
    leading: boolean;

    /**
     * Whether to include a trailing separator.
     */
    trailing: boolean;
}

/**
 * Predefined leading/trailing options.
 */
export const LeadingTrailing = {
    LEADING: { leading: true, trailing: false },
    TRAILING: { leading: false, trailing: true },
    BOTH: { leading: true, trailing: true },
    NONE: { leading: false, trailing: false }
};

/**
 * Creates a rule entry for a set of elements separated by a separator.
 * The result is always optional and supports zero or more elements.
 *
 * @param entry The main rule entry that represents each element in the set
 * @param sep The separator rule entry that appears between elements
 * @param leadingTrailing Options for including optional leading and/or trailing separators (defaults to NONE)
 * @returns An array of rule entries that form the complete set pattern
 */
export function manySep(
    entry: RuleEntry,
    sep: RuleEntry,
    leadingTrailing: LeadingTrailingOptions = LeadingTrailing.NONE
): RuleEntry[] {
    const entries: RuleEntry[] = [];
    if (leadingTrailing.leading) {
        entries.push(optional(sep));
    }
    entries.push(optional(entry, many(sep, entry)));
    if (leadingTrailing.trailing) {
        entries.push(optional(sep));
    }
    return entries;
}

/**
 * Creates a rule entry for a set of elements separated by a separator.
 * The result requires at least one element.
 *
 * @param entry The main rule entry that represents each element in the set
 * @param sep The separator rule entry that appears between elements
 * @param leadingTrailing Options for including optional leading and/or trailing separators (defaults to NONE)
 * @returns An array of rule entries that form the complete set pattern
 */
export function atLeastOneSep(
    entry: RuleEntry,
    sep: RuleEntry,
    leadingTrailing: LeadingTrailingOptions = LeadingTrailing.NONE
): RuleEntry[] {
    const entries: RuleEntry[] = [];
    if (leadingTrailing.leading) {
        entries.push(optional(sep));
    }
    entries.push(entry, many(sep, entry));
    if (leadingTrailing.trailing) {
        entries.push(optional(sep));
    }
    return entries;
}

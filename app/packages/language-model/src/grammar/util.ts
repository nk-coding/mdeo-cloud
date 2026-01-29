import type { AstReflection } from "@mdeo/language-common";
import { RangeMultiplicity, SingleMultiplicity, type MultiplicityType } from "@mdeo/language-metamodel";

/**
 * Checks if a multiplicity represents multiple values (> 1).
 *
 * @param multiplicity The multiplicity to check
 * @param reflection The AST reflection utility
 * @returns True if the multiplicity allows multiple values
 */
export function isMultipleMultiplicity(multiplicity: MultiplicityType | undefined, reflection: AstReflection): boolean {
    if (multiplicity == undefined) {
        return false;
    }

    if (reflection.isInstance(multiplicity, SingleMultiplicity)) {
        const value = multiplicity.value;
        const numericValue = multiplicity.numericValue;

        if (value === "*" || value === "+") {
            return true;
        }

        if (numericValue !== undefined && numericValue > 1) {
            return true;
        }

        return false;
    }

    if (reflection.isInstance(multiplicity, RangeMultiplicity)) {
        const upper = multiplicity.upper;
        const upperNumeric = multiplicity.upperNumeric;

        if (upper === "*" || (upperNumeric !== undefined && upperNumeric > 1)) {
            return true;
        }

        return false;
    }

    return false;
}

/**
 * Checks if a multiplicity is optional (0..1).
 *
 * @param multiplicity The multiplicity to check
 * @param reflection The AST reflection utility
 * @returns True if the multiplicity is optional
 */
export function isOptionalMultiplicity(multiplicity: MultiplicityType | undefined, reflection: AstReflection): boolean {
    if (multiplicity == undefined) {
        return false;
    }

    if (reflection.isInstance(multiplicity, SingleMultiplicity)) {
        return multiplicity.value === "?";
    }

    if (reflection.isInstance(multiplicity, RangeMultiplicity)) {
        return multiplicity.lower === 0 && multiplicity.upperNumeric === 1;
    }

    return false;
}

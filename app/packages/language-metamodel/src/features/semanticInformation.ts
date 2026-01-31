import type { AstReflection } from "@mdeo/language-common";
import {
    Class,
    RangeMultiplicity,
    SingleMultiplicity,
    type ClassType,
    type MultiplicityType
} from "../grammar/metamodelTypes.js";

/**
 * Resolves the full class chain including all extended classes for a given class.
 * Guarantees topological order (derived classes come after base classes).
 *
 * @param startClass The starting class
 * @param reflection The AST reflection to check node types
 * @returns An array of classes in topological order
 */
export function resolveClassChain(startClass: ClassType, reflection: AstReflection): ClassType[] {
    const result: ClassType[] = [];
    const resultSet = new Set<ClassType>();
    const queue: ClassType[] = [startClass];

    while (queue.length > 0) {
        const currentClass = queue.shift()!;
        if (!reflection.isInstance(currentClass, Class)) {
            continue;
        }
        if (resultSet.has(currentClass)) {
            continue;
        }
        resultSet.add(currentClass);
        result.push(currentClass);
        for (const classExtension of currentClass.extensions?.extensions ?? []) {
            const reference = classExtension.class.ref;
            if (reference != undefined) {
                queue.push(reference);
            }
        }
    }
    return result;
}

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

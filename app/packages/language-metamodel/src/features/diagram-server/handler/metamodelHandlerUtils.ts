import type { GModelElement } from "@eclipse-glsp/server";
import {
    SingleMultiplicity,
    RangeMultiplicity,
    type SingleMultiplicityType,
    type RangeMultiplicityType,
    type MultiplicityType
} from "../../../grammar/metamodelTypes.js";

/**
 * Checks whether a diagram model element was imported from an external metamodel file.
 *
 * Imported elements are decorated with the {@code "imported"} CSS class by the
 * {@link MetamodelGModelFactory} at model-creation time. Context action handlers
 * use this helper to suppress editing operations on elements that cannot be
 * modified because they live in a different file.
 *
 * @param element The diagram model element to inspect
 * @returns {@code true} when the element carries the {@code "imported"} CSS class
 */
export function isImportedElement(element: GModelElement): boolean {
    return (element as unknown as { cssClasses?: string[] }).cssClasses?.includes("imported") ?? false;
}

/**
 * Derives a default property name from a class name by lower-casing the first letter.
 *
 * For example, {@code "MyClass"} becomes {@code "myClass"} and {@code "B"} becomes {@code "b"}.
 * This mirrors the convention used in {@code MetamodelCreateEdgeSchemaResolver.computePropertyName}.
 *
 * @param className The class name to derive the property name from
 * @returns The default property name, or {@code "ref"} when the class name is empty
 */
export function generateDefaultPropertyName(className: string): string {
    if (!className) {
        return "ref";
    }
    return className.charAt(0).toLowerCase() + className.slice(1);
}

/**
 * Parses a multiplicity string and creates the appropriate AST node.
 *
 * Valid formats:
 * - Single: *, +, ?, or a number
 * - Range: number..*, or number..number
 *
 * @param multiplicity the multiplicity content string (without brackets)
 * @returns the parsed multiplicity AST node or undefined if invalid
 */
export function parseMultiplicity(multiplicity: string): MultiplicityType | undefined {
    if (multiplicity.includes("..")) {
        const parts = multiplicity.split("..");
        if (parts.length !== 2) {
            return undefined;
        }

        const lower = parts[0].trim();
        const upper = parts[1].trim();

        if (!/^\d+$/.test(lower)) {
            return undefined;
        }

        const lowerNum = parseInt(lower, 10);

        if (upper === "*") {
            return {
                $type: RangeMultiplicity.name,
                lower: lowerNum,
                upper: "*"
            } as RangeMultiplicityType;
        } else if (/^\d+$/.test(upper)) {
            return {
                $type: RangeMultiplicity.name,
                lower: lowerNum,
                upperNumeric: parseInt(upper, 10)
            } as RangeMultiplicityType;
        }

        return undefined;
    }

    if (multiplicity === "*" || multiplicity === "+" || multiplicity === "?") {
        return {
            $type: SingleMultiplicity.name,
            value: multiplicity
        } as SingleMultiplicityType;
    }

    if (/^\d+$/.test(multiplicity)) {
        return {
            $type: SingleMultiplicity.name,
            numericValue: parseInt(multiplicity, 10)
        } as SingleMultiplicityType;
    }

    return undefined;
}

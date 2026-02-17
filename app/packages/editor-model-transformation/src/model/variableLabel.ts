import { GLabel } from "@mdeo/editor-shared";
import type { GModelElement } from "@eclipse-glsp/sprotty";

/**
 * Client-side model for variable labels in patterns.
 * Displays the variable name, optional type, and value expression.
 * Example: "var x: Int = a.value + 1"
 */
export class GVariableLabel extends GLabel {}

/**
 * Type guard to check if an element is a variable label.
 *
 * @param element The model element to check
 * @returns True if the element is a GVariableLabel
 */
export function isVariableLabel(element: GModelElement): element is GVariableLabel {
    return element instanceof GVariableLabel;
}

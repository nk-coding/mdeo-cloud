import { GLabel } from "@mdeo/editor-shared";
import type { GModelElement } from "@eclipse-glsp/sprotty";

/**
 * Client-side model for where clause labels in patterns.
 * Displays the constraint expression.
 * Example: "where a.value > 0"
 */
export class GWhereClauseLabel extends GLabel {}

/**
 * Type guard to check if an element is a where clause label.
 *
 * @param element The model element to check
 * @returns True if the element is a GWhereClauseLabel
 */
export function isWhereClauseLabel(element: GModelElement): element is GWhereClauseLabel {
    return element instanceof GWhereClauseLabel;
}

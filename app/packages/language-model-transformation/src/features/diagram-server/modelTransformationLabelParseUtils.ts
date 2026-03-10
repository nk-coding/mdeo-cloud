/**
 * Shared parsing utilities for model transformation label editing.
 * Used by both the apply-label-edit handler and the label-edit validator.
 */

/**
 * Parses an instance label in `name : type` format.
 *
 * @param label - The label text to parse
 * @returns The parsed name and type, or undefined if the format is invalid
 */
export function parseInstanceLabel(label: string): { name: string; type: string } | undefined {
    const colonIndex = label.lastIndexOf(":");
    if (colonIndex === -1) {
        return undefined;
    }
    const name = label.substring(0, colonIndex).trim();
    const type = label.substring(colonIndex + 1).trim();
    return name.length > 0 && type.length > 0 ? { name, type } : undefined;
}

/**
 * Finds the index of the assignment `=` operator in a string,
 * skipping over `==` comparison operators.
 *
 * @param text - The text to search
 * @returns The index of the single `=`, or -1 if not found
 */
export function findAssignmentIndex(text: string): number {
    for (let i = 0; i < text.length; i++) {
        if (text[i] === "=") {
            // Skip `==`.
            if (text[i + 1] === "=") {
                i++;
                continue;
            }
            return i;
        }
    }
    return -1;
}

/**
 * Parses a variable label in `var name[: type] = expr` format.
 * The optional type annotation is preserved in the output for round-tripping,
 * but the type itself is not validated or edited beyond being passed through.
 *
 * @param label - The label text to parse
 * @returns The parsed name and value expression, or undefined if the format is invalid
 */
export function parseVariableLabel(label: string): { name: string; value: string } | undefined {
    if (!label.startsWith("var ")) {
        return undefined;
    }
    const rest = label.substring(4).trim(); // strip "var "
    const eqIndex = findAssignmentIndex(rest);
    if (eqIndex === -1) {
        return undefined;
    }
    // The portion before `=` may be `name` or `name: type` — extract only the name.
    const beforeEq = rest.substring(0, eqIndex).trim();
    const colonIndex = beforeEq.indexOf(":");
    const name = (colonIndex !== -1 ? beforeEq.substring(0, colonIndex) : beforeEq).trim();
    const value = rest.substring(eqIndex + 1).trim();
    return name.length > 0 && value.length > 0 ? { name, value } : undefined;
}

/**
 * Parses a property assignment label into its three parts.
 *
 * Supports both assignment (`=`) and comparison (`==`) operators.
 * The first `=` occurrence determines operator type; `==` takes precedence.
 *
 * @param label - The raw label text to parse
 * @returns The parsed `{ name, operator, value }`, or an error message string if parsing fails
 */
export function parseModelTransformationPropertyLabel(
    label: string
): { name: string; operator: "=" | "=="; value: string } | string {
    const eqIndex = label.indexOf("=");
    if (eqIndex === -1) {
        return "Missing '=' or '==' in property label.";
    }

    const isDouble = label[eqIndex + 1] === "=";
    const operator: "=" | "==" = isDouble ? "==" : "=";
    const operatorEnd = eqIndex + operator.length;

    const name = label.substring(0, eqIndex).trim();
    const value = label.substring(operatorEnd).trim();

    if (name.length === 0) {
        return "Missing property name before operator.";
    }
    if (value.length === 0) {
        return "Missing value expression after operator.";
    }

    return { name, operator, value };
}

/**
 * Extracts the expression text from a where clause label.
 * Expected format: `where <expression>`
 *
 * @param label - The full label text
 * @returns The expression string, or undefined if the prefix is missing
 */
export function extractWhereClauseExpression(label: string): string | undefined {
    const prefix = "where ";
    if (!label.startsWith(prefix)) {
        return undefined;
    }
    return label.substring(prefix.length).trim();
}

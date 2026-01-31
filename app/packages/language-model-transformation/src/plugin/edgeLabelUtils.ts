/**
 * @module edgeLabelUtils
 *
 * Utility functions for computing edge labels in model transformations.
 *
 * Edge labels follow a specific format that encodes the start and end property names
 * of a relation. This format is used consistently across the typed AST representation
 * for pattern links and relations.
 */

/**
 * Computes an edge label from start and end property names.
 *
 * The format is `` `startProperty`_`endProperty` `` where:
 * - Backticks are required around each property name
 * - If a property is undefined, an empty string is used inside the backticks
 * - The order respects the isOutgoing flag: if not outgoing, the order is swapped
 *
 * @param startProperty The property name at the start of the edge, or undefined if not specified
 * @param endProperty The property name at the end of the edge, or undefined if not specified
 * @param isOutgoing True if this is an outgoing edge (start to end), false for incoming
 * @returns The computed edge label string
 */
export function computeEdgeLabel(
    startProperty: string | undefined,
    endProperty: string | undefined,
    isOutgoing: boolean
): string {
    const start = startProperty ?? "";
    const end = endProperty ?? "";
    if (isOutgoing) {
        return `\`${start}\`_\`${end}\``;
    } else {
        return `\`${end}\`_\`${start}\``;
    }
}

/**
 * Parses an edge label back into its component property names.
 *
 * This is the inverse operation of computeEdgeLabel. It extracts the start and end
 * property names from a formatted edge label string.
 *
 * @param edgeLabel The edge label to parse
 * @param isOutgoing True if this is an outgoing edge, false for incoming
 * @returns A tuple of [startProperty, endProperty], where empty strings are converted to undefined
 */
export function parseEdgeLabel(edgeLabel: string, isOutgoing: boolean): [string | undefined, string | undefined] {
    const parts = edgeLabel.split("`_`");
    if (parts.length !== 2) {
        return [undefined, undefined];
    }
    const first = parts[0].replace(/^`/, "") || undefined;
    const second = parts[1].replace(/`$/, "") || undefined;
    if (isOutgoing) {
        return [first, second];
    } else {
        return [second, first];
    }
}

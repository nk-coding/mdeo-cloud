package com.mdeo.modeltransformation.ast

/**
 * Utility functions for computing edge labels in model transformations.
 *
 * Edge labels follow a specific format that encodes the start and end property names
 * of a relation. This format is used consistently across the typed AST representation
 * for pattern links and relations.
 */
object EdgeLabelUtils {

    /**
     * Computes an edge label from start and end property names.
     *
     * The format is `` `startProperty`_`endProperty` `` where:
     * - Backticks are required around each property name
     * - If a property is null, an empty string is used inside the backticks
     * - The order respects the isOutgoing flag: if not outgoing, the order is swapped
     *
     * @param startProperty The property name at the start of the edge, or null if not specified
     * @param endProperty The property name at the end of the edge, or null if not specified
     * @param isOutgoing True if this is an outgoing edge (start to end), false for incoming
     * @return The computed edge label string
     */
    fun computeEdgeLabel(startProperty: String?, endProperty: String?, isOutgoing: Boolean): String {
        val start = startProperty ?: ""
        val end = endProperty ?: ""
        return if (isOutgoing) {
            "`$start`_`$end`"
        } else {
            "`$end`_`$start`"
        }
    }

    /**
     * Parses an edge label back into its component property names.
     *
     * This is the inverse operation of [computeEdgeLabel]. It extracts the start and end
     * property names from a formatted edge label string.
     *
     * @param edgeLabel The edge label to parse
     * @param isOutgoing True if this is an outgoing edge, false for incoming
     * @return A pair of (startProperty, endProperty), where empty strings are converted to null
     */
    fun parseEdgeLabel(edgeLabel: String, isOutgoing: Boolean): Pair<String?, String?> {
        val parts = edgeLabel.split("`_`")
        if (parts.size != 2) {
            return Pair(null, null)
        }
        val first = parts[0].removePrefix("`").ifEmpty { null }
        val second = parts[1].removeSuffix("`").ifEmpty { null }
        return if (isOutgoing) {
            Pair(first, second)
        } else {
            Pair(second, first)
        }
    }
}

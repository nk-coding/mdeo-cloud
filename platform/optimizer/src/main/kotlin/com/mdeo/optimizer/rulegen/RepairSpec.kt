package com.mdeo.optimizer.rulegen

/**
 * Represents a specific repair operation on a metamodel node or edge.
 *
 * Equivalent to RepairSpec in the original mde_optimiser rulegen library.
 *
 * @param nodeName The name of the EClass being operated on.
 * @param edgeName The name of the EReference being modified, or null for node-only operations
 *                 (CREATE / DELETE).
 * @param type     The type of repair operation.
 */
data class RepairSpec(
    val nodeName: String,
    val edgeName: String?,
    val type: RepairSpecType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepairSpec) return false
        return nodeName == other.nodeName &&
            edgeName == other.edgeName &&
            type == other.type
    }

    override fun hashCode(): Int =
        31 * (31 * nodeName.hashCode() + (edgeName?.hashCode() ?: 0)) + type.hashCode()
}

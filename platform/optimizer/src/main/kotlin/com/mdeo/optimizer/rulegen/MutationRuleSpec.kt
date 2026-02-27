package com.mdeo.optimizer.rulegen

import kotlinx.serialization.Serializable

/**
 * Specifies what mutation rules to auto-generate for a given node/edge.
 *
 * Equivalent to RuleSpec in the original mde_optimiser rulegen library.
 * Describes what the rule engine should generate: which node/edge, and what
 * operations (CREATE, DELETE, ADD, REMOVE, or ALL).
 *
 * @param node The EClass name to generate rules for.
 * @param edge Optional reference name to specifically target. When provided,
 *             only edge operations (ADD/REMOVE) for that ref are generated.
 * @param action Which mutation actions to generate. Defaults to ALL.
 */
@Serializable
data class MutationRuleSpec(
    val node: String,
    val edge: String? = null,
    val action: MutationAction = MutationAction.ALL
) {
    /** True when this spec targets a specific edge (reference) rather than the whole node. */
    fun isEdge(): Boolean = edge != null

    /** True when this spec targets the whole node (no specific edge). */
    fun isNode(): Boolean = edge == null
}

/**
 * The set of mutation operations to auto-generate.
 *
 * - CREATE: generate rules that create new nodes
 * - DELETE: generate rules that delete existing nodes
 * - ADD: generate rules that add an edge between two existing nodes
 * - REMOVE: generate rules that remove an edge between two existing nodes
 * - ALL: generate all of the above as appropriate for the target node/edge
 */
@Serializable
enum class MutationAction { ALL, CREATE, DELETE, ADD, REMOVE }

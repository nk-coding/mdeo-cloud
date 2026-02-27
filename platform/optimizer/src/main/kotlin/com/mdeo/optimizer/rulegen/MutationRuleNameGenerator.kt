package com.mdeo.optimizer.rulegen

/**
 * Generates human-readable names for auto-generated mutation rules.
 *
 * Equivalent to RuleNameGenerator in the original mde_optimiser rulegen library.
 * Rule names are used as unique keys in the [MutationRuleGenerator] output and as
 * keys in the transformations map passed to [OptimizationOrchestrator].
 *
 * Naming conventions:
 * - Node-only rules: `<ACTION>_<ClassName>`            e.g. `CREATE_Foo`
 * - Edge rules:      `<ACTION>_<ClassName>_<refName>`  e.g. `ADD_Foo_bar`
 * - Container rules: `CREATE_<ClassName>_in_<Container>_via_<ref>`
 * - LB-repair rules: same as above with `_LBREPAIR` / `_LBREPAIR_MANY` suffix
 */
object MutationRuleNameGenerator {

    /**
     * Generates a name for a node-level operation (CREATE / DELETE).
     *
     * @param action   Uppercase action string, e.g. "CREATE" or "DELETE".
     * @param nodeName The target class name.
     */
    fun forNode(action: String, nodeName: String): String =
        "${action}_${nodeName}"

    /**
     * Generates a name for a node CREATE rule with a specific containment context.
     *
     * @param nodeName       The class being created.
     * @param containerClass The class that will own the new instance.
     * @param containerRef   The containment reference used.
     */
    fun forNodeCreate(nodeName: String, containerClass: String, containerRef: String): String =
        "CREATE_${nodeName}_in_${containerClass}_via_${containerRef}"

    /**
     * Generates a name for an edge-level operation (ADD / REMOVE / CHANGE / SWAP).
     *
     * @param action   Uppercase action string.
     * @param nodeName The source class name.
     * @param refName  The reference name.
     */
    fun forEdge(action: String, nodeName: String, refName: String): String =
        "${action}_${nodeName}_${refName}"

    /**
     * Derives the rule name from a [RepairSpec].
     *
     * @param spec The repair spec describing the operation.
     */
    fun fromRepairSpec(spec: RepairSpec): String {
        val baseAction = when (spec.type) {
            RepairSpecType.CREATE, RepairSpecType.CREATE_LB_REPAIR,
            RepairSpecType.CREATE_LB_REPAIR_MANY -> "CREATE"
            RepairSpecType.DELETE, RepairSpecType.DELETE_LB_REPAIR,
            RepairSpecType.DELETE_LB_REPAIR_MANY -> "DELETE"
            RepairSpecType.ADD -> "ADD"
            RepairSpecType.REMOVE -> "REMOVE"
            RepairSpecType.CHANGE -> "CHANGE"
            RepairSpecType.SWAP -> "SWAP"
        }
        val suffix = when (spec.type) {
            RepairSpecType.CREATE_LB_REPAIR, RepairSpecType.DELETE_LB_REPAIR -> "_LBREPAIR"
            RepairSpecType.CREATE_LB_REPAIR_MANY,
            RepairSpecType.DELETE_LB_REPAIR_MANY -> "_LBREPAIR_MANY"
            else -> ""
        }
        return if (spec.edgeName != null) {
            "${baseAction}_${spec.nodeName}_${spec.edgeName}${suffix}"
        } else {
            "${baseAction}_${spec.nodeName}${suffix}"
        }
    }
}

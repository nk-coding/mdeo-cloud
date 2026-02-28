package com.mdeo.optimizer.rulegen

/**
 * Generates human-readable names for auto-generated mutation rules.
 *
 * Equivalent to RuleNameGenerator in the original mde_optimiser rulegen library.
 * Rule names are used as unique keys in the [MutationRuleGenerator] output and as
 * keys in the transformations map passed to [OptimizationOrchestrator].
 *
 * Naming conventions:
 * - Node-only rules: `<prefix><ACTION>_<ClassName>`            e.g. `CREATE_Foo`, `S_CREATE_Foo`
 * - Edge rules:      `<prefix><ACTION>_<ClassName>_<refName>`  e.g. `ADD_Foo_bar`, `S_ADD_Foo_bar`
 * - Container rules: `<prefix>CREATE_<ClassName>_in_<Container>_via_<ref>`
 * - LB-repair rules: same as above with `_LBREPAIR` / `_LBREPAIR_MANY` suffix
 * - The `prefix` defaults to `""` (base/problem-space rules) and is `"S_"` for
 *   solution-space (refinement-tightened) rules.
 */
object MutationRuleNameGenerator {

    /**
     * Generates a name for a node-level operation (CREATE / DELETE).
     *
     * @param action   Uppercase action string, e.g. "CREATE" or "DELETE".
     * @param nodeName The target class name.
     * @param prefix   Optional name prefix, e.g. `"S_"` for solution-space rules.
     */
    fun forNode(action: String, nodeName: String, prefix: String = ""): String =
        "${prefix}${action}_${nodeName}"

    /**
     * Generates a name for a node CREATE rule with a specific containment context.
     *
     * @param nodeName       The class being created.
     * @param containerClass The class that will own the new instance.
     * @param containerRef   The containment reference used.
     * @param prefix         Optional name prefix, e.g. `"S_"` for solution-space rules.
     */
    fun forNodeCreate(
        nodeName: String,
        containerClass: String,
        containerRef: String,
        prefix: String = ""
    ): String = "${prefix}CREATE_${nodeName}_in_${containerClass}_via_${containerRef}"

    /**
     * Generates a name for an edge-level operation (ADD / REMOVE / CHANGE / SWAP).
     *
     * @param action   Uppercase action string.
     * @param nodeName The source class name.
     * @param refName  The reference name.
     * @param prefix   Optional name prefix, e.g. `"S_"` for solution-space rules.
     */
    fun forEdge(action: String, nodeName: String, refName: String, prefix: String = ""): String =
        "${prefix}${action}_${nodeName}_${refName}"

    /**
     * Derives the rule name from a [RepairSpec].
     *
     * @param spec   The repair spec describing the operation.
     * @param prefix Optional name prefix, e.g. `"S_"` for solution-space rules.
     */
    fun fromRepairSpec(spec: RepairSpec, prefix: String = ""): String {
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
            "${prefix}${baseAction}_${spec.nodeName}_${spec.edgeName}${suffix}"
        } else {
            "${prefix}${baseAction}_${spec.nodeName}${suffix}"
        }
    }
}

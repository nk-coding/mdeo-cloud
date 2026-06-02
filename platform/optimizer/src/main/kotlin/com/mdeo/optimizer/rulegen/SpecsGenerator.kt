package com.mdeo.optimizer.rulegen

/**
 * Translates a [MutationRuleSpec] into one or more [RepairSpec] objects grouped by operation
 * category ("CREATE", "DELETE", "ADD", "REMOVE").
 *
 * The decision logic implements the CPO generation tables from the CPO_Agent_Instructions:
 *
 * ### Table 1 – Create Node aCPSOs
 * A base node-creation spec ([RepairSpecType.CREATE]) is always emitted.  When a reference on
 * the new node has a positive lower bound **and** its opposite end has a finite upper bound:
 * - [RepairSpecType.CREATE_LB_REPAIR] is emitted (single-donor, n ≥ 1).
 * - [RepairSpecType.CREATE_LB_REPAIR_MULTI] is additionally emitted when n > 1 (multi-donor).
 *
 * ### Table 2 – Delete Node aCPSOs
 * A [RepairSpecType.DELETE] spec is always emitted.  For every reference whose opposite end has
 * **fixed cardinality** (k == l > 0):
 * - [RepairSpecType.DELETE_REPAIR_SINGLE] is emitted (move one neighbour to a single other node).
 * - [RepairSpecType.DELETE_REPAIR_MULTI] is additionally emitted when k > 1 (move k neighbours
 *   simultaneously, each to a different replacement node).
 *
 * ### Table 3 – Add Edge aCPSOs
 * - `lower == upper` (fixed cardinality) → [RepairSpecType.SWAP]
 * - `opposite.lower == 1` (target requires exactly one back-link) → [RepairSpecType.CHANGE]
 * - Otherwise → [RepairSpecType.ADD]
 *
 * ### Table 4 – Remove Edge aCPSOs
 * Uses the same decision tree as Add (above).
 */
class SpecsGenerator {

    /**
     * Produces the map of category → repair specs for the given [spec] and [info].
     *
     * Returns an empty map when [MutationRuleSpec.node] is not a known class in [info].
     */
    fun getRepairsForRuleSpec(
        spec: MutationRuleSpec,
        info: MetamodelInfo
    ): Map<String, List<RepairSpec>> {

        if (!info.classNames().contains(spec.node)) return emptyMap()

        val refs = if (spec.edge != null) {
            info.referencesForNode(spec.node).filter { it.refName == spec.edge }
        } else {
            info.referencesForNode(spec.node)
        }

        val result = mutableMapOf<String, MutableList<RepairSpec>>()

        when (spec.action) {
            MutationAction.ALL -> {
                generateCreate(spec.node, refs, result)
                generateDelete(spec.node, info, result)
                generateAdd(spec.node, refs, result)
                generateRemove(spec.node, refs, result)
            }
            MutationAction.CREATE -> generateCreate(spec.node, refs, result)
            MutationAction.DELETE -> generateDelete(spec.node, info, result)
            MutationAction.ADD -> generateAdd(spec.node, refs, result)
            MutationAction.REMOVE -> generateRemove(spec.node, refs, result)
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Combination view (aligned with the Java reference)
    // -------------------------------------------------------------------------

    /**
     * Produces the map of category → **combinations** of repair specs for the given [spec].
     *
     * This mirrors the Java reference (`SpecsGenerator.getRepairsForRuleSpec` returning
     * `Map<String, Set<List<RepairSpec>>>`): for node CREATE/DELETE operations a per-reference set
     * of repair choices is built and then expanded into the cartesian product across references,
     * so that a single generated operator can repair several references at once.
     *
     * Each `List<RepairSpec>` in the resulting sets is one combination — one chosen repair per
     * reference. For edge ADD/REMOVE operations every reference contributes exactly one repair, so
     * the cartesian product yields a single combination that is later split into one rule per edge.
     *
     * The flat [getRepairsForRuleSpec] remains available as the per-reference view.
     */
    fun getRepairCombinationsForRuleSpec(
        spec: MutationRuleSpec,
        info: MetamodelInfo
    ): Map<String, Set<List<RepairSpec>>> {

        if (!info.classNames().contains(spec.node)) return emptyMap()

        val edgeRefs = if (spec.edge != null) {
            info.referencesForNode(spec.node).filter { it.refName == spec.edge }
        } else {
            info.referencesForNode(spec.node)
        }

        val result = mutableMapOf<String, Set<List<RepairSpec>>>()

        when (spec.action) {
            MutationAction.ALL -> {
                result["CREATE"] = cartesianProduct(createRepairsByEdge(spec.node, edgeRefs))
                result["DELETE"] = cartesianProduct(deleteRepairsByEdge(spec.node, info))
                result["ADD"] = cartesianProduct(edgeRepairsByEdge(spec.node, edgeRefs, isRemove = false))
                result["REMOVE"] = cartesianProduct(edgeRepairsByEdge(spec.node, edgeRefs, isRemove = true))
            }
            MutationAction.CREATE -> result["CREATE"] = cartesianProduct(createRepairsByEdge(spec.node, edgeRefs))
            MutationAction.DELETE -> result["DELETE"] = cartesianProduct(deleteRepairsByEdge(spec.node, info))
            MutationAction.ADD -> result["ADD"] = cartesianProduct(edgeRepairsByEdge(spec.node, edgeRefs, isRemove = false))
            MutationAction.REMOVE -> result["REMOVE"] = cartesianProduct(edgeRepairsByEdge(spec.node, edgeRefs, isRemove = true))
        }

        return result
    }

    /**
     * Per-reference CREATE repair choices. Every reference always offers a plain `CREATE` choice
     * (meaning "no donor-based repair for this reference") alongside any applicable donor-based
     * lower-bound repair variants, matching the flat [generateCreate] selection logic.
     */
    private fun createRepairsByEdge(
        className: String,
        refs: List<ReferenceInfo>
    ): Map<String, Set<RepairSpec>> {
        if (refs.isEmpty()) {
            return mapOf("" to linkedSetOf(RepairSpec(className, null, RepairSpecType.CREATE)))
        }
        val byEdge = LinkedHashMap<String, Set<RepairSpec>>()
        for (ref in refs) {
            val choices = linkedSetOf<RepairSpec>()
            // Base choice: create the node without a donor-steal for this reference.
            choices.add(RepairSpec(className, ref.refName, RepairSpecType.CREATE))
            if (ref.lower > 0 && ref.opposite != null && ref.opposite.upper != -1) {
                choices.add(RepairSpec(className, ref.refName, RepairSpecType.CREATE_LB_REPAIR))
                if (ref.lower > 1) {
                    choices.add(RepairSpec(className, ref.refName, RepairSpecType.CREATE_LB_REPAIR_MULTI))
                }
            }
            byEdge[ref.refName] = choices
        }
        return byEdge
    }

    /**
     * Per-reference DELETE repair choices. Mirrors the flat [generateDelete] selection: every
     * reference offers a plain `DELETE` choice, plus neighbour-reconnection repair variants when
     * the opposite end has fixed cardinality (k = l > 0) and the source side is not itself fixed.
     */
    private fun deleteRepairsByEdge(
        className: String,
        info: MetamodelInfo
    ): Map<String, Set<RepairSpec>> {
        val refs = info.referencesForNode(className)
        if (refs.isEmpty()) {
            return mapOf("" to linkedSetOf(RepairSpec(className, null, RepairSpecType.DELETE)))
        }
        val byEdge = LinkedHashMap<String, Set<RepairSpec>>()
        for (ref in refs) {
            val choices = linkedSetOf<RepairSpec>()
            // Base choice: delete the node without re-homing this reference's neighbours.
            choices.add(RepairSpec(className, ref.refName, RepairSpecType.DELETE))

            val targetRefs = info.referencesForNode(ref.targetClass)
            val backRef = targetRefs.find {
                it.targetClass == className && it.isReverse == !ref.isReverse
            }
            if (backRef != null && backRef.lower == backRef.upper && backRef.lower > 0 && ref.lower != ref.upper) {
                choices.add(RepairSpec(className, ref.refName, RepairSpecType.DELETE_REPAIR_SINGLE))
                if (backRef.lower > 1) {
                    choices.add(RepairSpec(className, ref.refName, RepairSpecType.DELETE_REPAIR_MULTI))
                }
            }
            byEdge[ref.refName] = choices
        }
        return byEdge
    }

    /** Per-reference edge ADD/REMOVE repair choice (exactly one per reference). */
    private fun edgeRepairsByEdge(
        className: String,
        refs: List<ReferenceInfo>,
        isRemove: Boolean
    ): Map<String, Set<RepairSpec>> {
        val byEdge = LinkedHashMap<String, Set<RepairSpec>>()
        for (ref in refs) {
            byEdge[ref.refName] = linkedSetOf(
                RepairSpec(className, ref.refName, edgeRepairType(ref, isRemove))
            )
        }
        return byEdge
    }

    /**
     * Cartesian product of per-reference repair-choice sets, mirroring the Java
     * `generateNodeRepairCombinations` (`Sets.cartesianProduct`). Each result list contains one
     * chosen repair per reference; the ordering of references is preserved.
     */
    private fun cartesianProduct(byEdge: Map<String, Set<RepairSpec>>): Set<List<RepairSpec>> {
        if (byEdge.isEmpty()) return emptySet()
        var combinations: List<List<RepairSpec>> = listOf(emptyList())
        for (choices in byEdge.values) {
            if (choices.isEmpty()) continue
            combinations = combinations.flatMap { prefix ->
                choices.map { prefix + it }
            }
        }
        return combinations.toSet()
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun generateCreate(
        className: String,
        refs: List<ReferenceInfo>,
        result: MutableMap<String, MutableList<RepairSpec>>
    ) {
        // Base node-creation rule
        result.getOrPut("CREATE") { mutableListOf() }
            .add(RepairSpec(className, null, RepairSpecType.CREATE))

        // LB-repair variants: needed when the source lower > 0, not fixed-cardinality,
        // and opposite has a finite upper bound (can't always freely steal)
        for (ref in refs) {
            if (ref.lower > 0 && ref.opposite != null && ref.opposite.upper != -1) {
                // Single-donor repair (n ≥ 1)
                result.getOrPut("CREATE") { mutableListOf() }
                    .add(RepairSpec(className, ref.refName, RepairSpecType.CREATE_LB_REPAIR))
                // Multi-donor repair (n > 1): steal one target from each of n different donors
                if (ref.lower > 1) {
                    result.getOrPut("CREATE") { mutableListOf() }
                        .add(RepairSpec(className, ref.refName, RepairSpecType.CREATE_LB_REPAIR_MULTI))
                }
            }
        }
    }

    private fun generateDelete(
        className: String,
        info: MetamodelInfo,
        result: MutableMap<String, MutableList<RepairSpec>>
    ) {
        // Always emit the plain delete (engine adds WHERE guards for soft lower-bounds)
        result.getOrPut("DELETE") { mutableListOf() }
            .add(RepairSpec(className, null, RepairSpecType.DELETE))

        // For every reference whose opposite has FIXED cardinality (k == l > 0):
        // emit repair rules that atomically delete the node and reconnect its neighbours,
        // but only when the source side itself is not fixed-cardinality (n != m).
        val refs = info.referencesForNode(className)
        for (ref in refs) {
            val targetRefs = info.referencesForNode(ref.targetClass)
            val backRef = targetRefs.find {
                it.targetClass == className && it.isReverse == !ref.isReverse
            }
            if (backRef != null && backRef.lower == backRef.upper && backRef.lower > 0 && ref.lower != ref.upper) {
                // Single-neighbour repair (k = l = 1 or k = l > 1, one per application)
                result.getOrPut("DELETE") { mutableListOf() }
                    .add(RepairSpec(className, ref.refName, RepairSpecType.DELETE_REPAIR_SINGLE))
                // Multi-neighbour repair (k = l > 1, handles k neighbours simultaneously)
                if (backRef.lower > 1) {
                    result.getOrPut("DELETE") { mutableListOf() }
                        .add(RepairSpec(className, ref.refName, RepairSpecType.DELETE_REPAIR_MULTI))
                }
            }
        }
    }

    private fun generateAdd(
        className: String,
        refs: List<ReferenceInfo>,
        result: MutableMap<String, MutableList<RepairSpec>>
    ) {
        for (ref in refs) {
            val type = edgeRepairType(ref, isRemove = false)
            result.getOrPut("ADD") { mutableListOf() }
                .add(RepairSpec(className, ref.refName, type))
        }
    }

    private fun generateRemove(
        className: String,
        refs: List<ReferenceInfo>,
        result: MutableMap<String, MutableList<RepairSpec>>
    ) {
        for (ref in refs) {
            val type = edgeRepairType(ref, isRemove = true)
            result.getOrPut("REMOVE") { mutableListOf() }
                .add(RepairSpec(className, ref.refName, type))
        }
    }

    /**
     * Chooses the repair type for an edge operation (both ADD and REMOVE follow the same logic).
     *
     * Mirrors the Java reference (`SpecsGenerator.generateEdgeAddRules` /
     * `generateEdgeRemoveRules`):
     * - Fixed source cardinality (`lower == upper`) → [RepairSpecType.SWAP] (takes precedence).
     * - Opposite end with **fixed** cardinality `k = l > 0`
     *   (`opposite.lower > 0 && opposite.lower == opposite.upper`) → [RepairSpecType.CHANGE].
     * - Everything else (no opposite, soft/optional opposite, or non-fixed opposite) →
     *   [RepairSpecType.ADD] for add, [RepairSpecType.REMOVE] for remove.
     *
     * Note: an opposite whose lower bound is merely `1` but whose upper bound differs
     * (e.g. `1..3` or `1..*`) is **not** fixed cardinality and therefore maps to ADD/REMOVE,
     * not CHANGE.
     */
    private fun edgeRepairType(ref: ReferenceInfo, isRemove: Boolean): RepairSpecType {
        val opposite = ref.opposite
        return when {
            ref.lower == ref.upper -> RepairSpecType.SWAP
            opposite != null && opposite.lower > 0 && opposite.lower == opposite.upper -> RepairSpecType.CHANGE
            else -> if (isRemove) RepairSpecType.REMOVE else RepairSpecType.ADD
        }
    }
}

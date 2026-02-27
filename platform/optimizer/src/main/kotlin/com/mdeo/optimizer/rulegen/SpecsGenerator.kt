package com.mdeo.optimizer.rulegen

/**
 * Computes which [RepairSpec] combinations to generate for a given [MutationRuleSpec],
 * based on metamodel structure.
 *
 * Equivalent to SpecsGenerator in the original mde_optimiser rulegen library.
 *
 * The Java original produces cartesian products across all references of a node (to
 * generate combined multi-reference rules).  This Kotlin port generates independent
 * flat lists — one [RepairSpec] per reference per action — which maps directly to
 * single-operation DSL transformation rules.  Cartesian-product combinations are
 * intentionally omitted as a **design decision** to keep each generated rule atomic
 * (performing exactly one graph operation per application), rather than because they
 * are technically impossible to represent in the custom DSL.
 *
 * REPORT: Cartesian-product rule combinations (e.g. create node AND simultaneously add
 * required neighbours in a single rule) from the Java original's generateNodeRepairCombinations()
 * are not ported.  Each reference is handled by its own independent mutation rule.
 */
class SpecsGenerator {

    /**
     * Returns all [RepairSpec] entries to be generated for [spec], grouped by action key
     * ("CREATE", "DELETE", "ADD", "REMOVE").
     *
     * @param spec            The mutation rule specification.
     * @param metamodelInfo   Platform metamodel wrapper for introspection.
     * @return Map of action key → list of [RepairSpec] values.
     */
    fun getRepairsForRuleSpec(
        spec: MutationRuleSpec,
        metamodelInfo: MetamodelInfo
    ): Map<String, List<RepairSpec>> {
        val result = mutableMapOf<String, List<RepairSpec>>()

        when (spec.action) {
            MutationAction.ALL -> {
                if (spec.isEdge()) {
                    result["ADD"] = generateEdgeAddRepairs(spec, metamodelInfo)
                    result["REMOVE"] = generateEdgeRemoveRepairs(spec, metamodelInfo)
                } else {
                    result["CREATE"] = generateNodeCreateRepairs(spec, metamodelInfo)
                    result["DELETE"] = generateNodeDeleteRepairs(spec, metamodelInfo)
                    result["ADD"] = generateEdgeAddRepairs(spec, metamodelInfo)
                    result["REMOVE"] = generateEdgeRemoveRepairs(spec, metamodelInfo)
                }
            }
            MutationAction.CREATE ->
                result["CREATE"] = generateNodeCreateRepairs(spec, metamodelInfo)
            MutationAction.DELETE ->
                result["DELETE"] = generateNodeDeleteRepairs(spec, metamodelInfo)
            MutationAction.ADD ->
                result["ADD"] = generateEdgeAddRepairs(spec, metamodelInfo)
            MutationAction.REMOVE ->
                result["REMOVE"] = generateEdgeRemoveRepairs(spec, metamodelInfo)
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Node CREATE
    // -------------------------------------------------------------------------

    /**
     * Computes CREATE repair specs for the node named in [spec].
     *
     * Mirrors SpecsGenerator.generateNodeCreateRules() in the Java original.
     *
     * Decision logic per reference:
     * - No opposite: → CREATE (node only, no mandatory neighbours)
     * - With opposite:
     *   - lower == 0: → CREATE (no mandatory neighbours for this ref)
     *   - lower != upper AND opposite has optional upper: → CREATE
     *   - lower != upper AND opposite upper is bounded: → CREATE + CREATE_LB_REPAIR
     *   - lower > 1 AND opposite upper bounded: additionally → CREATE_LB_REPAIR_MANY
     *   - lower == upper AND opposite is optional: → CREATE
     *
     * REPORT: lower-bound repair rules produced here (CREATE_LB_REPAIR / CREATE_LB_REPAIR_MANY)
     * are generated as plain CREATE rules in MutationAstBuilder because the DSL cannot express
     * the "simultaneously satisfy LB on opposite" semantics of the Henshin LB-repair variant.
     */
    private fun generateNodeCreateRepairs(
        spec: MutationRuleSpec,
        metamodelInfo: MetamodelInfo
    ): List<RepairSpec> {
        val repairs = mutableListOf<RepairSpec>()
        val references = metamodelInfo.referencesForNode(spec.node)

        // If there are no references, still generate one standalone CREATE rule.
        if (references.isEmpty()) {
            repairs.add(RepairSpec(spec.node, null, RepairSpecType.CREATE))
            return repairs
        }

        for (ref in references) {
            val opp = ref.opposite
            if (opp == null) {
                repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.CREATE))
            } else {
                when {
                    ref.lower == 0 -> {
                        repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.CREATE))
                    }
                    ref.lower != ref.upper -> {
                        if (opp.lower >= 0 && opp.lower != opp.upper) {
                            repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.CREATE))
                        }
                        if (ref.lower >= 0 && opp.upper != -1) {
                            repairs.add(
                                RepairSpec(spec.node, ref.refName, RepairSpecType.CREATE_LB_REPAIR)
                            )
                        }
                        if (ref.lower > 1 && opp.upper != -1) {
                            repairs.add(
                                RepairSpec(
                                    spec.node, ref.refName,
                                    RepairSpecType.CREATE_LB_REPAIR_MANY
                                )
                            )
                        }
                    }
                    ref.lower == ref.upper -> {
                        if (opp.lower != opp.upper) {
                            repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.CREATE))
                        }
                    }
                }
            }
        }

        // Deduplicate (same node/edge/type triple may be produced multiple times)
        return repairs.distinct()
    }

    // -------------------------------------------------------------------------
    // Node DELETE
    // -------------------------------------------------------------------------

    /**
     * Computes DELETE repair specs for the node named in [spec].
     *
     * Mirrors SpecsGenerator.generateNodeDeleteRules() in the Java original.
     *
     * REPORT: DELETE_LB_REPAIR / DELETE_LB_REPAIR_MANY variants are generated as plain
     * DELETE rules in MutationAstBuilder; the PAC chain is not representable in the DSL.
     */
    private fun generateNodeDeleteRepairs(
        spec: MutationRuleSpec,
        metamodelInfo: MetamodelInfo
    ): List<RepairSpec> {
        val repairs = mutableListOf<RepairSpec>()
        val references = metamodelInfo.referencesForNode(spec.node)

        if (references.isEmpty()) {
            repairs.add(RepairSpec(spec.node, null, RepairSpecType.DELETE))
            return repairs
        }

        for (ref in references) {
            val opp = ref.opposite
            if (opp == null || opp.lower == 0) {
                repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.DELETE))
            } else {
                if (opp.lower > 0 && (opp.upper > opp.lower || opp.upper == -1)) {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.DELETE))
                }
                if (opp.lower == 1 && opp.upper == 1) {
                    if (ref.lower != ref.upper) {
                        repairs.add(
                            RepairSpec(spec.node, ref.refName, RepairSpecType.DELETE_LB_REPAIR)
                        )
                    }
                }
                if (opp.lower == opp.upper && opp.lower > 1) {
                    if (ref.lower != ref.upper) {
                        repairs.add(
                            RepairSpec(spec.node, ref.refName, RepairSpecType.DELETE_LB_REPAIR)
                        )
                        if (ref.lower == 0 || ref.lower > 1) {
                            repairs.add(
                                RepairSpec(
                                    spec.node, ref.refName,
                                    RepairSpecType.DELETE_LB_REPAIR_MANY
                                )
                            )
                        }
                    }
                }
            }
        }

        return repairs.distinct()
    }

    // -------------------------------------------------------------------------
    // Edge ADD
    // -------------------------------------------------------------------------

    /**
     * Computes ADD (and CHANGE/SWAP) repair specs for the edge(s) of the node in [spec].
     *
     * When [MutationRuleSpec.edge] is set, only that reference is considered.
     * Mirrors SpecsGenerator.generateEdgeAddRules() in the Java original.
     *
     * Decision logic per reference:
     * - No opposite OR opposite.lower == 0:
     *   - src lower == upper: → SWAP
     *   - else: → ADD
     * - With opposite AND opposite.lower > 0:
     *   - src lower == upper: → SWAP
     *   - src is variable AND opposite is fixed: → CHANGE
     *   - else: → ADD
     */
    private fun generateEdgeAddRepairs(
        spec: MutationRuleSpec,
        metamodelInfo: MetamodelInfo
    ): List<RepairSpec> {
        val repairs = mutableListOf<RepairSpec>()
        val references = filterReferences(metamodelInfo.referencesForNode(spec.node), spec)

        for (ref in references) {
            val opp = ref.opposite
            if (opp == null || opp.lower == 0) {
                if (ref.lower == ref.upper) {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.SWAP))
                } else {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.ADD))
                }
            } else {
                if (ref.lower == ref.upper) {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.SWAP))
                } else {
                    if (opp.lower == opp.upper) {
                        repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.CHANGE))
                    } else {
                        repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.ADD))
                    }
                }
            }
        }

        return repairs.distinct()
    }

    // -------------------------------------------------------------------------
    // Edge REMOVE
    // -------------------------------------------------------------------------

    /**
     * Computes REMOVE (and CHANGE/SWAP) repair specs for the edge(s) of the node in [spec].
     *
     * When [MutationRuleSpec.edge] is set, only that reference is considered.
     * Mirrors SpecsGenerator.generateEdgeRemoveRules() in the Java original.
     */
    private fun generateEdgeRemoveRepairs(
        spec: MutationRuleSpec,
        metamodelInfo: MetamodelInfo
    ): List<RepairSpec> {
        val repairs = mutableListOf<RepairSpec>()
        val references = filterReferences(metamodelInfo.referencesForNode(spec.node), spec)

        for (ref in references) {
            val opp = ref.opposite
            if (opp == null || opp.lower == 0) {
                if (ref.lower == ref.upper) {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.SWAP))
                } else {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.REMOVE))
                }
            } else {
                if (ref.lower == ref.upper) {
                    repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.SWAP))
                } else {
                    if (opp.lower == opp.upper) {
                        repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.CHANGE))
                    } else {
                        repairs.add(RepairSpec(spec.node, ref.refName, RepairSpecType.REMOVE))
                    }
                }
            }
        }

        return repairs.distinct()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Filters [references] to only the edge named in [spec] when the spec targets a
     * specific edge; returns the full list otherwise.
     */
    private fun filterReferences(
        references: List<ReferenceInfo>,
        spec: MutationRuleSpec
    ): List<ReferenceInfo> =
        if (spec.isEdge()) references.filter { it.refName == spec.edge }
        else references
}

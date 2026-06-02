package com.mdeo.optimizer.rulegen

import com.mdeo.metamodel.data.MetamodelData
import com.mdeo.optimizer.config.RefinementConfig
import org.slf4j.LoggerFactory

/**
 * Entry-point for automatic consistency-preserving mutation operator generation.
 *
 * Given a metamodel and a list of [MutationRuleSpec] entries this object produces a flat list of
 * [GeneratedMutation] objects – each containing a human-readable name and a compiled [TypedAst]
 * ready for execution by the model-transformation engine.
 *
 * ### Generation passes
 *
 * **Pass 1 – Base rules (prefix: none)**
 * Generated from the metamodel as-is.  The specs drive which classes and edges are considered.
 *
 * **Pass 2 – Solution-space rules (prefix: `S_`)**
 * Only performed when [refinements] is non-empty.  A second [MetamodelInfo] is constructed with
 * the refined multiplicities applied, and the same spec list is processed again.  Resulting rule
 * names receive the `"S_"` prefix so they can be distinguished from base rules.
 *
 * ### Deduplication
 * Rules with the same name are silently dropped so that duplicate [MutationRuleSpec] entries in
 * the input do not produce duplicate operators.
 *
 * ### Unknown classes
 * Any spec referencing a class that is not present in the metamodel is skipped with a warning log.
 */
object MutationRuleGenerator {

    private val log = LoggerFactory.getLogger(MutationRuleGenerator::class.java)

    private val specsGenerator = SpecsGenerator()

    /**
     * Generates mutation operators from [metamodelData] according to [specs].
     *
     * @param metamodelData  The metamodel to analyse.
     * @param specs          Operator generation specifications.
     * @param refinements    Optional multiplicity overrides for the solution-space pass.
     * @return               Deduplicated list of [GeneratedMutation] objects.
     */
    fun generate(
        metamodelData: MetamodelData,
        specs: List<MutationRuleSpec>,
        refinements: List<RefinementConfig> = emptyList()
    ): List<GeneratedMutation> {
        if (specs.isEmpty()) return emptyList()

        val baseInfo = MetamodelInfo(metamodelData)
        val result = mutableMapOf<String, GeneratedMutation>()

        // Pass 1: base rules
        generatePass(specs, baseInfo, metamodelData.path, prefix = "", result)

        // Pass 2: solution-space rules (only when refinements are declared)
        if (refinements.isNotEmpty()) {
            val overrides = refinements.map {
                MultiplicityOverride(
                    className = it.className,
                    refName = it.fieldName,
                    lower = it.lower,
                    upper = it.upper
                )
            }
            val refinedInfo = MetamodelInfo.withOverrides(metamodelData, overrides)
            generatePass(specs, refinedInfo, metamodelData.path, prefix = "S_", result)
        }

        return result.values.toList()
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Runs a single generation pass and merges results into [accumulator].
     * Already-seen names are skipped (deduplication across passes).
     */
    private fun generatePass(
        specs: List<MutationRuleSpec>,
        info: MetamodelInfo,
        metamodelPath: String,
        prefix: String,
        accumulator: MutableMap<String, GeneratedMutation>
    ) {
        for (spec in specs) {
            if (!info.classNames().contains(spec.node)) {
                log.warn("MutationRuleGenerator: unknown class '${spec.node}' – skipping spec")
                continue
            }

            val combinationMap = specsGenerator.getRepairCombinationsForRuleSpec(spec, info)

            for ((category, combinations) in combinationMap) {
                for (combination in combinations) {
                    emitCombination(category, combination, info, metamodelPath, prefix, accumulator)
                }
            }
        }
    }

    /**
     * Emits the rule(s) for a single repair-spec combination.
     *
     * For node CREATE/DELETE categories the combination is merged into a single operator (matching
     * the Java reference). Combinations with zero or exactly one *active* repair are routed through
     * the per-spec [emitMutation] so their names and ASTs are byte-for-byte identical to the
     * pre-combination behaviour; combinations with two or more active repairs are emitted as new
     * merged rules. Edge ADD/REMOVE categories are split into one rule per repair spec.
     */
    private fun emitCombination(
        category: String,
        combination: List<RepairSpec>,
        info: MetamodelInfo,
        metamodelPath: String,
        prefix: String,
        accumulator: MutableMap<String, GeneratedMutation>
    ) {
        when (category) {
            "CREATE", "DELETE" -> {
                val active = combination.filter { isActiveRepair(it) }
                when {
                    active.isEmpty() -> {
                        val className = combination.firstOrNull()?.className ?: return
                        val baseType = if (category == "CREATE") RepairSpecType.CREATE else RepairSpecType.DELETE
                        emitMutation(RepairSpec(className, null, baseType), info, metamodelPath, prefix, accumulator)
                    }
                    active.size == 1 -> emitMutation(active.first(), info, metamodelPath, prefix, accumulator)
                    else -> emitMergedCombination(category, active, info, metamodelPath, prefix, accumulator)
                }
            }
            else -> {
                for (repairSpec in combination) {
                    emitMutation(repairSpec, info, metamodelPath, prefix, accumulator)
                }
            }
        }
    }

    private fun isActiveRepair(spec: RepairSpec): Boolean = when (spec.type) {
        RepairSpecType.CREATE_LB_REPAIR,
        RepairSpecType.CREATE_LB_REPAIR_MULTI,
        RepairSpecType.DELETE_REPAIR_SINGLE,
        RepairSpecType.DELETE_REPAIR_MULTI -> true
        else -> false
    }

    /**
     * Emits a merged operator that applies two or more active repairs to the same created/deleted
     * node. CREATE combinations are expanded across every containment context (as for single CREATE
     * rules); DELETE combinations have no containment context.
     */
    private fun emitMergedCombination(
        category: String,
        active: List<RepairSpec>,
        info: MetamodelInfo,
        metamodelPath: String,
        prefix: String,
        accumulator: MutableMap<String, GeneratedMutation>
    ) {
        val className = active.first().className
        if (category == "DELETE") {
            val name = prefix + MutationRuleNameGenerator.forCombination(
                MutationRuleNameGenerator.forNode("DELETE", className), active
            )
            addCombinationIfAbsent(name, active, metamodelPath, info, null, accumulator)
            return
        }

        // CREATE: expand across containment contexts, mirroring emitMutation.
        val contexts = info.containmentContextsFor(className)
        if (contexts.isEmpty()) {
            val name = prefix + MutationRuleNameGenerator.forCombination(
                MutationRuleNameGenerator.forNode("CREATE", className), active
            )
            addCombinationIfAbsent(name, active, metamodelPath, info, null, accumulator)
        } else {
            for ((containerClass, refName) in contexts) {
                val baseName = MutationRuleNameGenerator.forNodeCreate(className, containerClass, refName)
                val name = prefix + MutationRuleNameGenerator.forCombination(baseName, active)
                addCombinationIfAbsent(name, active, metamodelPath, info, containerClass to refName, accumulator)
            }
        }
    }

    private fun addCombinationIfAbsent(
        name: String,
        active: List<RepairSpec>,
        metamodelPath: String,
        info: MetamodelInfo,
        createContext: Pair<String, String>?,
        accumulator: MutableMap<String, GeneratedMutation>
    ) {
        if (accumulator.containsKey(name)) return
        try {
            val ast = MutationAstBuilder.buildCombination(active, metamodelPath, info, createContext)
            accumulator[name] = GeneratedMutation(name, ast)
        } catch (e: Exception) {
            log.warn("MutationRuleGenerator: failed to build merged rule '$name': ${e.message}")
        }
    }

    /**
     * Translates a single [RepairSpec] into one or more [GeneratedMutation] objects and adds them
     * to [accumulator] (skipping names that are already present).
     *
     * CREATE specs are special: they may expand into multiple rules, one per containment context.
     * All other spec types produce exactly one rule.
     */
    private fun emitMutation(
        spec: RepairSpec,
        info: MetamodelInfo,
        metamodelPath: String,
        prefix: String,
        accumulator: MutableMap<String, GeneratedMutation>
    ) {
        when (spec.type) {
            RepairSpecType.CREATE -> {
                val contexts = info.containmentContextsFor(spec.className)
                if (contexts.isEmpty()) {
                    // Standalone create (no containment context)
                    val name = prefix + MutationRuleNameGenerator.forNode("CREATE", spec.className)
                    addIfAbsent(name, spec, metamodelPath, info, null, accumulator)
                } else {
                    for ((containerClass, refName) in contexts) {
                        val name = prefix + MutationRuleNameGenerator.forNodeCreate(
                            spec.className, containerClass, refName
                        )
                        addIfAbsent(name, spec, metamodelPath, info, containerClass to refName, accumulator)
                    }
                }
            }

            RepairSpecType.CREATE_LB_REPAIR,
            RepairSpecType.CREATE_LB_REPAIR_MULTI -> {
                val contexts = info.containmentContextsFor(spec.className)
                val suffix = when (spec.type) {
                    RepairSpecType.CREATE_LB_REPAIR -> "_LBREPAIR"
                    RepairSpecType.CREATE_LB_REPAIR_MULTI -> "_LBREPAIR_MULTI"
                }

                if (contexts.isEmpty()) {
                    val name = prefix + MutationRuleNameGenerator.fromRepairSpec(spec)
                    addIfAbsent(name, spec, metamodelPath, info, null, accumulator)
                } else {
                    for ((containerClass, refName) in contexts) {
                        val name = prefix + MutationRuleNameGenerator.forNodeCreate(
                            spec.className, containerClass, refName
                        ) + suffix
                        addIfAbsent(name, spec, metamodelPath, info, containerClass to refName, accumulator)
                    }
                }
            }

            else -> {
                val name = prefix + MutationRuleNameGenerator.fromRepairSpec(spec)
                addIfAbsent(name, spec, metamodelPath, info, null, accumulator)
            }
        }
    }

    private fun addIfAbsent(
        name: String,
        spec: RepairSpec,
        metamodelPath: String,
        info: MetamodelInfo,
        createContext: Pair<String, String>?,
        accumulator: MutableMap<String, GeneratedMutation>
    ) {
        if (accumulator.containsKey(name)) return
        try {
            val ast = MutationAstBuilder.build(name, spec, metamodelPath, info, createContext)
            accumulator[name] = GeneratedMutation(name, ast)
        } catch (e: Exception) {
            log.warn("MutationRuleGenerator: failed to build rule '$name': ${e.message}")
        }
    }
}

package com.mdeo.optimizer.rulegen

import com.mdeo.expression.ast.types.MetamodelData
import org.slf4j.LoggerFactory

/**
 * Entry point for auto-generating mutation operators from a metamodel description.
 *
 * Equivalent to RulesGenerator in the original mde_optimiser rulegen library, but
 * instead of producing Henshin Module objects it produces [GeneratedMutation] values
 * — each containing a unique rule name and a ready-to-run [TypedAst].
 *
 * ## Usage
 * ```kotlin
 * val mutations = MutationRuleGenerator.generate(
 *     metamodelData = metamodelData,
 *     specs = listOf(MutationRuleSpec(node = "Foo", action = MutationAction.ALL)),
 *     metamodelPath = "/path/to/metamodel.mm"
 * )
 * // mutations: List<GeneratedMutation>, each with a unique name and TypedAst
 * ```
 *
 * ## Deduplication
 * The Java original uses `HenshinModuleAnalysis` (semantic rule equality) to remove
 * duplicate rules.  This port deduplicates by rule name instead — an identical
 * (node, edge, type) triple always produces the same name, so structural duplicates
 * collapse naturally.
 *
 * REPORT: Semantic deduplication (two structurally different rules that are
 * behaviourally equivalent) is not performed. Name-based deduplication covers all
 * cases produced by the current [SpecsGenerator] logic.
 *
 * **Known limitation:** The original Java `RulesGenerator` performs a second generation
 * pass using refined multiplicities from `GoalConfig.refinements`. This Kotlin port
 * currently performs only a single pass with the base metamodel multiplicities.
 * Refinements (lower/upper bound tightenings from
 * [com.mdeo.optimizer.config.RefinementConfig]) are intentionally not applied during
 * generation. This means the "solution metamodel" rules (S-type in the original) are
 * not generated.
 */
object MutationRuleGenerator {

    private val logger = LoggerFactory.getLogger(MutationRuleGenerator::class.java)

    /**
     * Generates mutation rules for all [specs] against the given [metamodelData].
     *
     * @param metamodelData The platform metamodel description to introspect.
     * @param specs         List of [MutationRuleSpec] entries describing what to generate.
     * @param metamodelPath Absolute file-system path of the metamodel (stored in each TypedAst).
     *                      Defaults to [MetamodelData.path].
     * @return Deduplicated list of [GeneratedMutation] values in stable order.
     */
    fun generate(
        metamodelData: MetamodelData,
        specs: List<MutationRuleSpec>,
        metamodelPath: String = metamodelData.path
    ): List<GeneratedMutation> {
        if (specs.isEmpty()) {
            logger.debug("MutationRuleGenerator: no specs provided, returning empty list")
            return emptyList()
        }

        val metamodelInfo = MetamodelInfo(metamodelData)
        val specsGenerator = SpecsGenerator()

        // name → GeneratedMutation (insertion-ordered, deduplicated by name)
        val seen = LinkedHashMap<String, GeneratedMutation>()

        for (spec in specs) {
            logger.debug("Processing MutationRuleSpec: node={}, edge={}, action={}", spec.node, spec.edge, spec.action)

            if (!metamodelInfo.hasClass(spec.node)) {
                logger.warn("Class '{}' not found in metamodel — skipping spec", spec.node)
                continue
            }

            val repairMap = specsGenerator.getRepairsForRuleSpec(spec, metamodelInfo)

            for ((_, repairList) in repairMap) {
                for (repairSpec in repairList) {
                    val ruleName = MutationRuleNameGenerator.fromRepairSpec(repairSpec)

                    if (seen.containsKey(ruleName)) {
                        logger.debug("Skipping duplicate rule: {}", ruleName)
                        continue
                    }

                    val asts = buildAsts(ruleName, repairSpec, metamodelPath, metamodelInfo)
                    for ((name, typedAst) in asts) {
                        if (!seen.containsKey(name)) {
                            seen[name] = GeneratedMutation(name = name, typedAst = typedAst)
                            logger.debug("Generated mutation rule: {}", name)
                        }
                    }
                }
            }
        }

        logger.info("MutationRuleGenerator produced {} rule(s) from {} spec(s)", seen.size, specs.size)
        return seen.values.toList()
    }

    /**
     * Builds one or more (name, TypedAst) pairs for [repairSpec].
     *
     * CREATE rules expand to one rule per containment context (or one standalone rule
     * when none exist).  The rule name is derived from the containment context rather
     * than [baseName], so multiple CREATE specs for the same node (different edge variants)
     * always collapse to the same set of context-specific or standalone names — the
     * deduplication in [generate] then keeps only the first occurrence.
     *
     * Non-CREATE rules produce exactly one rule using [baseName].
     */
    private fun buildAsts(
        baseName: String,
        repairSpec: RepairSpec,
        metamodelPath: String,
        metamodelInfo: MetamodelInfo
    ): List<Pair<String, com.mdeo.modeltransformation.ast.TypedAst>> {
        return when (repairSpec.type) {
            RepairSpecType.CREATE,
            RepairSpecType.CREATE_LB_REPAIR,
            RepairSpecType.CREATE_LB_REPAIR_MANY -> {
                val contexts = metamodelInfo.containmentContextsFor(repairSpec.nodeName)
                if (contexts.isEmpty()) {
                    // Standalone: always use CREATE_ClassName — deduplication handles duplicates
                    val standaloneName = MutationRuleNameGenerator.forNode("CREATE", repairSpec.nodeName)
                    val ast = MutationAstBuilder.build(
                        standaloneName, repairSpec, metamodelPath, metamodelInfo, createContext = null
                    )
                    listOfNotNull(ast?.let { standaloneName to it })
                } else {
                    // One TypedAst per containment context — each runs independently
                    contexts.mapNotNull { (containerClass, containerRef) ->
                        val contextName = MutationRuleNameGenerator.forNodeCreate(
                            repairSpec.nodeName, containerClass, containerRef
                        )
                        val ast = MutationAstBuilder.build(
                            contextName, repairSpec, metamodelPath, metamodelInfo,
                            createContext = containerClass to containerRef
                        )
                        ast?.let { contextName to it }
                    }
                }
            }
            else -> {
                val ast = MutationAstBuilder.build(
                    baseName, repairSpec, metamodelPath, metamodelInfo
                )
                listOfNotNull(ast?.let { baseName to it })
            }
        }
    }
}

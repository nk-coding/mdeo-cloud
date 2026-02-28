package com.mdeo.optimizer.rulegen

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLink
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement

/**
 * Builds [TypedAst] objects programmatically for each [RepairSpec].
 *
 * This is the DSL-generation core, equivalent to the command classes
 * (AddEdgeRuleCommand, RemoveEdgeRuleCommand, ChangeEdgeRuleCommand, etc.)
 * in the original mde_optimiser rulegen library — but instead of producing
 * Henshin Module objects, it produces [TypedAst] instances ready for direct
 * execution by the platform's [TransformationEngine].
 *
 * ## Pattern model mapping
 * Every generated rule is a single [TypedMatchStatement] whose [TypedPattern]
 * contains elements with different modifiers:
 *
 * | Modifier   | Meaning                                    |
 * |------------|--------------------------------------------|
 * | null       | match (LHS node/edge — must exist)         |
 * | "create"   | create during execution                    |
 * | "delete"   | delete during execution                    |
 * | "forbid"   | NAC — must NOT exist for rule to fire      |
 *
 * ## Multiplicity guards
 * [MultiplicityGuardBuilder] generates `where` clauses with `.size()` checks
 * to prevent rules from firing when they would violate multiplicity constraints:
 *
 * - **ADD**: upper-bound guard on the source reference.
 * - **REMOVE**: lower-bound guard on the source reference.
 * - **CHANGE**: upper/lower-bound guards on the opposite reference (if bidirectional).
 * - **SWAP**: no guards (cardinality is preserved).
 * - **CREATE**: upper-bound guard on the container's containment reference.
 * - **DELETE**: lower-bound guards on neighbour nodes whose opposite refs have positive lower bounds.
 *
 * Note: CREATE_LB_REPAIR, CREATE_LB_REPAIR_MANY, DELETE_LB_REPAIR and
 * DELETE_LB_REPAIR_MANY are treated as their base variants (CREATE/DELETE) because
 * the custom DSL has no representation for lower-bound repair chains.
 */
object MutationAstBuilder {

    /**
     * Builds a [TypedAst] for the given [spec] and [ruleName].
     *
     * @param ruleName       Unique name for the rule.
     * @param spec           The repair spec describing what to generate.
     * @param metamodelPath  Absolute path of the metamodel this rule targets (stored in TypedAst).
     * @param metamodelInfo  Introspection helper for target/container class lookup.
     * @param createContext  For CREATE specs only: the specific containment context
     *                       (containerClass to containerRef) to use.  When null a standalone
     *                       creation rule (no container) is generated.  Ignored for non-CREATE specs.
     * @return [TypedAst] ready for the TransformationEngine, or null if the spec
     *         cannot be generated.
     */
    fun build(
        ruleName: String,
        spec: RepairSpec,
        metamodelPath: String,
        metamodelInfo: MetamodelInfo,
        createContext: Pair<String, String>? = null
    ): TypedAst? {
        val guardBuilder = MultiplicityGuardBuilder(metamodelPath)

        val statements = when (spec.type) {
            RepairSpecType.ADD ->
                buildAddEdge(spec, metamodelInfo, guardBuilder)
            RepairSpecType.REMOVE ->
                buildRemoveEdge(spec, metamodelInfo, guardBuilder)
            RepairSpecType.CHANGE, RepairSpecType.SWAP ->
                buildChangeEdge(spec, metamodelInfo, guardBuilder)
            RepairSpecType.CREATE,
            RepairSpecType.CREATE_LB_REPAIR,
            RepairSpecType.CREATE_LB_REPAIR_MANY ->
                buildCreateNode(spec, createContext, metamodelInfo, guardBuilder)
            RepairSpecType.DELETE,
            RepairSpecType.DELETE_LB_REPAIR,
            RepairSpecType.DELETE_LB_REPAIR_MANY ->
                buildDeleteNode(spec, metamodelInfo, guardBuilder)
        } ?: return null

        return TypedAst(
            types = guardBuilder.getTypes(),
            metamodelPath = metamodelPath,
            statements = statements
        )
    }

    // -------------------------------------------------------------------------
    // ADD edge
    // -------------------------------------------------------------------------

    /**
     * Generates a rule that ADDS an edge between two existing nodes.
     *
     * Pattern:
     * ```
     * match {
     *   source: SourceClass {}
     *   target: TargetClass {}
     *   forbid source.refName -- target   // NAC: link must not already exist
     *   create source.refName -- target   // effect: create the link
     *   where source.refName.size() < upper  // only when upper > 0
     * }
     * ```
     *
     * When the reference has a bounded upper multiplicity (`upper > 0`), an
     * upper-bound guard is added to prevent exceeding the maximum cardinality.
     *
     * @param spec           The repair spec with node/edge info.
     * @param metamodelInfo  Metamodel introspection for reference lookup.
     * @param guardBuilder   Builder for multiplicity guard where-clauses.
     * @return Match statements for the rule, or null when [RepairSpec.edgeName] is null.
     */
    private fun buildAddEdge(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedMatchStatement>? {
        val refName = spec.edgeName ?: return null
        val refInfo = findReference(spec.nodeName, refName, metamodelInfo) ?: return null

        val sourceVar = "source"
        val targetVar = "target"

        val pattern = TypedPattern(
            elements = buildList {
                add(objectElement(modifier = null, name = sourceVar, className = spec.nodeName))
                add(objectElement(modifier = null, name = targetVar, className = refInfo.targetClass))
                add(linkElement(modifier = "forbid", srcName = sourceVar, refName = refName, tgtName = targetVar))
                add(linkElement(modifier = "create", srcName = sourceVar, refName = refName, tgtName = targetVar))
                if (refInfo.upper > 0) {
                    add(guardBuilder.buildUpperBoundGuard(
                        varName = sourceVar,
                        varClassName = spec.nodeName,
                        refName = refName,
                        targetClassName = refInfo.targetClass,
                        upperBound = refInfo.upper
                    ))
                }
            }
        )

        return listOf(TypedMatchStatement(pattern = pattern))
    }

    // -------------------------------------------------------------------------
    // REMOVE edge
    // -------------------------------------------------------------------------

    /**
     * Generates a rule that REMOVES an existing edge.
     *
     * Pattern:
     * ```
     * match {
     *   source: SourceClass {}
     *   target: TargetClass {}
     *   delete source.refName -- target   // effect: remove the link
     *   where source.refName.size() > lower  // only when lower > 0
     * }
     * ```
     *
     * When the reference has a positive lower multiplicity (`lower > 0`), a
     * lower-bound guard is added to prevent dropping below the minimum cardinality.
     *
     * @param spec           The repair spec with node/edge info.
     * @param metamodelInfo  Metamodel introspection for reference lookup.
     * @param guardBuilder   Builder for multiplicity guard where-clauses.
     * @return Match statements for the rule, or null when [RepairSpec.edgeName] is null.
     */
    private fun buildRemoveEdge(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedMatchStatement>? {
        val refName = spec.edgeName ?: return null
        val refInfo = findReference(spec.nodeName, refName, metamodelInfo) ?: return null

        val sourceVar = "source"
        val targetVar = "target"

        val pattern = TypedPattern(
            elements = buildList {
                add(objectElement(modifier = null, name = sourceVar, className = spec.nodeName))
                add(objectElement(modifier = null, name = targetVar, className = refInfo.targetClass))
                add(linkElement(modifier = "delete", srcName = sourceVar, refName = refName, tgtName = targetVar))
                if (refInfo.lower > 0) {
                    add(guardBuilder.buildLowerBoundGuard(
                        varName = sourceVar,
                        varClassName = spec.nodeName,
                        refName = refName,
                        targetClassName = refInfo.targetClass,
                        lowerBound = refInfo.lower
                    ))
                }
            }
        )

        return listOf(TypedMatchStatement(pattern = pattern))
    }

    // -------------------------------------------------------------------------
    // CHANGE / SWAP edge
    // -------------------------------------------------------------------------

    /**
     * Generates a rule that CHANGEs (or SWAPs) the target of an edge.
     *
     * Pattern:
     * ```
     * match {
     *   source:    SourceClass {}
     *   oldTarget: TargetClass {}
     *   newTarget: TargetClass {}
     *   forbid source.refName -- newTarget  // NAC: new link must not exist yet
     *   delete source.refName -- oldTarget  // remove old link
     *   create source.refName -- newTarget  // add new link
     *   where newTarget.oppositeRef.size() < upper  // CHANGE only, when bounded
     *   where oldTarget.oppositeRef.size() > lower  // CHANGE only, when lower > 0
     * }
     * ```
     *
     * For CHANGE rules with a bidirectional reference, the opposite end's cardinality
     * is affected: `newTarget` gains one opposite edge and `oldTarget` loses one.
     * Upper/lower-bound guards on the opposite reference prevent violations.
     *
     * For SWAP rules no guards are generated (cardinality is preserved by definition).
     *
     * @param spec           The repair spec with node/edge info and type (CHANGE or SWAP).
     * @param metamodelInfo  Metamodel introspection for reference lookup.
     * @param guardBuilder   Builder for multiplicity guard where-clauses.
     * @return Match statements for the rule, or null when [RepairSpec.edgeName] is null.
     */
    private fun buildChangeEdge(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedMatchStatement>? {
        val refName = spec.edgeName ?: return null
        val refInfo = findReference(spec.nodeName, refName, metamodelInfo) ?: return null

        val sourceVar = "source"
        val oldTargetVar = "oldTarget"
        val newTargetVar = "newTarget"

        val pattern = TypedPattern(
            elements = buildList {
                add(objectElement(modifier = null, name = sourceVar, className = spec.nodeName))
                add(objectElement(modifier = null, name = oldTargetVar, className = refInfo.targetClass))
                add(objectElement(modifier = null, name = newTargetVar, className = refInfo.targetClass))
                add(linkElement(modifier = "forbid", srcName = sourceVar, refName = refName, tgtName = newTargetVar))
                add(linkElement(modifier = "delete", srcName = sourceVar, refName = refName, tgtName = oldTargetVar))
                add(linkElement(modifier = "create", srcName = sourceVar, refName = refName, tgtName = newTargetVar))
                if (spec.type != RepairSpecType.SWAP) {
                    val opp = refInfo.opposite
                    if (opp?.refName != null) {
                        if (opp.upper > 0) {
                            add(guardBuilder.buildUpperBoundGuard(
                                varName = newTargetVar,
                                varClassName = refInfo.targetClass,
                                refName = opp.refName,
                                targetClassName = spec.nodeName,
                                upperBound = opp.upper
                            ))
                        }
                        if (opp.lower > 0) {
                            add(guardBuilder.buildLowerBoundGuard(
                                varName = oldTargetVar,
                                varClassName = refInfo.targetClass,
                                refName = opp.refName,
                                targetClassName = spec.nodeName,
                                lowerBound = opp.lower
                            ))
                        }
                    }
                }
            }
        )

        return listOf(TypedMatchStatement(pattern = pattern))
    }

    // -------------------------------------------------------------------------
    // CREATE node
    // -------------------------------------------------------------------------

    /**
     * Generates a CREATE rule for a single containment context (or standalone).
     *
     * When [createContext] is provided:
     * ```
     * match {
     *   container: ContainerClass {}
     *   create newNode: NodeClass {}
     *   create container.containsRef -- newNode
     *   where container.containsRef.size() < upper  // only when upper > 0
     * }
     * ```
     *
     * When [createContext] is null (standalone):
     * ```
     * match {
     *   create newNode: NodeClass {}
     * }
     * ```
     *
     * When the containment reference has a bounded upper multiplicity (`upper > 0`),
     * an upper-bound guard prevents creating more children than the constraint allows.
     *
     * REPORT: CREATE_LB_REPAIR and CREATE_LB_REPAIR_MANY variants are downgraded to this
     * plain CREATE; LB repair chain is not representable in the custom DSL.
     *
     * @param spec           The repair spec with node info.
     * @param createContext  Optional (containerClass, containerRef) for containment creation.
     * @param metamodelInfo  Metamodel introspection for reference lookup.
     * @param guardBuilder   Builder for multiplicity guard where-clauses.
     */
    private fun buildCreateNode(
        spec: RepairSpec,
        createContext: Pair<String, String>?,
        metamodelInfo: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedMatchStatement>? {
        val newNodeVar = "newNode"

        return if (createContext == null) {
            val pattern = TypedPattern(
                elements = listOf(
                    objectElement(modifier = "create", name = newNodeVar, className = spec.nodeName)
                )
            )
            listOf(TypedMatchStatement(pattern = pattern))
        } else {
            val (containerClass, containerRef) = createContext
            val containerVar = "container"
            val containerRefInfo = findReference(containerClass, containerRef, metamodelInfo)
            val pattern = TypedPattern(
                elements = buildList {
                    add(objectElement(modifier = null, name = containerVar, className = containerClass))
                    add(objectElement(modifier = "create", name = newNodeVar, className = spec.nodeName))
                    add(linkElement(
                        modifier = "create",
                        srcName = containerVar,
                        refName = containerRef,
                        tgtName = newNodeVar
                    ))
                    if (containerRefInfo != null && containerRefInfo.upper > 0) {
                        add(guardBuilder.buildUpperBoundGuard(
                            varName = containerVar,
                            varClassName = containerClass,
                            refName = containerRef,
                            targetClassName = spec.nodeName,
                            upperBound = containerRefInfo.upper
                        ))
                    }
                }
            )
            listOf(TypedMatchStatement(pattern = pattern))
        }
    }

    // -------------------------------------------------------------------------
    // DELETE node
    // -------------------------------------------------------------------------

    /**
     * Generates a rule that DELETES an existing node.
     *
     * Pattern (no guarded references):
     * ```
     * match {
     *   node: NodeClass {}      // match (find it first)
     *   delete node             // then delete (reference to matched node, no className)
     * }
     * ```
     *
     * Pattern (with guarded references):
     * ```
     * match {
     *   node: NodeClass {}
     *   delete node
     *   neighbor_ref: TargetClass {}                  // match a representative neighbour
     *   node.ref -- neighbor_ref                      // match the existing link
     *   where neighbor_ref.opposite.size() > lower    // guard: neighbour must have more than lower edges
     * }
     * ```
     *
     * Two separate elements with the same name are required: the first (modifier=null)
     * performs the graph search; the second (modifier="delete", className=null) marks
     * the matched instance for deletion.
     *
     * For each reference `r` of [spec.nodeName] where the opposite end has a named
     * reference with `lower > 0`, a neighbour match element, a link match element, and
     * a lower-bound where-clause are added to prevent deletion when the neighbour node
     * would drop below its required minimum cardinality.
     *
     * Note: DELETE_LB_REPAIR and DELETE_LB_REPAIR_MANY are downgraded to this DELETE rule.
     *
     * @param spec           The repair spec with node info.
     * @param metamodelInfo  Metamodel introspection for reference lookup.
     * @param guardBuilder   Builder for multiplicity guard where-clauses.
     * @return Match statements for the rule.
     */
    private fun buildDeleteNode(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedMatchStatement>? {
        val nodeVar = "node"

        val guardedRefs = metamodelInfo.referencesForNode(spec.nodeName)
            .filter { ref ->
                ref.opposite != null &&
                ref.opposite.refName != null &&
                ref.opposite.lower > 0
            }

        val pattern = TypedPattern(
            elements = buildList {
                add(objectElement(modifier = null, name = nodeVar, className = spec.nodeName))
                add(TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        modifier = "delete",
                        name = nodeVar,
                        className = null,
                        properties = emptyList()
                    )
                ))
                for (ref in guardedRefs) {
                    val opp = ref.opposite!!
                    val neighborVar = "neighbor_${ref.refName}"
                    add(objectElement(modifier = null, name = neighborVar, className = ref.targetClass))
                    add(linkElement(modifier = null, srcName = nodeVar, refName = ref.refName, tgtName = neighborVar))
                    add(guardBuilder.buildLowerBoundGuard(
                        varName = neighborVar,
                        varClassName = ref.targetClass,
                        refName = opp.refName!!,
                        targetClassName = spec.nodeName,
                        lowerBound = opp.lower
                    ))
                }
            }
        )

        return listOf(TypedMatchStatement(pattern = pattern))
    }

    // -------------------------------------------------------------------------
    // Pattern element factory helpers
    // -------------------------------------------------------------------------

    private fun objectElement(
        modifier: String?,
        name: String,
        className: String?
    ): TypedPatternObjectInstanceElement =
        TypedPatternObjectInstanceElement(
            objectInstance = TypedPatternObjectInstance(
                modifier = modifier,
                name = name,
                className = className,
                properties = emptyList()
            )
        )

    private fun linkElement(
        modifier: String?,
        srcName: String,
        refName: String,
        tgtName: String
    ): TypedPatternLinkElement =
        TypedPatternLinkElement(
            link = TypedPatternLink(
                modifier = modifier,
                source = TypedPatternLinkEnd(objectName = srcName, propertyName = refName),
                target = TypedPatternLinkEnd(objectName = tgtName, propertyName = null)
            )
        )

    // -------------------------------------------------------------------------
    // Lookup helpers
    // -------------------------------------------------------------------------

    private fun findReference(
        className: String,
        refName: String,
        metamodelInfo: MetamodelInfo
    ): ReferenceInfo? =
        metamodelInfo.referencesForNode(className).find { it.refName == refName }
}

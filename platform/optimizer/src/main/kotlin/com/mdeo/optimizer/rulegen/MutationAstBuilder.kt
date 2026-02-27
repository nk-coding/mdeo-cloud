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
 * REPORT: CHANGE and SWAP rules ideally require a `where oldTarget != newTarget`
 * guard to avoid matching the same target for both roles.  TypedWhereClause
 * expressions require fully indexed TypedIdentifierExpression objects (with scope
 * indices assigned by the compiler).  Building those indices without a compiler
 * pass is fragile, so the where-clause is omitted.  The "forbid newLink" NAC still
 * prevents creating a link that already exists, which guards the degenerate case
 * (but does not strictly prevent oldTarget == newTarget when the forbid is on the
 * NOT-YET-existing target link).  Mark as known limitation.
 *
 * REPORT: CREATE_LB_REPAIR, CREATE_LB_REPAIR_MANY, DELETE_LB_REPAIR and
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
        val statements = when (spec.type) {
            RepairSpecType.ADD ->
                buildAddEdge(spec, metamodelInfo)
            RepairSpecType.REMOVE ->
                buildRemoveEdge(spec, metamodelInfo)
            RepairSpecType.CHANGE, RepairSpecType.SWAP ->
                buildChangeEdge(spec, metamodelInfo)
            RepairSpecType.CREATE,
            RepairSpecType.CREATE_LB_REPAIR,
            RepairSpecType.CREATE_LB_REPAIR_MANY ->
                buildCreateNode(spec, createContext)
            RepairSpecType.DELETE,
            RepairSpecType.DELETE_LB_REPAIR,
            RepairSpecType.DELETE_LB_REPAIR_MANY ->
                buildDeleteNode(spec)
        } ?: return null

        return TypedAst(
            types = emptyList(),
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
     * }
     * ```
     *
     * Returns null when [spec.edgeName] is null (no reference to add).
     */
    private fun buildAddEdge(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo
    ): List<TypedMatchStatement>? {
        val refName = spec.edgeName ?: return null
        val refInfo = findReference(spec.nodeName, refName, metamodelInfo) ?: return null

        val sourceVar = "source"
        val targetVar = "target"

        val pattern = TypedPattern(
            elements = listOf(
                objectElement(modifier = null, name = sourceVar, className = spec.nodeName),
                objectElement(modifier = null, name = targetVar, className = refInfo.targetClass),
                linkElement(modifier = "forbid", srcName = sourceVar, refName = refName, tgtName = targetVar),
                linkElement(modifier = "create", srcName = sourceVar, refName = refName, tgtName = targetVar)
            )
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
     *   source.refName -- target          // must exist
     *   delete source.refName -- target   // effect: remove the link
     * }
     * ```
     *
     * Returns null when [spec.edgeName] is null.
     */
    private fun buildRemoveEdge(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo
    ): List<TypedMatchStatement>? {
        val refName = spec.edgeName ?: return null
        val refInfo = findReference(spec.nodeName, refName, metamodelInfo) ?: return null

        val sourceVar = "source"
        val targetVar = "target"

        val pattern = TypedPattern(
            elements = listOf(
                objectElement(modifier = null, name = sourceVar, className = spec.nodeName),
                objectElement(modifier = null, name = targetVar, className = refInfo.targetClass),
                linkElement(modifier = "delete", srcName = sourceVar, refName = refName, tgtName = targetVar)
            )
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
     *   source.refName -- oldTarget         // existing link to be replaced
     *   newTarget: TargetClass {}
     *   forbid source.refName -- newTarget  // NAC: new link must not exist yet
     *   delete source.refName -- oldTarget  // remove old link
     *   create source.refName -- newTarget  // add new link
     * }
     * ```
     *
     * // The forbid NAC on (source → newTarget) is sufficient: when oldTarget == newTarget,
     * // the already-matched link satisfies the forbid condition, preventing the degenerate
     * // no-op application. No explicit where-clause guard is needed here.
     *
     * Returns null when [spec.edgeName] is null.
     */
    private fun buildChangeEdge(
        spec: RepairSpec,
        metamodelInfo: MetamodelInfo
    ): List<TypedMatchStatement>? {
        val refName = spec.edgeName ?: return null
        val refInfo = findReference(spec.nodeName, refName, metamodelInfo) ?: return null

        val sourceVar = "source"
        val oldTargetVar = "oldTarget"
        val newTargetVar = "newTarget"

        val pattern = TypedPattern(
            elements = listOf(
                objectElement(modifier = null, name = sourceVar, className = spec.nodeName),
                objectElement(modifier = null, name = oldTargetVar, className = refInfo.targetClass),
                objectElement(modifier = null, name = newTargetVar, className = refInfo.targetClass),
                linkElement(modifier = "forbid", srcName = sourceVar, refName = refName, tgtName = newTargetVar),
                linkElement(modifier = "delete", srcName = sourceVar, refName = refName, tgtName = oldTargetVar),
                linkElement(modifier = "create", srcName = sourceVar, refName = refName, tgtName = newTargetVar)
            )
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
     * REPORT: CREATE_LB_REPAIR and CREATE_LB_REPAIR_MANY variants are downgraded to this
     * plain CREATE; LB repair chain is not representable in the custom DSL.
     *
     * @param createContext Optional (containerClass, containerRef) for containment creation.
     */
    private fun buildCreateNode(
        spec: RepairSpec,
        createContext: Pair<String, String>?
    ): List<TypedMatchStatement>? {
        val newNodeVar = "newNode"

        return if (createContext == null) {
            // Standalone create (graph vertex without container)
            val pattern = TypedPattern(
                elements = listOf(
                    objectElement(modifier = "create", name = newNodeVar, className = spec.nodeName)
                )
            )
            listOf(TypedMatchStatement(pattern = pattern))
        } else {
            val (containerClass, containerRef) = createContext
            val containerVar = "container"
            val pattern = TypedPattern(
                elements = listOf(
                    objectElement(modifier = null, name = containerVar, className = containerClass),
                    objectElement(modifier = "create", name = newNodeVar, className = spec.nodeName),
                    linkElement(
                        modifier = "create",
                        srcName = containerVar,
                        refName = containerRef,
                        tgtName = newNodeVar
                    )
                )
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
     * Pattern:
     * ```
     * match {
     *   node: NodeClass {}      // match (find it first)
     *   delete node             // then delete (reference to matched node, no className)
     * }
     * ```
     *
     * Two separate elements with the same name are required: the first (modifier=null)
     * performs the graph search; the second (modifier="delete", className=null) marks
     * the matched instance for deletion.
     *
     * REPORT: DELETE_LB_REPAIR and DELETE_LB_REPAIR_MANY are downgraded to this plain
     * DELETE rule.
     */
    private fun buildDeleteNode(spec: RepairSpec): List<TypedMatchStatement>? {
        val nodeVar = "node"

        val pattern = TypedPattern(
            elements = listOf(
                // First: match the node
                objectElement(modifier = null, name = nodeVar, className = spec.nodeName),
                // Second: delete the matched node (className=null = reference to already-matched)
                TypedPatternObjectInstanceElement(
                    objectInstance = TypedPatternObjectInstance(
                        modifier = "delete",
                        name = nodeVar,
                        className = null,
                        properties = emptyList()
                    )
                )
            )
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

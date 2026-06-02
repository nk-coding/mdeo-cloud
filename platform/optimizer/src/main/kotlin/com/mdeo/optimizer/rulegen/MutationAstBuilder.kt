package com.mdeo.optimizer.rulegen

import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.TypedPattern
import com.mdeo.modeltransformation.ast.patterns.TypedPatternElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLink
import com.mdeo.modeltransformation.ast.patterns.TypedPatternLinkEnd
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstance
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement

/**
 * Constructs [TypedAst] transformation rules from [RepairSpec] descriptors.
 *
 * Each build method corresponds to one of the seven [RepairSpecType] values and emits a single
 * `match` statement whose pattern elements (object instances, links, where clauses) implement the
 * appropriate consistency-preserving operator.
 *
 * ### Pattern variable naming conventions
 * | Role                   | Variable name          |
 * |------------------------|------------------------|
 * | Source / owner node    | `source`               |
 * | Target / contained node| `target`               |
 * | Container node (CREATE)| `container`            |
 * | Node being deleted     | `node`                 |
 * | Old edge target        | `oldTarget`            |
 * | New edge target        | `newTarget`            |
 * | SWAP node 2            | `otherSource`          |
 * | SWAP target 2          | `otherTarget`          |
 * | Guard neighbour (DELETE)| `neighbor_<refName>`  |
 * | Donor node (LB repair) | `donor`                |
 */
object MutationAstBuilder {

    /**
     * Builds a [TypedAst] for the given [spec].
     *
     * @param name          Human-readable rule name (not stored in the AST itself).
     * @param spec          The repair spec describing the mutation.
     * @param metamodelPath Path to the metamodel file (stored in [TypedAst.metamodelPath]).
     * @param info          Metamodel structural information.
     * @param createContext For [RepairSpecType.CREATE] rules: the `(containerClass, refName)` pair
     *                      that establishes the containment context, or `null` for a standalone
     *                      node creation.
     */
    fun build(
        name: String,
        spec: RepairSpec,
        metamodelPath: String,
        info: MetamodelInfo,
        createContext: Pair<String, String>? = null
    ): TypedAst {
        val guardBuilder = MultiplicityGuardBuilder(metamodelPath)
        val elements: List<TypedPatternElement> = when (spec.type) {
            RepairSpecType.CREATE              -> buildCreate(spec, info, guardBuilder, createContext)
            RepairSpecType.CREATE_LB_REPAIR    -> buildCreateLbRepair(spec, info, guardBuilder, createContext)
            RepairSpecType.CREATE_LB_REPAIR_MULTI -> buildCreateLbRepairMulti(spec, info, guardBuilder, createContext)
            RepairSpecType.DELETE              -> buildDelete(spec, info, guardBuilder)
            RepairSpecType.DELETE_REPAIR_SINGLE -> buildDeleteRepairSingle(spec, info, guardBuilder)
            RepairSpecType.DELETE_REPAIR_MULTI  -> buildDeleteRepairMulti(spec, info, guardBuilder)
            RepairSpecType.ADD                 -> buildAdd(spec, info, guardBuilder)
            RepairSpecType.REMOVE              -> buildRemove(spec, info, guardBuilder)
            RepairSpecType.CHANGE              -> buildChange(spec, info, guardBuilder)
            RepairSpecType.SWAP                -> buildSwap(spec, info, guardBuilder)
        }
        return TypedAst(
            types = guardBuilder.getTypes(),
            metamodelPath = metamodelPath,
            statements = listOf(TypedMatchStatement(pattern = TypedPattern(elements = elements)))
        )
    }

    /**
     * Builds a single [TypedAst] that merges **several** repair specs from one combination into one
     * rule (aligned with the Java reference, whose `CreateNodeRuleCommand` /`DeleteNodeRuleCommand`
     * apply every [RepairSpec] in a combination to the same created/deleted node).
     *
     * The [activeSpecs] must all belong to the same family (all CREATE_LB_REPAIR(_MULTI) or all
     * DELETE_REPAIR_SINGLE/MULTI) and act on distinct references of the same class. This entry point
     * is used for genuine multi-reference combinations; single- and zero-repair combinations are
     * built through the per-spec [build] above to preserve their exact structure.
     */
    fun buildCombination(
        activeSpecs: List<RepairSpec>,
        metamodelPath: String,
        info: MetamodelInfo,
        createContext: Pair<String, String>? = null
    ): TypedAst {
        require(activeSpecs.isNotEmpty()) { "buildCombination requires at least one repair spec" }
        val guardBuilder = MultiplicityGuardBuilder(metamodelPath)
        val isCreate = activeSpecs.first().type == RepairSpecType.CREATE_LB_REPAIR ||
            activeSpecs.first().type == RepairSpecType.CREATE_LB_REPAIR_MULTI
        val elements = if (isCreate) {
            buildCreateLbRepairCombined(activeSpecs, info, guardBuilder, createContext)
        } else {
            buildDeleteRepairCombined(activeSpecs, info, guardBuilder)
        }
        return TypedAst(
            types = guardBuilder.getTypes(),
            metamodelPath = metamodelPath,
            statements = listOf(TypedMatchStatement(pattern = TypedPattern(elements = elements)))
        )
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    private fun buildCreate(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        createContext: Pair<String, String>?
    ): List<TypedPatternElement> {
        val elements = mutableListOf<TypedPatternElement>()

        if (createContext != null) {
            val (containerClass, refName) = createContext

            // Match the container
            elements += objectInstance(modifier = null, name = "container", className = containerClass)

            // Create the new node
            elements += objectInstance(modifier = "create", name = "newNode", className = spec.className)

            // Create containment link
            elements += linkElement(
                modifier = "create",
                sourceObj = "container", sourceClassName = containerClass, sourceRef = refName,
                targetObj = "newNode", info = info
            )

            // Upper-bound guard on the container's collection if bounded
            val containerRef = info.referencesForNode(containerClass).find { it.refName == refName }
            if (containerRef != null && containerRef.upper != -1) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = "container",
                    varClassName = containerClass,
                    refName = refName,
                    targetClassName = spec.className,
                    upperBound = containerRef.upper
                )
            }
        } else {
            // Standalone creation: just create the node
            elements += objectInstance(modifier = "create", name = "newNode", className = spec.className)
        }

        appendMandatoryOutgoingReferences(
            elements = elements,
            spec = spec,
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = emptySet()
        )

        return elements
    }

    // =========================================================================
    // CREATE_LB_REPAIR
    // =========================================================================

    /**
     * Builds a rule that:
     * 1. Matches a donor node that owns more than the minimum required connections.
     * 2. Creates the new node.
     * 3. Moves one connection from the donor to the new node.
     */
    private fun buildCreateLbRepair(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        createContext: Pair<String, String>? = null
    ): List<TypedPatternElement> {
        val refName = spec.edgeName
            ?: return buildCreate(spec, info, guardBuilder, null)

        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return buildCreate(spec, info, guardBuilder, null)

        val elements = mutableListOf<TypedPatternElement>()

        // Match the donor (existing node of same class)
        elements += objectInstance(modifier = null, name = "donor", className = spec.className)

        // Match the target being transferred
        elements += objectInstance(modifier = null, name = "target", className = ref.targetClass)

        // Match existing link: donor → target
        elements += linkElement(
            modifier = null,
            sourceObj = "donor", sourceClassName = spec.className, sourceRef = refName,
            targetObj = "target", info = info
        )

        // Create new source node
        elements += objectInstance(modifier = "create", name = "newNode", className = spec.className)

        if (createContext != null) {
            val (containerClass, refName) = createContext

            // Match the container for the new node
            elements += objectInstance(modifier = null, name = "container", className = containerClass)

            // Create the containment link alongside the LB repair
            elements += linkElement(
                modifier = "create",
                sourceObj = "container", sourceClassName = containerClass, sourceRef = refName,
                targetObj = "newNode", info = info
            )

            val containerRef = info.referencesForNode(containerClass).find { it.refName == refName }
            if (containerRef != null && containerRef.upper != -1) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = "container",
                    varClassName = containerClass,
                    refName = refName,
                    targetClassName = spec.className,
                    upperBound = containerRef.upper
                )
            }
        }

        // Delete old link: donor → target
        elements += linkElement(
            modifier = "delete",
            sourceObj = "donor", sourceClassName = spec.className, sourceRef = refName,
            targetObj = "target", info = info
        )

        // Create new link: newNode → target
        elements += linkElement(
            modifier = "create",
            sourceObj = "newNode", sourceClassName = spec.className, sourceRef = refName,
            targetObj = "target", info = info
        )

        // Guard: donor still has enough after losing one connection
        if (ref.lower > 0) {
            elements += guardBuilder.buildLowerBoundGuard(
                varName = "donor",
                varClassName = spec.className,
                refName = refName,
                targetClassName = ref.targetClass,
                lowerBound = ref.lower
            )
        }

        appendMandatoryOutgoingReferences(
            elements = elements,
            spec = spec,
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = setOf(refName)
        )

        return elements
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Builds a DELETE rule with neighbour guards for every reference whose opposite end has a
     * positive lower bound.
     *
     * Pattern structure:
    * - `node` (modifier=delete, className set)
     * - For each guarded reference: match link + neighbour object + where clause
     */
    private fun buildDelete(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val elements = mutableListOf<TypedPatternElement>()

        // Combined match+delete on the same object instance.
        elements += objectInstance(modifier = "delete", name = "node", className = spec.className)

        appendDeleteGuards(
            elements = elements,
            className = spec.className,
            nodeVarName = "node",
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = emptySet()
        )

        return elements
    }

    private fun appendDeleteGuards(
        elements: MutableList<TypedPatternElement>,
        className: String,
        nodeVarName: String,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        skipRefNames: Set<String>
    ) {
        val usedNeighborNames = elements
            .filterIsInstance<TypedPatternObjectInstanceElement>()
            .map { it.objectInstance.name }
            .toMutableSet()

        // Add guards for every reference whose target still needs a minimum number of back-links
        val refs = info.referencesForNode(className)
        for (ref in refs) {
            if (ref.refName in skipRefNames) continue
            // For a guard, we look at ref's opposite's lower bound from the target's perspective.
            // The guard checks: if we delete this node, the target (neighbor) must still have
            // enough connections of the kind described by the opposite end of our reference.
            //
            // The opposite end name (from the target node's perspective looking back) is the
            // "opposite" reference. We can find it from the target class's references.
            val targetRefs = info.referencesForNode(ref.targetClass)
            val backRef = targetRefs.find { it.targetClass == className && it.isReverse == !ref.isReverse }

            if (backRef != null && backRef.lower > 0) {
                val baseNeighborName = "neighbor_${ref.refName}"
                var neighborName = baseNeighborName
                var suffix = 2
                while (!usedNeighborNames.add(neighborName)) {
                    neighborName = "${baseNeighborName}_$suffix"
                    suffix++
                }

                // Match the neighbour object
                elements += objectInstance(modifier = null, name = neighborName, className = ref.targetClass)

                // Match link connecting node → neighbour via this reference
                elements += linkElement(
                    modifier = null,
                    sourceObj = nodeVarName, sourceClassName = className, sourceRef = ref.refName,
                    targetObj = neighborName, info = info
                )

                // Where clause: neighbour's back-reference count > lower bound
                elements += guardBuilder.buildLowerBoundGuard(
                    varName = neighborName,
                    varClassName = ref.targetClass,
                    refName = backRef.refName,
                    targetClassName = className,
                    lowerBound = backRef.lower
                )
            }
        }
    }

    // =========================================================================
    // ADD
    // =========================================================================

    /**
     * Builds an ADD rule:
     * - `source` (match) + `target` (match)
     * - forbid link: NAC ensuring edge doesn't already exist
     * - create link: creates the new edge
     * - optional upper-bound guard on source
     * - optional upper-bound guard on target (if opposite has bounded upper)
     */
    private fun buildAdd(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val refName = requireEdgeName(spec) ?: return emptyList()
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return emptyList()

        val elements = mutableListOf<TypedPatternElement>()

        // Match source and target nodes
        elements += objectInstance(modifier = null, name = "source", className = spec.className)
        elements += objectInstance(modifier = null, name = "target", className = ref.targetClass)

        // NAC: don't add if already connected
        elements += linkElement(
            modifier = "forbid",
            sourceObj = "source",
            sourceClassName = spec.className,
            sourceRef = refName,
            targetObj = "target",
            info = info
        )

        // Create the new edge
        elements += linkElement(
            modifier = "create",
            sourceObj = "source",
            sourceClassName = spec.className,
            sourceRef = refName,
            targetObj = "target",
            info = info
        )

        // Upper-bound guard on source (source.refName.size() < upper)
        if (ref.upper != -1) {
            elements += guardBuilder.buildUpperBoundGuard(
                varName = "source",
                varClassName = spec.className,
                refName = refName,
                targetClassName = ref.targetClass,
                upperBound = ref.upper
            )
        }

        // Upper-bound guard on target's opposite (target.oppositeRefName.size() < opposite.upper)
        if (ref.opposite != null && ref.opposite.upper != -1) {
            val oppositeRefName = findOppositeRefName(info, ref.targetClass, spec.className, !ref.isReverse)
            if (oppositeRefName != null) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = "target",
                    varClassName = ref.targetClass,
                    refName = oppositeRefName,
                    targetClassName = spec.className,
                    upperBound = ref.opposite.upper
                )
            }
        }

        return elements
    }

    // =========================================================================
    // REMOVE
    // =========================================================================

    /**
     * Builds a REMOVE rule:
     * - `source` (match) + `target` (match)
     * - delete link (implicitly matches and deletes the edge)
     * - optional lower-bound guard on source
     * - optional lower-bound guard on target (if opposite has positive lower)
     */
    private fun buildRemove(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val refName = requireEdgeName(spec) ?: return emptyList()
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return emptyList()

        val elements = mutableListOf<TypedPatternElement>()

        // Match source and target
        elements += objectInstance(modifier = null, name = "source", className = spec.className)
        elements += objectInstance(modifier = null, name = "target", className = ref.targetClass)

        // Delete the existing edge (combined match + delete)
        elements += linkElement(
            modifier = "delete",
            sourceObj = "source",
            sourceClassName = spec.className,
            sourceRef = refName,
            targetObj = "target",
            info = info
        )

        // Lower-bound guard on source (source.refName.size() > lower)
        if (ref.lower > 0) {
            elements += guardBuilder.buildLowerBoundGuard(
                varName = "source",
                varClassName = spec.className,
                refName = refName,
                targetClassName = ref.targetClass,
                lowerBound = ref.lower
            )
        }

        // Lower-bound guard on target's opposite
        if (ref.opposite != null && ref.opposite.lower > 0) {
            val oppositeRefName = findOppositeRefName(info, ref.targetClass, spec.className, !ref.isReverse)
            if (oppositeRefName != null) {
                elements += guardBuilder.buildLowerBoundGuard(
                    varName = "target",
                    varClassName = ref.targetClass,
                    refName = oppositeRefName,
                    targetClassName = spec.className,
                    lowerBound = ref.opposite.lower
                )
            }
        }

        return elements
    }

    // =========================================================================
    // CHANGE
    // =========================================================================

    /**
     * Builds a CHANGE rule (retarget an existing edge):
     * - `source` (match) + `oldTarget` (match) + `newTarget` (match)
     * - delete link: source → oldTarget
     * - forbid link: source → newTarget (NAC)
     * - create link: source → newTarget
     * - upper-bound guard on newTarget's back-reference (newTarget.opp.size() < upper)
     * - lower-bound guard on oldTarget's back-reference (oldTarget.opp.size() > lower)
     */
    private fun buildChange(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val refName = requireEdgeName(spec) ?: return emptyList()
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return emptyList()

        val elements = mutableListOf<TypedPatternElement>()

        // Match nodes
        elements += objectInstance(modifier = null, name = "source",    className = spec.className)
        elements += objectInstance(modifier = null, name = "oldTarget", className = ref.targetClass)
        elements += objectInstance(modifier = null, name = "newTarget", className = ref.targetClass)

        // Delete existing edge
        elements += linkElement(
            modifier = "delete",
            sourceObj = "source",
            sourceClassName = spec.className,
            sourceRef = refName,
            targetObj = "oldTarget",
            info = info
        )

        // NAC: new target not already connected
        elements += linkElement(
            modifier = "forbid",
            sourceObj = "source",
            sourceClassName = spec.className,
            sourceRef = refName,
            targetObj = "newTarget",
            info = info
        )

        // Create new edge
        elements += linkElement(
            modifier = "create",
            sourceObj = "source",
            sourceClassName = spec.className,
            sourceRef = refName,
            targetObj = "newTarget",
            info = info
        )

        // Guards on the opposite side if the opposite has bounded multiplicity
        if (ref.opposite != null) {
            val oppositeRefName = findOppositeRefName(info, ref.targetClass, spec.className, !ref.isReverse)
            if (oppositeRefName != null) {
                // Upper-bound guard on newTarget (newTarget.opp.size() < upper)
                if (ref.opposite.upper != -1) {
                    elements += guardBuilder.buildUpperBoundGuard(
                        varName = "newTarget",
                        varClassName = ref.targetClass,
                        refName = oppositeRefName,
                        targetClassName = spec.className,
                        upperBound = ref.opposite.upper
                    )
                }
                // Lower-bound guard on oldTarget (oldTarget.opp.size() > lower)
                if (ref.opposite.lower > 0) {
                    elements += guardBuilder.buildLowerBoundGuard(
                        varName = "oldTarget",
                        varClassName = ref.targetClass,
                        refName = oppositeRefName,
                        targetClassName = spec.className,
                        lowerBound = ref.opposite.lower
                    )
                }
            }
        }

        return elements
    }

    // =========================================================================
    // SWAP
    // =========================================================================

    /**
     * Builds a SWAP rule (exchange targets between two parallel edges):
     * - `source` / `otherSource` (both of [spec.className], match)
     * - `target` / `otherTarget` (both of target class, match)
     * - match links binding pairs
     * - delete + create links performing the swap
     * - NAC links preventing trivial self-swaps
     */
    private fun buildSwap(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val refName = requireEdgeName(spec) ?: return emptyList()
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return emptyList()

        val elements = mutableListOf<TypedPatternElement>()

        // Match two source nodes and their current targets
        elements += objectInstance(modifier = null, name = "source",      className = spec.className)
        elements += objectInstance(modifier = null, name = "otherSource", className = spec.className)
        elements += objectInstance(modifier = null, name = "target",      className = ref.targetClass)
        elements += objectInstance(modifier = null, name = "otherTarget", className = ref.targetClass)

        // Match links establishing which source owns which target
        elements += linkElement(modifier = null, sourceObj = "source",      sourceClassName = spec.className, sourceRef = refName, targetObj = "target", info = info)
        elements += linkElement(modifier = null, sourceObj = "otherSource", sourceClassName = spec.className, sourceRef = refName, targetObj = "otherTarget", info = info)

        // Delete old links
        elements += linkElement(modifier = "delete", sourceObj = "source",      sourceClassName = spec.className, sourceRef = refName, targetObj = "target", info = info)
        elements += linkElement(modifier = "delete", sourceObj = "otherSource", sourceClassName = spec.className, sourceRef = refName, targetObj = "otherTarget", info = info)

        // Create crossed links
        elements += linkElement(modifier = "create", sourceObj = "source",      sourceClassName = spec.className, sourceRef = refName, targetObj = "otherTarget", info = info)
        elements += linkElement(modifier = "create", sourceObj = "otherSource", sourceClassName = spec.className, sourceRef = refName, targetObj = "target", info = info)

        // NAC: ensure targets are different (avoid trivial no-op)
        elements += linkElement(modifier = "forbid", sourceObj = "source", sourceClassName = spec.className, sourceRef = refName, targetObj = "otherTarget", info = info)

        // No multiplicity guards – SWAP is cardinality-preserving by construction.

        return elements
    }

    // =========================================================================
    // CREATE_LB_REPAIR_MULTI
    // =========================================================================

    /**
     * Builds a rule that simultaneously steals one target from each of [n] distinct donor nodes
     * to satisfy the new node's lower-bound requirement of n connections (n > 1).
     *
     * Pattern structure for lower=n:
     * - `donor_i` (match, n instances) + `target_i` (match, n instances)
     * - n match links: `donor_i.ref → target_i`
     * - `newNode` (create)
     * - n delete links: `donor_i.ref → target_i`
     * - n create links: `newNode.ref → target_i`
     * - n lower-bound guards: `donor_i.ref.size() > n`
     */
    private fun buildCreateLbRepairMulti(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        createContext: Pair<String, String>? = null
    ): List<TypedPatternElement> {
        val refName = spec.edgeName ?: return buildCreate(spec, info, guardBuilder, null)
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return buildCreate(spec, info, guardBuilder, createContext)
        val n = ref.lower
        if (n <= 1) return buildCreateLbRepair(spec, info, guardBuilder, createContext)

        val elements = mutableListOf<TypedPatternElement>()

        // n donor and target match objects
        for (i in 1..n) {
            elements += objectInstance(modifier = null, name = "donor_$i", className = spec.className)
            elements += objectInstance(modifier = null, name = "target_$i", className = ref.targetClass)
        }

        // n match links: donor_i → target_i
        for (i in 1..n) {
            elements += linkElement(modifier = null, sourceObj = "donor_$i", sourceClassName = spec.className, sourceRef = refName, targetObj = "target_$i", info = info)
        }

        // Create the new source node
        elements += objectInstance(modifier = "create", name = "newNode", className = spec.className)

        if (createContext != null) {
            val (containerClass, ctxRefName) = createContext

            elements += objectInstance(modifier = null, name = "container", className = containerClass)
            elements += linkElement(
                modifier = "create",
                sourceObj = "container", sourceClassName = containerClass, sourceRef = ctxRefName,
                targetObj = "newNode", info = info
            )

            val containerRef = info.referencesForNode(containerClass).find { it.refName == ctxRefName }
            if (containerRef != null && containerRef.upper != -1) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = "container",
                    varClassName = containerClass,
                    refName = ctxRefName,
                    targetClassName = spec.className,
                    upperBound = containerRef.upper
                )
            }
        }

        // n delete links: donor_i → target_i (steal)
        for (i in 1..n) {
            elements += linkElement(modifier = "delete", sourceObj = "donor_$i", sourceClassName = spec.className, sourceRef = refName, targetObj = "target_$i", info = info)
        }

        // n create links: newNode → target_i (give to new node)
        for (i in 1..n) {
            elements += linkElement(modifier = "create", sourceObj = "newNode", sourceClassName = spec.className, sourceRef = refName, targetObj = "target_$i", info = info)
        }

        // n lower-bound guards: each donor must still have enough after losing one
        for (i in 1..n) {
            elements += guardBuilder.buildLowerBoundGuard(
                varName = "donor_$i",
                varClassName = spec.className,
                refName = refName,
                targetClassName = ref.targetClass,
                lowerBound = n
            )
        }

        appendMandatoryOutgoingReferences(
            elements = elements,
            spec = spec,
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = setOf(refName)
        )

        return elements
    }

    // =========================================================================
    // DELETE_REPAIR_SINGLE
    // =========================================================================

    /**
     * Builds a rule that deletes a node and simultaneously moves ONE of its neighbours (whose
     * opposite has fixed cardinality k=l≥1) to a single replacement node of the same class.
     *
     * Pattern structure:
    * - `node` (modifier=delete, className set)
     * - `neighbor_{refName}` (match) + match link `node.ref → neighbor`
     * - `other_{refName}` (match): the replacement source
     * - forbid link: `other.ref → neighbor` (NAC – other is not already connected)
     * - create link: `other.ref → neighbor` (reconnect)
     * - optional upper-bound guard on `other_{refName}` if `ref.upper` is bounded
     */
    private fun buildDeleteRepairSingle(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val refName = spec.edgeName ?: return buildDelete(spec, info, guardBuilder)
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return buildDelete(spec, info, guardBuilder)

        val elements = mutableListOf<TypedPatternElement>()
        val neighborName = "neighbor_$refName"
        val otherName = "other_$refName"

        // Combined match+delete on the node.
        elements += objectInstance(modifier = "delete", name = "node", className = spec.className)

        // Match the specific neighbour being repaired
        elements += objectInstance(modifier = null, name = neighborName, className = ref.targetClass)
        elements += linkElement(modifier = null, sourceObj = "node", sourceClassName = spec.className, sourceRef = refName, targetObj = neighborName, info = info)

        // Match the replacement source node
        elements += objectInstance(modifier = null, name = otherName, className = spec.className)

        // NAC: replacement not already connected to the neighbour
        elements += linkElement(modifier = "forbid", sourceObj = otherName, sourceClassName = spec.className, sourceRef = refName, targetObj = neighborName, info = info)

        // Create the new connection: replacement → neighbour
        elements += linkElement(modifier = "create", sourceObj = otherName, sourceClassName = spec.className, sourceRef = refName, targetObj = neighborName, info = info)

        // Upper-bound guard on the replacement if the reference has a bounded upper
        if (ref.upper != -1) {
            elements += guardBuilder.buildUpperBoundGuard(
                varName = otherName,
                varClassName = spec.className,
                refName = refName,
                targetClassName = ref.targetClass,
                upperBound = ref.upper
            )
        }

        appendDeleteGuards(
            elements = elements,
            className = spec.className,
            nodeVarName = "node",
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = setOf(refName)
        )

        return elements
    }

    // =========================================================================
    // DELETE_REPAIR_MULTI
    // =========================================================================

    /**
     * Builds a rule that deletes a node and simultaneously redistributes k neighbours (whose
     * opposite has fixed cardinality k=l>1) each to a different replacement node.
     *
     * Pattern structure for k neighbours:
    * - `node` (modifier=delete, className set)
     * - k × `neighbor_{refName}_{i}` (match) + k × match links `node.ref → neighbor_i`
     * - k × `other_{refName}_{i}` (match, distinct replacement sources)
     * - k × forbid links: `other_i.ref → neighbor_i` (NAC)
     * - k × create links: `other_i.ref → neighbor_i` (reconnect)
     * - k × optional upper-bound guards on `other_i` if `ref.upper` is bounded
     */
    private fun buildDeleteRepairMulti(
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val refName = spec.edgeName ?: return buildDelete(spec, info, guardBuilder)
        val ref = info.referencesForNode(spec.className).find { it.refName == refName }
            ?: return buildDelete(spec, info, guardBuilder)

        // Look up k from the back-reference's fixed cardinality
        val backRef = info.referencesForNode(ref.targetClass)
            .find { it.targetClass == spec.className && it.isReverse == !ref.isReverse }
            ?: return buildDeleteRepairSingle(spec, info, guardBuilder)
        val k = backRef.lower
        if (k <= 1) return buildDeleteRepairSingle(spec, info, guardBuilder)

        val elements = mutableListOf<TypedPatternElement>()

        // Combined match+delete on the node.
        elements += objectInstance(modifier = "delete", name = "node", className = spec.className)

        // k neighbour match objects and links
        for (i in 1..k) {
            val neighborName = "neighbor_${refName}_$i"
            elements += objectInstance(modifier = null, name = neighborName, className = ref.targetClass)
            elements += linkElement(modifier = null, sourceObj = "node", sourceClassName = spec.className, sourceRef = refName, targetObj = neighborName, info = info)
        }

        // k replacement sources, each with NAC, create-link, and optional guard
        for (i in 1..k) {
            val neighborName = "neighbor_${refName}_$i"
            val otherName = "other_${refName}_$i"
            elements += objectInstance(modifier = null, name = otherName, className = spec.className)
            elements += linkElement(modifier = "forbid", sourceObj = otherName, sourceClassName = spec.className, sourceRef = refName, targetObj = neighborName, info = info)
            elements += linkElement(modifier = "create", sourceObj = otherName, sourceClassName = spec.className, sourceRef = refName, targetObj = neighborName, info = info)
            if (ref.upper != -1) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = otherName,
                    varClassName = spec.className,
                    refName = refName,
                    targetClassName = ref.targetClass,
                    upperBound = ref.upper
                )
            }
        }

        appendDeleteGuards(
            elements = elements,
            className = spec.className,
            nodeVarName = "node",
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = setOf(refName)
        )

        return elements
    }

    // =========================================================================
    // Combined (multi-reference) repair builders
    // =========================================================================

    /**
     * Merges several donor-based CREATE lower-bound repairs onto one created node, so that a single
     * operator can satisfy positive lower bounds on multiple references at once (Java parity:
     * `CreateNodeRuleCommand.applyRepairOperations` iterating over the combination's repair specs).
     *
     * Per-reference variable names are qualified by the reference name so that distinct references
     * never collide.
     */
    private fun buildCreateLbRepairCombined(
        specs: List<RepairSpec>,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        createContext: Pair<String, String>?
    ): List<TypedPatternElement> {
        val className = specs.first().className
        val elements = mutableListOf<TypedPatternElement>()

        // Create the new node (plus its containment context, if any) once.
        appendCreatedNodeWithContext(elements, className, info, guardBuilder, createContext)

        val repairedRefs = mutableSetOf<String>()
        for (spec in specs) {
            val refName = spec.edgeName ?: continue
            val ref = info.referencesForNode(className).find { it.refName == refName } ?: continue
            repairedRefs += refName
            if (spec.type == RepairSpecType.CREATE_LB_REPAIR_MULTI && ref.lower > 1) {
                appendCreateDonorStealMulti(elements, className, ref, info, guardBuilder)
            } else {
                appendCreateDonorStealSingle(elements, className, ref, info, guardBuilder)
            }
        }

        // Remaining mandatory references (not donor-repaired) still need created children.
        appendMandatoryOutgoingReferences(
            elements = elements,
            spec = RepairSpec(className, null, RepairSpecType.CREATE),
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = repairedRefs
        )

        return elements
    }

    /** Adds the `newNode` (create) plus an optional matched `container` and containment link. */
    private fun appendCreatedNodeWithContext(
        elements: MutableList<TypedPatternElement>,
        className: String,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        createContext: Pair<String, String>?
    ) {
        elements += objectInstance(modifier = "create", name = "newNode", className = className)
        if (createContext != null) {
            val (containerClass, refName) = createContext
            elements += objectInstance(modifier = null, name = "container", className = containerClass)
            elements += linkElement(
                modifier = "create",
                sourceObj = "container", sourceClassName = containerClass, sourceRef = refName,
                targetObj = "newNode", info = info
            )
            val containerRef = info.referencesForNode(containerClass).find { it.refName == refName }
            if (containerRef != null && containerRef.upper != -1) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = "container",
                    varClassName = containerClass,
                    refName = refName,
                    targetClassName = className,
                    upperBound = containerRef.upper
                )
            }
        }
    }

    /** Single-donor steal for [ref], using reference-qualified variable names. */
    private fun appendCreateDonorStealSingle(
        elements: MutableList<TypedPatternElement>,
        className: String,
        ref: ReferenceInfo,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ) {
        val donor = "donor_${ref.refName}"
        val target = "target_${ref.refName}"
        elements += objectInstance(modifier = null, name = donor, className = className)
        elements += objectInstance(modifier = null, name = target, className = ref.targetClass)
        elements += linkElement(modifier = null, sourceObj = donor, sourceClassName = className, sourceRef = ref.refName, targetObj = target, info = info)
        elements += linkElement(modifier = "delete", sourceObj = donor, sourceClassName = className, sourceRef = ref.refName, targetObj = target, info = info)
        elements += linkElement(modifier = "create", sourceObj = "newNode", sourceClassName = className, sourceRef = ref.refName, targetObj = target, info = info)
        if (ref.lower > 0) {
            elements += guardBuilder.buildLowerBoundGuard(
                varName = donor,
                varClassName = className,
                refName = ref.refName,
                targetClassName = ref.targetClass,
                lowerBound = ref.lower
            )
        }
    }

    /** Multi-donor steal for [ref] (n = ref.lower donors), using reference-qualified names. */
    private fun appendCreateDonorStealMulti(
        elements: MutableList<TypedPatternElement>,
        className: String,
        ref: ReferenceInfo,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ) {
        val n = ref.lower
        for (i in 1..n) {
            elements += objectInstance(modifier = null, name = "donor_${ref.refName}_$i", className = className)
            elements += objectInstance(modifier = null, name = "target_${ref.refName}_$i", className = ref.targetClass)
            elements += linkElement(modifier = null, sourceObj = "donor_${ref.refName}_$i", sourceClassName = className, sourceRef = ref.refName, targetObj = "target_${ref.refName}_$i", info = info)
        }
        for (i in 1..n) {
            elements += linkElement(modifier = "delete", sourceObj = "donor_${ref.refName}_$i", sourceClassName = className, sourceRef = ref.refName, targetObj = "target_${ref.refName}_$i", info = info)
        }
        for (i in 1..n) {
            elements += linkElement(modifier = "create", sourceObj = "newNode", sourceClassName = className, sourceRef = ref.refName, targetObj = "target_${ref.refName}_$i", info = info)
        }
        for (i in 1..n) {
            elements += guardBuilder.buildLowerBoundGuard(
                varName = "donor_${ref.refName}_$i",
                varClassName = className,
                refName = ref.refName,
                targetClassName = ref.targetClass,
                lowerBound = n
            )
        }
    }

    /**
     * Merges several neighbour-reconnection DELETE repairs onto one deleted node, so that a single
     * operator can re-home the fixed-cardinality neighbours of multiple references at once (Java
     * parity: `DeleteNodeRuleCommand` applying every repair spec in the combination).
     */
    private fun buildDeleteRepairCombined(
        specs: List<RepairSpec>,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ): List<TypedPatternElement> {
        val className = specs.first().className
        val elements = mutableListOf<TypedPatternElement>()

        elements += objectInstance(modifier = "delete", name = "node", className = className)

        val repairedRefs = mutableSetOf<String>()
        for (spec in specs) {
            val refName = spec.edgeName ?: continue
            val ref = info.referencesForNode(className).find { it.refName == refName } ?: continue
            repairedRefs += refName
            val backRef = info.referencesForNode(ref.targetClass)
                .find { it.targetClass == className && it.isReverse == !ref.isReverse }
            val k = backRef?.lower ?: 1
            if (spec.type == RepairSpecType.DELETE_REPAIR_MULTI && k > 1) {
                appendDeleteReconnectMulti(elements, className, ref, k, info, guardBuilder)
            } else {
                appendDeleteReconnectSingle(elements, className, ref, info, guardBuilder)
            }
        }

        appendDeleteGuards(
            elements = elements,
            className = className,
            nodeVarName = "node",
            info = info,
            guardBuilder = guardBuilder,
            skipRefNames = repairedRefs
        )

        return elements
    }

    /** Re-homes ONE neighbour of [ref] to a replacement source (reference-qualified names). */
    private fun appendDeleteReconnectSingle(
        elements: MutableList<TypedPatternElement>,
        className: String,
        ref: ReferenceInfo,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ) {
        val neighborName = "neighbor_${ref.refName}"
        val otherName = "other_${ref.refName}"
        elements += objectInstance(modifier = null, name = neighborName, className = ref.targetClass)
        elements += linkElement(modifier = null, sourceObj = "node", sourceClassName = className, sourceRef = ref.refName, targetObj = neighborName, info = info)
        elements += objectInstance(modifier = null, name = otherName, className = className)
        elements += linkElement(modifier = "forbid", sourceObj = otherName, sourceClassName = className, sourceRef = ref.refName, targetObj = neighborName, info = info)
        elements += linkElement(modifier = "create", sourceObj = otherName, sourceClassName = className, sourceRef = ref.refName, targetObj = neighborName, info = info)
        if (ref.upper != -1) {
            elements += guardBuilder.buildUpperBoundGuard(
                varName = otherName,
                varClassName = className,
                refName = ref.refName,
                targetClassName = ref.targetClass,
                upperBound = ref.upper
            )
        }
    }

    /** Re-homes k neighbours of [ref], each to a distinct replacement source. */
    private fun appendDeleteReconnectMulti(
        elements: MutableList<TypedPatternElement>,
        className: String,
        ref: ReferenceInfo,
        k: Int,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder
    ) {
        for (i in 1..k) {
            val neighborName = "neighbor_${ref.refName}_$i"
            elements += objectInstance(modifier = null, name = neighborName, className = ref.targetClass)
            elements += linkElement(modifier = null, sourceObj = "node", sourceClassName = className, sourceRef = ref.refName, targetObj = neighborName, info = info)
        }
        for (i in 1..k) {
            val neighborName = "neighbor_${ref.refName}_$i"
            val otherName = "other_${ref.refName}_$i"
            elements += objectInstance(modifier = null, name = otherName, className = className)
            elements += linkElement(modifier = "forbid", sourceObj = otherName, sourceClassName = className, sourceRef = ref.refName, targetObj = neighborName, info = info)
            elements += linkElement(modifier = "create", sourceObj = otherName, sourceClassName = className, sourceRef = ref.refName, targetObj = neighborName, info = info)
            if (ref.upper != -1) {
                elements += guardBuilder.buildUpperBoundGuard(
                    varName = otherName,
                    varClassName = className,
                    refName = ref.refName,
                    targetClassName = ref.targetClass,
                    upperBound = ref.upper
                )
            }
        }
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    private fun objectInstance(
        modifier: String?,
        name: String,
        className: String?
    ) = TypedPatternObjectInstanceElement(
        objectInstance = TypedPatternObjectInstance(
            modifier = modifier,
            name = name,
            className = className,
            properties = emptyList()
        )
    )

    private fun linkElement(
        modifier: String?,
        sourceObj: String,
        sourceClassName: String,
        sourceRef: String,
        targetObj: String,
        info: MetamodelInfo
    ) = TypedPatternLinkElement(
        link = canonicalLink(
            modifier = modifier,
            sourceObj = sourceObj,
            sourceClassName = sourceClassName,
            sourceRef = sourceRef,
            targetObj = targetObj,
            info = info
        )
    )

    private fun canonicalLink(
        modifier: String?,
        sourceObj: String,
        sourceClassName: String,
        sourceRef: String,
        targetObj: String,
        info: MetamodelInfo
    ): TypedPatternLink {
        val canonical = canonicalAssociationLink(info, sourceClassName, sourceObj, sourceRef, targetObj)
        return TypedPatternLink(
            modifier = modifier,
            source = TypedPatternLinkEnd(objectName = canonical.sourceObj, propertyName = canonical.sourceRef),
            target = TypedPatternLinkEnd(objectName = canonical.targetObj, propertyName = canonical.targetRef)
        )
    }

    /**
     * Canonical link orientation resolved from metamodel association direction.
     */
    private data class CanonicalAssociationLink(
        val sourceObj: String,
        val sourceRef: String,
        val targetObj: String,
        val targetRef: String?
    )

    private fun canonicalAssociationLink(
        info: MetamodelInfo,
        sourceClassName: String,
        sourceObj: String,
        sourceRef: String,
        targetObj: String
    ): CanonicalAssociationLink {
        val ref = info.referencesForNode(sourceClassName).find { it.refName == sourceRef }
            ?: return CanonicalAssociationLink(
                sourceObj = sourceObj,
                sourceRef = sourceRef,
                targetObj = targetObj,
                targetRef = null
            )
        val oppositeRefName = findOppositeRefName(
            info = info,
            fromClass = ref.targetClass,
            toClass = sourceClassName,
            reverseSearch = !ref.isReverse
        )
        return if (ref.isReverse) {
            // For reverse references, canonical association direction is targetObj -> sourceObj.
            CanonicalAssociationLink(
                sourceObj = targetObj,
                sourceRef = oppositeRefName ?: sourceRef,
                targetObj = sourceObj,
                targetRef = if (oppositeRefName != null) sourceRef else null
            )
        } else {
            CanonicalAssociationLink(
                sourceObj = sourceObj,
                sourceRef = sourceRef,
                targetObj = targetObj,
                targetRef = oppositeRefName
            )
        }
    }

    private fun requireEdgeName(spec: RepairSpec): String? = spec.edgeName

    private fun appendMandatoryOutgoingReferences(
        elements: MutableList<TypedPatternElement>,
        spec: RepairSpec,
        info: MetamodelInfo,
        guardBuilder: MultiplicityGuardBuilder,
        skipRefNames: Set<String>
    ) {
        val mandatoryRefs = info.referencesForNode(spec.className)
            .filter { !it.isReverse && it.lower > 0 && !skipRefNames.contains(it.refName) }

        for (mandatoryRef in mandatoryRefs) {
            val oppositeRefName = findOppositeRefName(
                info = info,
                fromClass = mandatoryRef.targetClass,
                toClass = spec.className,
                reverseSearch = !mandatoryRef.isReverse
            )

            for (i in 1..mandatoryRef.lower) {
                val targetName = if (mandatoryRef.lower == 1) {
                    "required_${mandatoryRef.refName}"
                } else {
                    "required_${mandatoryRef.refName}_$i"
                }

                elements += objectInstance(modifier = null, name = targetName, className = mandatoryRef.targetClass)
                elements += linkElement(
                    modifier = "create",
                    sourceObj = "newNode",
                    sourceClassName = spec.className,
                    sourceRef = mandatoryRef.refName,
                    targetObj = targetName,
                    info = info
                )

                if (mandatoryRef.opposite != null && mandatoryRef.opposite.upper != -1 && oppositeRefName != null) {
                    elements += guardBuilder.buildUpperBoundGuard(
                        varName = targetName,
                        varClassName = mandatoryRef.targetClass,
                        refName = oppositeRefName,
                        targetClassName = spec.className,
                        upperBound = mandatoryRef.opposite.upper
                    )
                }
            }
        }
    }

    /**
     * Finds the name of the reference on [fromClass] that points back to [toClass].
     *
     * Used to locate the opposite reference name for guard expressions.
     *
     * @param reverseSearch When `true`, looks for a *reverse* reference (isReverse=true);
     *                      when `false`, looks for a forward reference.
     */
    private fun findOppositeRefName(
        info: MetamodelInfo,
        fromClass: String,
        toClass: String,
        reverseSearch: Boolean
    ): String? =
        info.referencesForNode(fromClass)
            .find { it.targetClass == toClass && it.isReverse == reverseSearch }
            ?.refName
}

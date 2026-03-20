package com.mdeo.optimizer.rulegen

import com.mdeo.metamodel.data.MetamodelData

/**
 * Multiplicity and naming information for the "opposite" end of a bidirectional association.
 *
 * @param refName Name of the opposite reference, or null when the opposite end is unnamed
 *                (not navigable).
 * @param lower   Lower bound of the opposite reference (0 = optional).
 * @param upper   Upper bound of the opposite reference (-1 = unbounded).
 */
data class OppositeInfo(
    val refName: String?,
    val lower: Int,
    val upper: Int
)

/**
 * A tightened multiplicity bound for a single named reference, used to create the
 * solution-space (S-type) [MetamodelInfo] variant during the second generation pass.
 *
 * @param className The class that owns the reference.
 * @param refName   The reference name whose bounds are overridden.
 * @param lower     Replacement lower bound (>= original lower).
 * @param upper     Replacement upper bound (<= original upper, or -1 for unbounded).
 */
data class MultiplicityOverride(
    val className: String,
    val refName: String,
    val lower: Int,
    val upper: Int
)

/**
 * Information about a single named reference (EReference) owned by a metamodel class.
 *
 * @param ownerClass  The class that owns this reference.
 * @param refName     The role name (EReference name) at the source end.
 * @param targetClass The class at the target end.
 * @param lower       Lower bound of this end.
 * @param upper       Upper bound of this end (-1 = unbounded).
 * @param isContainment True when the association operator indicates composition/containment.
 *                      Always false for reverse-end references (the containment arrow points
 *                      the other way).
 * @param opposite    Multiplicity of the other end, or null if unidirectional.
 * @param isReverse   True when this [ReferenceInfo] was derived from the target end of an
 *                    [AssociationData] record — i.e. the association is stored with
 *                    [ownerClass] as the *target* and [targetClass] as the *source*.  The
 *                    [refName] is then the target-end role name, and navigating it in the
 *                    DSL is expressed as `ownerObject.refName -- targetObject` exactly
 *                    as for a forward reference.
 */
data class ReferenceInfo(
    val ownerClass: String,
    val refName: String,
    val targetClass: String,
    val lower: Int,
    val upper: Int,
    val isContainment: Boolean,
    val opposite: OppositeInfo?,
    val isReverse: Boolean = false
)

/**
 * Wraps a [MetamodelData] instance, providing metamodel introspection helpers
 * needed by [SpecsGenerator] and [MutationAstBuilder].
 *
 * Equivalent to MetamodelWrapper in the original mde_optimiser rulegen library,
 * but backed by the platform's [MetamodelData] instead of an EMF EPackage.
 *
 * An optional map of [MultiplicityOverride] entries allows the second-pass (S-type)
 * rule generation to tighten lower/upper bounds for specific references without
 * mutating the shared [MetamodelData] snapshot.
 *
 * @param metamodelData The platform's resolved metamodel description.
 * @param overrides     Per-reference multiplicity overrides, keyed by (className, refName).
 *                      Defaults to empty (base metamodel multiplicities used as-is).
 */
class MetamodelInfo(
    private val metamodelData: MetamodelData,
    private val overrides: Map<Pair<String, String>, MultiplicityOverride> = emptyMap()
) {

    companion object {
        /**
         * Operator string used in [MetamodelData] to denote a containment (composition)
         * association.  Confirmed from runtime test fixtures: `operator = "<>->"`.
         */
        private const val CONTAINMENT_OPERATOR = "<>->"

        /**
         * Creates a [MetamodelInfo] with the supplied multiplicity overrides applied.
         *
         * The resulting instance returns overridden lower/upper bounds from
         * [referencesForNode] for any (className, refName) pair present in [overrides].
         *
         * @param metamodelData Base metamodel description.
         * @param overrides     List of multiplicity overrides to apply.
         */
        fun withOverrides(
            metamodelData: MetamodelData,
            overrides: List<MultiplicityOverride>
        ): MetamodelInfo = MetamodelInfo(
            metamodelData,
            overrides.associateBy { it.className to it.refName }
        )
    }

    /**
     * Returns the names of all concrete (non-abstract) classes in the metamodel.
     */
    fun classNames(): List<String> =
        metamodelData.classes.filter { !it.isAbstract }.map { it.name }

    /**
     * Returns all named references for [className], covering **both** directions of
     * bidirectional associations.
     *
     * **Forward references** — associations where [className] is the *source* end
     * (`assoc.source.className == className && assoc.source.name != null`) — are always
     * included.  Per-reference [overrides] are applied to the forward-end lower/upper bounds.
     *
     * **Reverse-end references** — associations where [className] is the *target* end
     * (`assoc.target.className == className && assoc.target.name != null`) — are also
     * included.  These represent the navigable back-pointer role (e.g. `Room.house` for a
     * `House <>-> Room` containment stored as a single [AssociationData] record).  They are
     * returned with [ReferenceInfo.isReverse] set to `true` and [ReferenceInfo.isContainment]
     * set to `false` (the containment arrow points the other way).  Overrides are not applied
     * to reverse-end references.
     *
     * **Inheritance not flattened:** Only associations explicitly listed in
     * [MetamodelData.associations] with [className] at either end are returned.
     *
     * @param className The class whose references are requested.
     * @return List of [ReferenceInfo] objects (forward references first, then reverse), may be empty.
     */
    fun referencesForNode(className: String): List<ReferenceInfo> {
        val forward = metamodelData.associations
            .filter { assoc ->
                assoc.source.className == className && assoc.source.name != null
            }
            .map { assoc ->
                val refName = assoc.source.name!!
                val override = overrides[className to refName]
                ReferenceInfo(
                    ownerClass = assoc.source.className,
                    refName = refName,
                    targetClass = assoc.target.className,
                    lower = override?.lower ?: assoc.source.multiplicity.lower,
                    upper = override?.upper ?: assoc.source.multiplicity.upper,
                    isContainment = assoc.operator == CONTAINMENT_OPERATOR,
                    opposite = if (assoc.target.name != null) OppositeInfo(
                        refName = assoc.target.name,
                        lower = assoc.target.multiplicity.lower,
                        upper = assoc.target.multiplicity.upper
                    ) else null,
                    isReverse = false
                )
            }

        val reverse = metamodelData.associations
            .filter { assoc ->
                assoc.target.className == className &&
                    assoc.target.name != null &&
                    assoc.source.className != className
            }
            .map { assoc ->
                ReferenceInfo(
                    ownerClass = assoc.target.className,
                    refName = assoc.target.name!!,
                    targetClass = assoc.source.className,
                    lower = assoc.target.multiplicity.lower,
                    upper = assoc.target.multiplicity.upper,
                    isContainment = false,
                    opposite = OppositeInfo(
                        refName = assoc.source.name,
                        lower = assoc.source.multiplicity.lower,
                        upper = assoc.source.multiplicity.upper
                    ),
                    isReverse = true
                )
            }

        return forward + reverse
    }

    /**
     * Returns all containment contexts for [containedClassName]: the set of
     * (containerClass → containmentRef) pairs where [containedClassName] can live.
     *
     * Used by CREATE rules to generate one rule per possible container context.
     */
    fun containmentContextsFor(containedClassName: String): List<Pair<String, String>> =
        metamodelData.associations
            .filter { assoc ->
                assoc.operator == CONTAINMENT_OPERATOR &&
                    assoc.target.className == containedClassName &&
                    assoc.source.name != null
            }
            .map { assoc -> assoc.source.className to assoc.source.name!! }

    /**
     * Returns true if [className] exists in the metamodel (abstract or concrete).
     */
    fun hasClass(className: String): Boolean =
        metamodelData.classes.any { it.name == className }
}

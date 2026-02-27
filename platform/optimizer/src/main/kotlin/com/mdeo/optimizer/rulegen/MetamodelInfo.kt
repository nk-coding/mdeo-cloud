package com.mdeo.optimizer.rulegen

import com.mdeo.expression.ast.types.MetamodelData

/**
 * Multiplicity information for the "opposite" end of a bidirectional association.
 *
 * @param lower Lower bound of the opposite reference (0 = optional).
 * @param upper Upper bound of the opposite reference (-1 = unbounded).
 */
data class OppositeInfo(
    val lower: Int,
    val upper: Int
)

/**
 * Information about a single named reference (EReference) owned by a metamodel class.
 *
 * @param ownerClass  The class that owns this reference.
 * @param refName     The role name (EReference name) at the source end.
 * @param targetClass The class at the target end.
 * @param lower       Lower bound at the source end.
 * @param upper       Upper bound at the source end (-1 = unbounded).
 * @param isContainment True when the association operator indicates composition/containment.
 * @param opposite    Multiplicity of the reverse end, or null if unidirectional.
 */
data class ReferenceInfo(
    val ownerClass: String,
    val refName: String,
    val targetClass: String,
    val lower: Int,
    val upper: Int,
    val isContainment: Boolean,
    val opposite: OppositeInfo?
)

/**
 * Wraps a [MetamodelData] instance, providing metamodel introspection helpers
 * needed by [SpecsGenerator] and [MutationAstBuilder].
 *
 * Equivalent to MetamodelWrapper in the original mde_optimiser rulegen library,
 * but backed by the platform's [MetamodelData] instead of an EMF EPackage.
 *
 * Multiplicity refinements (analogous to the Java Multiplicity overrides) are not
 * supported in this port — the associations from MetamodelData are used as-is.
 *
 * @param metamodelData The platform's resolved metamodel description.
 */
class MetamodelInfo(private val metamodelData: MetamodelData) {

    companion object {
        /**
         * Operator string used in [MetamodelData] to denote a containment (composition)
         * association.  Confirmed from runtime test fixtures: `operator = "<>->"`.
         */
        private const val CONTAINMENT_OPERATOR = "<>->"
    }

    /**
     * Returns the names of all concrete (non-abstract) classes in the metamodel.
     */
    fun classNames(): List<String> =
        metamodelData.classes.filter { !it.isAbstract }.map { it.name }

    /**
     * Returns all named outgoing references defined for [className].
     *
     * **Source-end only:** This method returns only associations where [className] is
     * the *source* end (i.e. `assoc.source.className == className`). Bidirectional
     * associations stored as a single record will **not** return a reverse reference
     * for the target class — the target class must explicitly be the source of a
     * separate association record to appear here.
     *
     * **Inheritance not flattened:** Only associations explicitly declared with
     * [className] as their source are returned; references inherited from parent
     * classes are not included.
     *
     * A reference is "named" when the source end of the association has a non-null
     * role name.  Associations derived from the imported metamodels are NOT included
     * here (only the top-level metamodel is queried).
     *
     * @param className The class whose outgoing source-end references are requested.
     * @return List of [ReferenceInfo] objects, may be empty.
     */
    fun referencesForNode(className: String): List<ReferenceInfo> =
        metamodelData.associations
            .filter { assoc ->
                assoc.source.className == className && assoc.source.name != null
            }
            .map { assoc ->
                val hasOpposite = assoc.target.name != null
                ReferenceInfo(
                    ownerClass = assoc.source.className,
                    refName = assoc.source.name!!,
                    targetClass = assoc.target.className,
                    lower = assoc.source.multiplicity.lower,
                    upper = assoc.source.multiplicity.upper,
                    isContainment = assoc.operator == CONTAINMENT_OPERATOR,
                    opposite = if (hasOpposite) OppositeInfo(
                        lower = assoc.target.multiplicity.lower,
                        upper = assoc.target.multiplicity.upper
                    ) else null
                )
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

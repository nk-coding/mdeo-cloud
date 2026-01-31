package com.mdeo.expression.ast.types

import kotlinx.serialization.Serializable

/**
 * Represents a property in a typed class from the metamodel.
 *
 * Properties define simple attributes of a class with their associated types.
 * The type is referenced by index into the types array of the containing TypedAst.
 *
 * @param name The name of the property.
 * @param typeIndex Index into the types array of the TypedAst to resolve the property's type.
 */
@Serializable
data class TypedProperty(
    val name: String,
    val typeIndex: Int
)

/**
 * Represents a relation (association) in a typed class from the metamodel.
 *
 * Relations define connections between classes in the metamodel. They can be either
 * outgoing (from this class to another) or incoming (from another class to this one).
 *
 * The edge label can be computed from the property and oppositeProperty names.
 * For outgoing relations: `` `property`_`oppositeProperty` ``
 * For incoming relations: `` `oppositeProperty`_`property` ``
 *
 * @param property The property name that defines this relation on the class.
 * @param oppositeProperty The property name at the opposite end of the relation, or null if unnamed.
 * @param oppositeClassName The fully qualified name of the class at the opposite end of the relation.
 * @param isOutgoing True if this is an outgoing relation, false if incoming.
 * @param typeIndex Index into the types array of the TypedAst to resolve the relation's type.
 */
@Serializable
data class TypedRelation(
    val property: String,
    val oppositeProperty: String? = null,
    val oppositeClassName: String,
    val isOutgoing: Boolean,
    val typeIndex: Int
)

/**
 * Represents a class from the metamodel with full type information.
 *
 * A TypedClass contains all the metadata about a metamodel class including its
 * inheritance hierarchy, properties, and relations to other classes.
 *
 * @param name The fully qualified name of the class (e.g., "metamodel.Person").
 * @param package The package name where this class is defined.
 * @param superClasses Set of fully qualified names of all superclasses.
 * @param properties List of properties defined on this class.
 * @param relations List of relations (both incoming and outgoing) for this class.
 */
@Serializable
data class TypedClass(
    val name: String,
    val `package`: String,
    val superClasses: Set<String>,
    val properties: List<TypedProperty>,
    val relations: List<TypedRelation>
)

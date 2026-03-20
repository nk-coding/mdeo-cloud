package com.mdeo.metamodel

/**
 * Mapping from a property name to its generated field in an instance class.
 *
 * Single-valued properties (upper == 1) store the value directly in the field.
 * Multi-valued properties (upper != 1) store a `java.util.List`.
 * Link/association-end fields always use `java.util.Set` backing regardless of multiplicity.
 *
 * @property fieldIndex The index of the `prop_X` field (i.e. X).
 * @property fieldDescriptor The JVM type descriptor of the field.
 *                            Single-valued: the boxed element type (e.g. `Ljava/lang/Integer;`).
 *                            Multi-valued: `Ljava/util/List;`.
 * @property isCollection True for multi-valued properties (upper != 1); false for single-valued.
 * @property enumType The metamodel enum name when this property has an enum type, or null.
 * @property lower The lower bound of the multiplicity (0 or 1).
 * @property upper The upper bound of the multiplicity (1, n, or -1 for unbounded).
 * @property elementDescriptor The JVM type descriptor of individual elements
 *                            (e.g. `Ljava/lang/Integer;`, `Ljava/lang/String;`).
 */
data class PropertyFieldMapping(
    val fieldIndex: Int,
    val fieldDescriptor: String,
    val isCollection: Boolean,
    val enumType: String? = null,
    val lower: Int = 0,
    val upper: Int = -1,
    val elementDescriptor: String = "Ljava/lang/Object;"
)

/**
 * Mapping from a link/association role name to its generated field in an instance class.
 *
 * All generated link fields are backed by `java.util.Set`, regardless of multiplicity.
 *
 * @property fieldIndex The index of the `prop_X` field (i.e. X).
 * @property fieldDescriptor The JVM type descriptor of the field (always `Ljava/util/Set;`).
 * @property isOutgoing True if this end is the source end of the association.
 * @property oppositeClassName The class name at the other end of the association.
 * @property oppositeFieldName The role name at the other end, or null if unnamed.
 * @property isMultiple True — all link fields are now backed by a Set.
 * @property edgeLabel The association operator label (e.g. "--", "->").
 * @property lower The lower bound of the multiplicity (0 or 1).
 * @property upper The upper bound of the multiplicity (1, n, or -1 for unbounded).
 */
data class LinkFieldMapping(
    val fieldIndex: Int,
    val fieldDescriptor: String,
    val isOutgoing: Boolean,
    val oppositeClassName: String,
    val oppositeFieldName: String?,
    val isMultiple: Boolean,
    val edgeLabel: String,
    val lower: Int = 0,
    val upper: Int = -1
)

/**
 * Metadata for a single generated instance class.
 *
 * @property className The metamodel class name.
 * @property binaryName The JVM binary name of the generated class (dot-separated).
 * @property propertyFields Maps property names to their field mappings.
 * @property linkFields Maps link/association role names to their field mappings.
 * @property totalFieldCount Total number of `prop_X` fields in the generated class
 *                           (including inherited fields from parent classes).
 */
data class ClassMetadata(
    val className: String,
    val binaryName: String,
    val propertyFields: Map<String, PropertyFieldMapping>,
    val linkFields: Map<String, LinkFieldMapping>,
    val totalFieldCount: Int
)

/**
 * Complete metadata about all classes compiled by a [Metamodel].
 *
 * @property classes Maps metamodel class name to its [ClassMetadata].
 * @property classHierarchy Maps each class name to all its subtypes including itself.
 * @property enumValueClassNames Maps enum name to the binary name of the generated EnumValue class.
 * @property enumContainerClassNames Maps enum name to the binary name of the generated Enum container class.
 * @property classContainerClassNames Maps class name to the binary name of the generated ClassContainer class.
 */
data class MetamodelMetadata(
    val classes: Map<String, ClassMetadata>,
    val classHierarchy: Map<String, Set<String>>,
    val enumValueClassNames: Map<String, String>,
    val enumContainerClassNames: Map<String, String>,
    val classContainerClassNames: Map<String, String>
)

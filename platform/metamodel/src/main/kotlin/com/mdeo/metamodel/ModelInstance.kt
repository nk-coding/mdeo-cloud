package com.mdeo.metamodel

import java.io.DataInput
import java.io.DataOutput

/**
 * Abstract base class for all generated model instance classes.
 *
 * Generated classes for each metamodel class extend this class.
 * Properties and association ends are stored as `prop_0`, `prop_1`, ... fields
 * in the generated subclass, with proper JVM types based on the metamodel definition.
 *
 * Identity is based on JVM object identity (default [equals] / [hashCode]).
 * The class name can be resolved via [com.mdeo.metamodel.Metamodel.classNameOf].
 * The logical instance name is stored externally in [com.mdeo.metamodel.Model.instancesByName].
 */
abstract class ModelInstance {
    /**
     * Returns the metamodel class name, derived from the generated JVM class name.
     *
     * The generated class is named `<ClassName>Instance` so the suffix is stripped.
     */
    override fun toString(): String = this.javaClass.simpleName.removeSuffix("Instance")

    /**
     * Returns the value of the property or link field with the given key name.
     *
     * @param key The property or link name as declared in the metamodel.
     * @return The field value, or null if the field is not set.
     * @throws IllegalArgumentException if no field with the given key exists on this class.
     */
    abstract fun getPropertyByKey(key: String): Any?

    /**
     * Sets the value of the property or link field with the given key name.
     *
     * @param key The property or link name as declared in the metamodel.
     * @param value The new value to set.
     * @throws IllegalArgumentException if no field with the given key exists on this class.
     */
    abstract fun setPropertyByKey(key: String, value: Any?)

    /**
     * Creates a shallow property copy of this instance.
     *
     * The returned instance is created via the generated copy constructor, which copies
     * all scalar and multi-valued property fields but leaves link (association) Set fields
     * uninitialised (null). Call [copyReferences] on the original instance to populate the
     * link sets on the returned copy with presized [java.util.HashSet] instances.
     *
     * @return A new [ModelInstance] with the same property values and no link references set.
     */
    abstract fun copy(): ModelInstance

    /**
     * Resolves all link (association) fields in this instance via [instanceMap] and
     * writes the mapped references into [target].
     *
     * Every multi-valued link field is remapped into a fresh, presized [java.util.HashSet].
     * This is the second phase of a two-phase deep copy (after [copy] creates the instance).
     *
     * @param target The instance to write resolved references into.
     * @param instanceMap A mapping from old [ModelInstance] objects to their copies.
     */
    abstract fun copyReferences(target: ModelInstance, instanceMap: Map<*, *>)

    /**
     * Writes all scalar and list property field values to [out] in field-index order.
     *
     * Link (association) fields are skipped — they are handled separately by
     * [writeLinkFields]. For each field a one-byte null marker is written first
     * (0 = null, 1 = present), followed by the raw value bytes when present.
     * The exact encoding matches the JVM type declared by the metamodel:
     * `int`, `long`, `float`, `double`, `boolean`, `String`, enum entry name,
     * or `List` with per-element encoding.
     *
     * @param out The data output to write field values into.
     */
    abstract fun writeProperties(out: DataOutput)

    /**
     * Reads all scalar and list property field values from [input] in field-index order,
     * setting each field on this instance.
     *
     * Link (association) fields are skipped — they are handled separately by
     * [readLinkFields]. The byte layout must match what [writeProperties] produced
     * for the same metamodel class.
     *
     * @param input The data input to read field values from.
     * @param metamodel The compiled metamodel, used to resolve enum entry values.
     */
    abstract fun readProperties(input: DataInput, metamodel: Metamodel)

    /**
     * Writes all link (association) Set fields to [out] in field-index order.
     *
     * Each Set is written as an int count followed by one int per element, where the
     * int is the element's index in [instanceIndex]. This enables compact reference
     * serialization without embedding full instance identities.
     *
     * @param out The data output to write link references into.
     * @param instanceIndex Maps each [ModelInstance] to its sequential index in the
     *        serialization order.
     */
    abstract fun writeLinkFields(out: DataOutput, instanceIndex: Map<*, *>)

    /**
     * Reads all link (association) Set fields from [input] in field-index order,
     * populating each Set on this instance.
     *
     * Each Set is read as an int count followed by that many ints, where each int
     * is an index into [instances]. This is the second phase of two-phase binary
     * deserialization (after [readProperties] creates scalar state).
     *
     * @param input The data input to read link references from.
     * @param instances The ordered array of all deserialized instances, enabling
     *        index-based reference resolution.
     */
    abstract fun readLinkFields(input: DataInput, instances: Array<ModelInstance>)
}

package com.mdeo.metamodel

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
}

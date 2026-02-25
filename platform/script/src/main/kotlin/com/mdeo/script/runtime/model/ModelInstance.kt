package com.mdeo.script.runtime.model

/**
 * Abstract base class for all generated model instance classes.
 *
 * Generated classes for each metamodel class extend this class.
 * The backing field provides access to property values through
 * the ModelInstanceBacking interface, allowing different storage
 * implementations to be used at runtime.
 *
 * @param backing The backing storage for property access.
 */
abstract class ModelInstance(
    @JvmField
    protected val backing: ModelInstanceBacking
) {
    /**
     * Gets the instance name for display purposes.
     *
     * @return The instance name/identifier.
     */
    fun getInstanceName(): String = backing.getInstanceName()

    /**
     * Gets the class name of this instance.
     *
     * @return The class name from the metamodel.
     */
    fun getClassName(): String = backing.getClassName()

    /**
     * Returns a string representation of this instance.
     * Delegates to the backing for the actual formatting.
     *
     * @return A string representation in the format "ClassName:instanceName".
     */
    override fun toString(): String {
        return "${backing.getClassName()}:${backing.getInstanceName()}"
    }

    /**
     * Checks equality with another object.
     * Two ModelInstance objects are equal if they have the same
     * class name and instance name.
     *
     * @param other The object to compare with.
     * @return True if the objects are equal.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelInstance) return false
        return getClassName() == other.getClassName() &&
               getInstanceName() == other.getInstanceName()
    }

    /**
     * Computes the hash code for this instance.
     *
     * @return The hash code based on class name and instance name.
     */
    override fun hashCode(): Int {
        return 31 * getClassName().hashCode() + getInstanceName().hashCode()
    }
}

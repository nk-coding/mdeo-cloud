package com.mdeo.script.runtime.model

/**
 * Factory interface for creating ModelInstance and enum value objects.
 *
 * This interface allows different implementations to create instances
 * using the appropriate generated classes. The factory is used by
 * ModelInstanceBacking implementations to create related objects.
 */
interface ModelInstanceFactory {

    /**
     * Creates a ModelInstance for the given class and backing.
     *
     * @param className The metamodel class name.
     * @param backing The backing storage for the instance.
     * @return A ModelInstance of the appropriate generated subclass.
     */
    fun createInstance(className: String, backing: ModelInstanceBacking): ModelInstance

    /**
     * Creates an enum value singleton.
     *
     * @param enumName The enum type name.
     * @param entryName The enum entry name.
     * @return The enum value singleton object.
     */
    fun createEnumValue(enumName: String, entryName: String): Any
}

package com.mdeo.script.runtime.model

/**
 * Interface for accessing model instances at runtime.
 *
 * Implementations of this interface provide access to model data
 * during script execution. The script compiler generates calls to
 * this interface through the `model` global variable.
 *
 * Example usage in script:
 * ```
 * val allCars = model.allInstances("Car")
 * for (car in allCars) {
 *     println(car.name)
 * }
 * ```
 */
interface ScriptModel {

    /**
     * Gets all instances of a class (including subclass instances).
     *
     * The returned list contains instances of the specified class
     * and all its subclasses in the metamodel hierarchy.
     *
     * @param className The name of the class to get instances for.
     * @return A list of ModelInstance objects.
     */
    fun getAllInstances(className: String): List<ModelInstance>
}

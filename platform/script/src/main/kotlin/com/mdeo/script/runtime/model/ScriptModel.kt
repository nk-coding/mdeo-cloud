package com.mdeo.script.runtime.model

import com.mdeo.script.compiler.model.ScriptClassBytecodeGenerator
import com.mdeo.script.compiler.model.ScriptEnumBytecodeGenerator
import com.mdeo.script.runtime.ScriptClassLoader

/**
 * Abstract base class for accessing model instances at runtime.
 *
 * Implementations provide access to model data
 * during script execution. The script compiler generates calls to
 * this type through the `model` global variable.
 *
 * Example usage in script:
 * ```
 * val allCars = model.allInstances("Car")
 * for (car in allCars) {
 *     println(car.name)
 * }
 * ```
 *
 * The base class also provides reflective creation of generated
 * model instance classes and enum singleton values.
 *
 * @param classLoader The class loader containing generated script classes.
 * @param metamodelPath The metamodel path used for generated type naming.
 */
abstract class ScriptModel(
    private val classLoader: ScriptClassLoader,
    private val metamodelPath: String
) {

    /**
     * Gets all instances of a class (including subclass instances).
     *
     * The returned list contains instances of the specified class
     * and all its subclasses in the metamodel hierarchy.
     *
     * @param className The name of the class to get instances for.
     * @return A list of ModelInstance objects.
     */
    abstract fun getAllInstances(className: String): List<ModelInstance>

    /**
     * Creates a generated ModelInstance subclass reflectively.
     */
    protected fun createInstance(className: String, backing: ModelInstanceBacking): ModelInstance {
        val internalName = ScriptClassBytecodeGenerator.getInstanceClassName(className, metamodelPath)
        val jvmClassName = internalName.replace("/", ".")
        val clazz = classLoader.loadClass(jvmClassName)
        val constructor = clazz.getConstructor(ModelInstanceBacking::class.java)
        return constructor.newInstance(backing) as ModelInstance
    }

    /**
     * Resolves an enum entry singleton value reflectively.
     */
    protected fun createEnumValue(enumName: String, entryName: String): Any {
        val internalName = ScriptEnumBytecodeGenerator.getEnumContainerClassName(enumName, metamodelPath)
        val jvmClassName = internalName.replace("/", ".")
        val clazz = classLoader.loadClass(jvmClassName)
        val field = clazz.getField(entryName)
        return field.get(null) ?: error("Enum entry $enumName.$entryName not found")
    }
}

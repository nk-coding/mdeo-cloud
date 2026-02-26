package com.mdeo.script.runtime.model

import com.mdeo.script.compiler.CompiledProgram
import com.mdeo.script.runtime.ScriptClassLoader

/**
 * Abstract base class for accessing model instances at runtime.
 *
 * Implementations provide access to model data during script execution.
 * The script compiler generates calls to this type through the `model` global variable.
 *
 * Example usage in script:
 * ```
 * val allCars = model.allInstances("Car")
 * for (car in allCars) {
 *     println(car.name)
 * }
 * ```
 *
 * Class loading is performed here once at construction time using the lookup maps in
 * [program] and the given [classLoader], so subclasses do not need to deal with JVM
 * name resolution.
 *
 * @param classLoader  The class loader that holds the generated script bytecodes.
 * @param program      The compiled program whose [CompiledProgram.instanceClassNames] and
 *                     [CompiledProgram.enumContainerClassNames] maps are used to resolve
 *                     generated classes by metamodel name.
 */
abstract class ScriptModel(
    classLoader: ScriptClassLoader,
    program: CompiledProgram
) {
    private val instanceClasses: Map<String, Class<*>> =
        program.instanceClassNames.mapValues { (_, binaryName) -> classLoader.loadClass(binaryName) }

    private val enumContainerClasses: Map<String, Class<*>> =
        program.enumContainerClassNames.mapValues { (_, binaryName) -> classLoader.loadClass(binaryName) }


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
     * Creates a generated [ModelInstance] subclass for [className] with the given [backing].
     */
    protected fun createInstance(className: String, backing: ModelInstanceBacking): ModelInstance {
        val clazz = instanceClasses[className]
            ?: error("No generated class registered for model class '$className'")
        val constructor = clazz.getConstructor(ModelInstanceBacking::class.java)
        return constructor.newInstance(backing) as ModelInstance
    }

    /**
     * Resolves an enum entry singleton value from the generated enum container class.
     */
    protected fun createEnumValue(enumName: String, entryName: String): Any {
        val clazz = enumContainerClasses[enumName]
            ?: error("No generated class registered for enum '$enumName'")
        val field = clazz.getField(entryName)
        return field.get(null) ?: error("Enum entry $enumName.$entryName not found")
    }
}

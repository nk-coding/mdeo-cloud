package com.mdeo.metamodel

/**
 * Custom class loader that loads classes from in-memory bytecode maps.
 *
 * Used by [Metamodel] to load the JVM classes generated via ASM bytecode generation
 * for metamodel instance classes, enum value/container classes, and class container classes.
 *
 * @param bytecodes Map of binary class name (dot-separated) to bytecode.
 * @param parent The parent class loader.
 */
class MetamodelClassLoader(
    val bytecodes: Map<String, ByteArray>,
    parent: ClassLoader
) : ClassLoader(parent) {

    private val loadedClasses = mutableMapOf<String, Class<*>>()

    override fun findClass(name: String): Class<*> {
        val bytecode = bytecodes[name]
            ?: throw ClassNotFoundException(name)
        return loadedClasses.getOrPut(name) {
            defineClass(name, bytecode, 0, bytecode.size)
        }
    }
}

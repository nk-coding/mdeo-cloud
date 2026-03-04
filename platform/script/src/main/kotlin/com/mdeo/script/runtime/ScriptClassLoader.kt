package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledProgram

/**
 * Class loader for compiled script programs.
 *
 * Loads all bytecodes produced by the ScriptCompiler (script classes, generated functional
 * interfaces, and generated model classes) from a single flat [CompiledProgram.allBytecodes]
 * map.  A unified cache ensures each class is defined exactly once regardless of how it is
 * first requested.
 *
 * Use [loadClassForFile] to obtain the top-level script class for a source file, and
 * [loadClass] for any other class by binary name.
 *
 * @param program The compiled program containing all bytecodes and lookup maps.
 * @param parent  The parent ClassLoader – must be supplied explicitly.
 */
class ScriptClassLoader(
    private val program: CompiledProgram,
    parent: ClassLoader
) : ClassLoader(parent) {

    /**
     * Single unified cache of all classes that have been defined by this loader. 
     */
    private val loadedClasses = mutableMapOf<String, Class<*>>()

    /**
     * Loads the top-level JVM class that was compiled from [filePath].
     *
     * @throws ClassNotFoundException if no class was compiled for [filePath].
     */
    fun loadClassForFile(filePath: String): Class<*> {
        val binaryName = program.scriptFileToClass[filePath]
            ?: throw ClassNotFoundException("No compiled class for file: $filePath")
        return loadClass(binaryName)
    }

    /**
     * Finds a class by its binary name (dot-separated) in the compiled program.
     *
     * The class is defined at most once; subsequent calls return the cached instance.
     */
    override fun findClass(name: String): Class<*> {
        val bytecode = program.allBytecodes[name]
            ?: throw ClassNotFoundException(name)
        return loadedClasses.getOrPut(name) {
            defineClass(name, bytecode, 0, bytecode.size)
        }
    }
}

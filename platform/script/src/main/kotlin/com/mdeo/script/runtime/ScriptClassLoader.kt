package com.mdeo.script.runtime

import com.mdeo.script.compiler.CompiledProgram

/**
 * Class loader for compiled script programs.
 *
 * Loads all bytecodes produced by the ScriptCompiler (the single ScriptProgram class,
 * generated functional interfaces, and generated model classes) from a single flat
 * [CompiledProgram.allBytecodes] map.  A unified cache ensures each class is defined
 * exactly once regardless of how it is first requested.
 *
 * Use [loadScriptProgramClass] to load the single compiled script class, and
 * [loadClassForFile] to obtain the same class after verifying a source file was compiled.
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
     * Loads the single [CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME] class that contains
     * all compiled script functions.
     */
    fun loadScriptProgramClass(): Class<*> {
        return loadClass(CompiledProgram.SCRIPT_PROGRAM_BINARY_NAME)
    }

    /**
     * Loads the top-level JVM class that was compiled from [filePath].
     *
     * Verifies that [filePath] is present in [CompiledProgram.functionLookup] before
     * loading, so that callers receive a [ClassNotFoundException] for unknown files.
     *
     * @throws ClassNotFoundException if no functions were compiled for [filePath].
     */
    fun loadClassForFile(filePath: String): Class<*> {
        if (!program.functionLookup.containsKey(filePath)) {
            throw ClassNotFoundException("No compiled class for file: $filePath")
        }
        return loadScriptProgramClass()
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

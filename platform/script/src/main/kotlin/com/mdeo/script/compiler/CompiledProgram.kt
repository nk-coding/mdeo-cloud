package com.mdeo.script.compiler

import com.mdeo.metamodel.Metamodel

/**
 * Represents a compiled script program ready for class-loading and execution.
 *
 * All functions from all script files are compiled into a single JVM class ([SCRIPT_PROGRAM_BINARY_NAME]).
 * The [functionLookup] maps each (filePath, functionName) pair to the artificial JVM method name
 * (`fn0`, `fn1`, ...) assigned during compilation.
 *
 * All bytecodes (the script program class, generated lambda interfaces, and metamodel classes)
 * are stored in [allBytecodes] keyed by JVM binary class name (dot-separated).
 *
 * @param allBytecodes   All bytecodes keyed by JVM binary class name.
 * @param functionLookup Maps each script file path to a map of function name → JVM method name.
 * @param metamodel      The compiled metamodel, or null if no metamodel was used.
 */
data class CompiledProgram(
    val allBytecodes: Map<String, ByteArray>,
    val functionLookup: Map<String, Map<String, String>> = emptyMap(),
    val metamodel: Metamodel? = null
) {
    companion object {
        /**
         * JVM binary class name (dot-separated) of the single generated script class. 
         */
        const val SCRIPT_PROGRAM_BINARY_NAME = "com.mdeo.script.generated.ScriptProgram"

        /**
         * JVM internal class name (slash-separated) of the single generated script class. 
         */
        const val SCRIPT_PROGRAM_INTERNAL_NAME = "com/mdeo/script/generated/ScriptProgram"
    }
}

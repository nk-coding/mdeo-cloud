package com.mdeo.script.compiler

/**
 * Represents a compiled script program ready for class-loading and execution.
 *
 * All bytecodes produced by the compiler (script classes, lambda interfaces, model instance
 * classes, enum classes and their containers) are merged into a single flat map keyed by
 * JVM binary class name (dot-separated, e.g. `com.mdeo.script.generated.Script_...`).
 *
 * Separate lookup maps allow callers to resolve names without any string manipulation.
 *
 * @param allBytecodes           All bytecodes keyed by JVM binary class name.
 * @param scriptFileToClass      Maps each script file path to the binary class name of its
 *                               top-level generated class.
 * @param instanceClassNames     Maps each metamodel class name (e.g. "Car") to the binary
 *                               class name of the generated [ModelInstance] subclass.
 * @param enumContainerClassNames Maps each metamodel enum name (e.g. "Color") to the binary
 *                               class name of the generated enum container class.
 */
data class CompiledProgram(
    val allBytecodes: Map<String, ByteArray>,
    val scriptFileToClass: Map<String, String> = emptyMap(),
    val instanceClassNames: Map<String, String> = emptyMap(),
    val enumContainerClassNames: Map<String, String> = emptyMap()
)

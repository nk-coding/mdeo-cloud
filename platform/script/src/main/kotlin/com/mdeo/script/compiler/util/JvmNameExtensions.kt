package com.mdeo.script.compiler.util

/**
 * Converts a JVM internal class name to a JVM binary class name.
 *
 * JVM internal names use forward slashes as package separators (e.g.
 * `com/mdeo/script/generated/Script_...`).  The class-loading API (`ClassLoader`,
 * `Class.forName`, etc.) expects binary names, which use dots instead (e.g.
 * `com.mdeo.script.generated.Script_...`).
 *
 * This conversion is necessary wherever a name produced by the ASM bytecode
 * generator (which always uses the internal `/`-separated form) must be passed
 * to a class-loading or runtime lookup.
 *
 * @return The binary class name with dots as package separators.
 */
fun String.toJvmBinaryName(): String = replace("/", ".")

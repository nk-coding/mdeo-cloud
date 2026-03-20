@file:Suppress("unused")

package com.mdeo.script.runtime

/**
 * Deprecated: Thread-local execution context has been replaced by [ScriptContext].
 *
 * Script execution now uses instance-based [ScriptContext] passed through constructors,
 * which enables safe concurrent execution. Use [SimpleScriptContext] to create contexts
 * and [ExecutionEnvironment.invoke] to execute scripts.
 *
 * This object is retained only as a tombstone to prevent accidental re-introduction.
 */
@Deprecated("Use ScriptContext/SimpleScriptContext instead", level = DeprecationLevel.ERROR)
object ExecutionContext

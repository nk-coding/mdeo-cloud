package com.mdeo.script.runtime

import com.mdeo.script.runtime.model.ScriptModel
import java.io.PrintStream

/**
 * Thread-local execution context for script execution.
 *
 * This is the single unified context for all thread-local state during script execution.
 * It manages both the console output stream and the current script model, allowing
 * scripts to access these resources without requiring explicit parameter passing
 * through the call hierarchy.
 */
object ExecutionContext {
    @PublishedApi
    internal val consoleStream = ThreadLocal<PrintStream>()

    @PublishedApi
    internal val modelHolder = ThreadLocal<ScriptModel?>()

    /**
     * Gets the current console PrintStream for this thread.
     * Falls back to System.out if no context is set.
     *
     * @return The current PrintStream.
     */
    fun getConsole(): PrintStream {
        return consoleStream.get() ?: System.out
    }

    /**
     * Sets the console PrintStream for this thread.
     *
     * @param stream The PrintStream to use for console output.
     */
    fun setConsole(stream: PrintStream) {
        consoleStream.set(stream)
    }

    /**
     * Clears the console PrintStream for this thread.
     * After calling this, getConsole() will return System.out.
     */
    fun clearConsole() {
        consoleStream.remove()
    }

    /**
     * Gets the current ScriptModel for this thread, or null if none is set.
     *
     * @return The current ScriptModel, or null.
     */
    fun getModel(): ScriptModel? {
        return modelHolder.get()
    }

    /**
     * Gets the current ScriptModel for this thread, throwing if none is set.
     *
     * @return The current ScriptModel.
     * @throws IllegalStateException if no model context is available.
     */
    @JvmStatic
    fun requireModel(): ScriptModel {
        return modelHolder.get()
            ?: throw IllegalStateException(
                "No model context available. " +
                "Ensure the script is executed with ExecutionContext.withContext()"
            )
    }

    /**
     * Sets the script model for this thread.
     *
     * @param model The ScriptModel to use.
     */
    fun setModel(model: ScriptModel) {
        modelHolder.set(model)
    }

    /**
     * Clears the script model for this thread.
     */
    fun clearModel() {
        modelHolder.remove()
    }

    /**
     * Clears all thread-local state (console stream and model).
     */
    fun clear() {
        clearConsole()
        clearModel()
    }

    /**
     * Executes a block with both a console stream and an optional model set,
     * then restores both to their previous values on exit.
     *
     * @param stream The PrintStream to use for console output.
     * @param model The ScriptModel to set, or null to leave model unchanged.
     * @param block The code block to execute.
     * @return The result of the block.
     */
    inline fun <T> withContext(stream: PrintStream, model: ScriptModel?, block: () -> T): T {
        val previousConsole = consoleStream.get()
        val previousModel = modelHolder.get()
        try {
            setConsole(stream)
            if (model != null) {
                setModel(model)
            }
            return block()
        } finally {
            if (previousConsole != null) {
                consoleStream.set(previousConsole)
            } else {
                clearConsole()
            }
            if (previousModel != null) {
                modelHolder.set(previousModel)
            } else {
                clearModel()
            }
        }
    }
}

package com.mdeo.script.runtime

import java.io.PrintStream

/**
 * Thread-local execution context for script execution.
 *
 * This context stores the current PrintStream for output operations like println.
 * It allows GlobalFunctions to write to the correct output stream without
 * requiring explicit passing of the stream through the call hierarchy.
 */
object ExecutionContext {
    @PublishedApi
    internal val consoleStream = ThreadLocal<PrintStream>()

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
     * Executes a block with a specific console stream, then restores the previous context.
     *
     * @param stream The PrintStream to use during execution.
     * @param block The code block to execute.
     * @return The result of the block.
     */
    inline fun <T> withConsole(stream: PrintStream, block: () -> T): T {
        val previous = consoleStream.get()
        try {
            setConsole(stream)
            return block()
        } finally {
            if (previous != null) {
                consoleStream.set(previous)
            } else {
                clearConsole()
            }
        }
    }
}

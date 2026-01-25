package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for an action that takes no arguments and returns nothing.
 *
 * This is the script language's equivalent to [java.lang.Runnable],
 * providing a consistent `call` method naming convention across all functional interfaces.
 */
@FunctionalInterface
fun interface Action0 {
    /**
     * Invokes this action with no arguments.
     */
    fun call()
}

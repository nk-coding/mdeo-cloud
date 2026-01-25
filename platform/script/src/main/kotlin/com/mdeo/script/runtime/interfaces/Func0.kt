package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for a function that takes no arguments and returns a result.
 *
 * This is the script language's equivalent to [java.util.function.Supplier],
 * providing a consistent `call` method naming convention across all functional interfaces.
 *
 * @param R the return type of the function
 */
@FunctionalInterface
fun interface Func0<out R> {
    /**
     * Invokes this function with no arguments.
     *
     * @return the function result
     */
    fun call(): R
}

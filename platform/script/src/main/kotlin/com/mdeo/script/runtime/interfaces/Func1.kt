package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for a function that takes one argument and returns a result.
 *
 * This is the script language's equivalent to [java.util.function.Function],
 * providing a consistent `call` method naming convention across all functional interfaces.
 *
 * @param T the type of the input argument
 * @param R the return type of the function
 */
@FunctionalInterface
fun interface Func1<in T, out R> {
    /**
     * Invokes this function with the given argument.
     *
     * @param arg the function argument
     * @return the function result
     */
    fun call(arg: T): R
}

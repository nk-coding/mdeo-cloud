package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for a function that takes two arguments and returns a result.
 *
 * This is the script language's equivalent to [java.util.function.BiFunction],
 * providing a consistent `call` method naming convention across all functional interfaces.
 *
 * @param T1 the type of the first input argument
 * @param T2 the type of the second input argument
 * @param R the return type of the function
 */
@FunctionalInterface
fun interface Func2<in T1, in T2, out R> {
    /**
     * Invokes this function with the given arguments.
     *
     * @param arg1 the first function argument
     * @param arg2 the second function argument
     * @return the function result
     */
    fun call(arg1: T1, arg2: T2): R
}

package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for a function that takes three arguments and returns a result.
 *
 * This provides a consistent `call` method naming convention across all functional interfaces
 * in the script language runtime.
 *
 * @param T1 the type of the first input argument
 * @param T2 the type of the second input argument
 * @param T3 the type of the third input argument
 * @param R the return type of the function
 */
@FunctionalInterface
fun interface Func3<in T1, in T2, in T3, out R> {
    /**
     * Invokes this function with the given arguments.
     *
     * @param arg1 the first function argument
     * @param arg2 the second function argument
     * @param arg3 the third function argument
     * @return the function result
     */
    fun call(arg1: T1, arg2: T2, arg3: T3): R
}

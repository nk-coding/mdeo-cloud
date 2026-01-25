package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for an action that takes three arguments and returns nothing.
 *
 * This provides a consistent `call` method naming convention across all functional interfaces
 * in the script language runtime.
 *
 * @param T1 the type of the first input argument
 * @param T2 the type of the second input argument
 * @param T3 the type of the third input argument
 */
@FunctionalInterface
fun interface Action3<in T1, in T2, in T3> {
    /**
     * Invokes this action with the given arguments.
     *
     * @param arg1 the first action argument
     * @param arg2 the second action argument
     * @param arg3 the third action argument
     */
    fun call(arg1: T1, arg2: T2, arg3: T3)
}

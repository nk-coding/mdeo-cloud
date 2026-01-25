package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for an action that takes two arguments and returns nothing.
 *
 * This is the script language's equivalent to [java.util.function.BiConsumer],
 * providing a consistent `call` method naming convention across all functional interfaces.
 *
 * @param T1 the type of the first input argument
 * @param T2 the type of the second input argument
 */
@FunctionalInterface
fun interface Action2<in T1, in T2> {
    /**
     * Invokes this action with the given arguments.
     *
     * @param arg1 the first action argument
     * @param arg2 the second action argument
     */
    fun call(arg1: T1, arg2: T2)
}

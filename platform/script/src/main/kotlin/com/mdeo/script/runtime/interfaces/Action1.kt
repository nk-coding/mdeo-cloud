package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for an action that takes one argument and returns nothing.
 *
 * This is the script language's equivalent to [java.util.function.Consumer],
 * providing a consistent `call` method naming convention across all functional interfaces.
 *
 * @param T the type of the input argument
 */
@FunctionalInterface
fun interface Action1<in T> {
    /**
     * Invokes this action with the given argument.
     *
     * @param arg the action argument
     */
    fun call(arg: T)
}

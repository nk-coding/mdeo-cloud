package com.mdeo.script.runtime.interfaces

/**
 * Functional interface for a predicate that takes one argument and returns a boolean.
 *
 * This is the script language's equivalent to [java.util.function.Predicate],
 * providing a consistent `call` method naming convention across all functional interfaces.
 *
 * While this could technically be represented as `Func1<T, Boolean>`, having a dedicated
 * predicate interface provides clearer intent and better interoperability with existing
 * predicate-based operations in the standard library.
 *
 * @param T the type of the input argument
 */
@FunctionalInterface
fun interface Predicate1<in T> {
    /**
     * Evaluates this predicate on the given argument.
     *
     * @param arg the input argument
     * @return `true` if the input argument matches the predicate, otherwise `false`
     */
    fun call(arg: T): Boolean
}

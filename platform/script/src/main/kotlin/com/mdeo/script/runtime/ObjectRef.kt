package com.mdeo.script.runtime

/**
 * Mutable wrapper for object references.
 * Used when local variables need to be modified from within lambdas.
 *
 * This class provides a simple mutable container that can be captured
 * by lambda expressions, allowing the lambda to modify the original
 * variable's value.
 *
 * @param T The type of the wrapped object.
 */
class ObjectRef<T>(@JvmField var value: T? = null)

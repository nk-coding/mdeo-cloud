package com.mdeo.script.runtime

/**
 * Mutable wrapper for int values.
 * Used when local variables need to be modified from within lambdas.
 *
 * This class provides a simple mutable container that can be captured
 * by lambda expressions, allowing the lambda to modify the original
 * variable's value.
 */
class IntRef(@JvmField var value: Int = 0)

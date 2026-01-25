package com.mdeo.script.runtime.refs

/**
 * Mutable wrapper for double values.
 * Used when local variables need to be modified from within lambdas.
 *
 * This class provides a simple mutable container that can be captured
 * by lambda expressions, allowing the lambda to modify the original
 * variable's value.
 */
class DoubleRef(@JvmField var value: Double = 0.0)

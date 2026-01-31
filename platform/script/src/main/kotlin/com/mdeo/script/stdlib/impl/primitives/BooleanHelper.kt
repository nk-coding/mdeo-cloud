package com.mdeo.script.stdlib.impl.primitives

/**
 * Helper methods for the Boolean type in the script language.
 * These static methods are called with the instance as the first parameter.
 * 
 * Note: Most boolean operations are handled by Java's boolean primitive type directly.
 * This class provides additional utility methods specific to the script language.
 */
object BooleanHelper {

    /**
     * Converts a boolean to its string representation.
     *
     * @param value the boolean value
     * @return "true" or "false"
     */
    @JvmStatic
    fun toString(value: Boolean): String = value.toString()

    /**
     * Converts a boolean to an integer (1 for true, 0 for false).
     *
     * @param value the boolean value
     * @return 1 if true, 0 if false
     */
    @JvmStatic
    fun asInteger(value: Boolean): Int = if (value) 1 else 0

    /**
     * Converts a boolean to a long (1 for true, 0 for false).
     *
     * @param value the boolean value
     * @return 1L if true, 0L if false
     */
    @JvmStatic
    fun asLong(value: Boolean): Long = if (value) 1L else 0L

    /**
     * Converts a boolean to a double (1.0 for true, 0.0 for false).
     *
     * @param value the boolean value
     * @return 1.0 if true, 0.0 if false
     */
    @JvmStatic
    fun asDouble(value: Boolean): Double = if (value) 1.0 else 0.0

    /**
     * Converts a boolean to a float (1.0f for true, 0.0f for false).
     *
     * @param value the boolean value
     * @return 1.0f if true, 0.0f if false
     */
    @JvmStatic
    fun asFloat(value: Boolean): Float = if (value) 1.0f else 0.0f

    /**
     * Returns the logical negation of a boolean.
     *
     * @param value the boolean value
     * @return the negated value
     */
    @JvmStatic
    fun not(value: Boolean): Boolean = !value

    /**
     * Returns the logical AND of two booleans.
     *
     * @param value the first boolean value
     * @param other the second boolean value
     * @return true if both are true
     */
    @JvmStatic
    fun and(value: Boolean, other: Boolean): Boolean = value && other

    /**
     * Returns the logical OR of two booleans.
     *
     * @param value the first boolean value
     * @param other the second boolean value
     * @return true if either is true
     */
    @JvmStatic
    fun or(value: Boolean, other: Boolean): Boolean = value || other

    /**
     * Returns the logical XOR of two booleans.
     *
     * @param value the first boolean value
     * @param other the second boolean value
     * @return true if exactly one is true
     */
    @JvmStatic
    fun xor(value: Boolean, other: Boolean): Boolean = value xor other

    /**
     * Returns the logical implication (value implies other).
     *
     * @param value the antecedent
     * @param other the consequent
     * @return true unless value is true and other is false
     */
    @JvmStatic
    fun implies(value: Boolean, other: Boolean): Boolean = !value || other
}

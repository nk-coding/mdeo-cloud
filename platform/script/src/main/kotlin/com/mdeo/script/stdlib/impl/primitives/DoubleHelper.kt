package com.mdeo.script.stdlib.impl.primitives

import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.abs

/**
 * Helper methods for the Double type in the script language.
 * These static methods are called with the instance as the first parameter.
 */
object DoubleHelper {

    /**
     * Returns the absolute value of a double.
     *
     * @param value the double value
     * @return the absolute value
     */
    @JvmStatic
    fun abs(value: Double): Double = kotlin.math.abs(value)

    /**
     * Returns the ceiling of a double as a long.
     *
     * @param value the double value
     * @return the smallest long greater than or equal to the value
     */
    @JvmStatic
    fun ceiling(value: Double): Long = ceil(value).toLong()

    /**
     * Returns the floor of a double as a long.
     *
     * @param value the double value
     * @return the largest long less than or equal to the value
     */
    @JvmStatic
    fun floor(value: Double): Long = kotlin.math.floor(value).toLong()

    /**
     * Returns the natural logarithm of a double.
     *
     * @param value the double value
     * @return the natural logarithm
     */
    @JvmStatic
    fun log(value: Double): Double = ln(value)

    /**
     * Returns the base-10 logarithm of a double.
     *
     * @param value the double value
     * @return the base-10 logarithm
     */
    @JvmStatic
    fun log10(value: Double): Double = kotlin.math.log10(value)

    /**
     * Returns the maximum of a double and an int.
     *
     * @param value the double
     * @param other the int
     * @return the larger value as a double
     */
    @JvmStatic
    fun max(value: Double, other: Int): Double = maxOf(value, other.toDouble())

    /**
     * Returns the maximum of a double and a long.
     *
     * @param value the double
     * @param other the long
     * @return the larger value as a double
     */
    @JvmStatic
    fun max(value: Double, other: Long): Double = maxOf(value, other.toDouble())

    /**
     * Returns the maximum of a double and a float.
     *
     * @param value the double
     * @param other the float
     * @return the larger value as a double
     */
    @JvmStatic
    fun max(value: Double, other: Float): Double = maxOf(value, other.toDouble())

    /**
     * Returns the maximum of two doubles.
     *
     * @param value the first double
     * @param other the second double
     * @return the larger value
     */
    @JvmStatic
    fun max(value: Double, other: Double): Double = maxOf(value, other)

    /**
     * Returns the minimum of a double and an int.
     *
     * @param value the double
     * @param other the int
     * @return the smaller value as a double
     */
    @JvmStatic
    fun min(value: Double, other: Int): Double = minOf(value, other.toDouble())

    /**
     * Returns the minimum of a double and a long.
     *
     * @param value the double
     * @param other the long
     * @return the smaller value as a double
     */
    @JvmStatic
    fun min(value: Double, other: Long): Double = minOf(value, other.toDouble())

    /**
     * Returns the minimum of a double and a float.
     *
     * @param value the double
     * @param other the float
     * @return the smaller value as a double
     */
    @JvmStatic
    fun min(value: Double, other: Float): Double = minOf(value, other.toDouble())

    /**
     * Returns the minimum of two doubles.
     *
     * @param value the first double
     * @param other the second double
     * @return the smaller value
     */
    @JvmStatic
    fun min(value: Double, other: Double): Double = minOf(value, other)

    /**
     * Raises a double to the given power.
     *
     * @param value the base
     * @param exponent the exponent
     * @return the result
     */
    @JvmStatic
    fun pow(value: Double, exponent: Double): Double = value.pow(exponent)

    /**
     * Returns the rounded value of a double as a long.
     *
     * @param value the double value
     * @return the rounded long value
     */
    @JvmStatic
    fun round(value: Double): Long = kotlin.math.round(value).toLong()

    /**
     * Converts a double to a string.
     *
     * @param value the double value
     * @return the string representation
     */
    @JvmStatic
    fun asString(value: Double): String = value.toString()

    /**
     * Converts a double to a float.
     *
     * @param value the double value
     * @return the float value
     */
    @JvmStatic
    fun asFloat(value: Double): Float = value.toFloat()

    /**
     * Converts a double to an int.
     *
     * @param value the double value
     * @return the int value
     */
    @JvmStatic
    fun asInt(value: Double): Int = value.toInt()

    /**
     * Converts a double to a long.
     *
     * @param value the double value
     * @return the long value
     */
    @JvmStatic
    fun asLong(value: Double): Long = value.toLong()

    /**
     * Checks if the double is NaN.
     *
     * @param value the double value
     * @return true if NaN
     */
    @JvmStatic
    fun isNaN(value: Double): Boolean = value.isNaN()

    /**
     * Checks if the double is infinite.
     *
     * @param value the double value
     * @return true if infinite
     */
    @JvmStatic
    fun isInfinite(value: Double): Boolean = value.isInfinite()
}

package com.mdeo.script.stdlib.primitives

import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.abs

/**
 * Helper methods for the Float type in the script language.
 * These static methods are called with the instance as the first parameter.
 */
object FloatHelper {

    /**
     * Returns the absolute value of a float.
     *
     * @param value the float value
     * @return the absolute value
     */
    @JvmStatic
    fun abs(value: Float): Float = kotlin.math.abs(value)

    /**
     * Returns the ceiling of a float as an int.
     *
     * @param value the float value
     * @return the smallest integer greater than or equal to the value
     */
    @JvmStatic
    fun ceiling(value: Float): Int = ceil(value.toDouble()).toInt()

    /**
     * Returns the floor of a float as an int.
     *
     * @param value the float value
     * @return the largest integer less than or equal to the value
     */
    @JvmStatic
    fun floor(value: Float): Int = kotlin.math.floor(value.toDouble()).toInt()

    /**
     * Returns the natural logarithm of a float.
     *
     * @param value the float value
     * @return the natural logarithm
     */
    @JvmStatic
    fun log(value: Float): Float = ln(value.toDouble()).toFloat()

    /**
     * Returns the base-10 logarithm of a float.
     *
     * @param value the float value
     * @return the base-10 logarithm
     */
    @JvmStatic
    fun log10(value: Float): Float = kotlin.math.log10(value.toDouble()).toFloat()

    /**
     * Returns the maximum of a float and an int.
     *
     * @param value the float
     * @param other the int
     * @return the larger value as a float
     */
    @JvmStatic
    fun max(value: Float, other: Int): Float = maxOf(value, other.toFloat())

    /**
     * Returns the maximum of a float and a long.
     *
     * @param value the float
     * @param other the long
     * @return the larger value as a float
     */
    @JvmStatic
    fun max(value: Float, other: Long): Float = maxOf(value, other.toFloat())

    /**
     * Returns the maximum of two floats.
     *
     * @param value the first float
     * @param other the second float
     * @return the larger value
     */
    @JvmStatic
    fun max(value: Float, other: Float): Float = maxOf(value, other)

    /**
     * Returns the maximum of a float and a double.
     *
     * @param value the float
     * @param other the double
     * @return the larger value as a double
     */
    @JvmStatic
    fun max(value: Float, other: Double): Double = maxOf(value.toDouble(), other)

    /**
     * Returns the minimum of a float and an int.
     *
     * @param value the float
     * @param other the int
     * @return the smaller value as a float
     */
    @JvmStatic
    fun min(value: Float, other: Int): Float = minOf(value, other.toFloat())

    /**
     * Returns the minimum of a float and a long.
     *
     * @param value the float
     * @param other the long
     * @return the smaller value as a float
     */
    @JvmStatic
    fun min(value: Float, other: Long): Float = minOf(value, other.toFloat())

    /**
     * Returns the minimum of two floats.
     *
     * @param value the first float
     * @param other the second float
     * @return the smaller value
     */
    @JvmStatic
    fun min(value: Float, other: Float): Float = minOf(value, other)

    /**
     * Returns the minimum of a float and a double.
     *
     * @param value the float
     * @param other the double
     * @return the smaller value as a double
     */
    @JvmStatic
    fun min(value: Float, other: Double): Double = minOf(value.toDouble(), other)

    /**
     * Raises a float to the given power.
     *
     * @param value the base
     * @param exponent the exponent
     * @return the result as a double
     */
    @JvmStatic
    fun pow(value: Float, exponent: Double): Double = value.toDouble().pow(exponent)

    /**
     * Returns the rounded value of a float as an int.
     *
     * @param value the float value
     * @return the rounded integer value
     */
    @JvmStatic
    fun round(value: Float): Int = kotlin.math.round(value).toInt()

    /**
     * Converts a float to a string.
     *
     * @param value the float value
     * @return the string representation
     */
    @JvmStatic
    fun asString(value: Float): String = value.toString()

    /**
     * Converts a float to a double.
     *
     * @param value the float value
     * @return the double value
     */
    @JvmStatic
    fun asDouble(value: Float): Double = value.toDouble()

    /**
     * Converts a float to an int.
     *
     * @param value the float value
     * @return the int value
     */
    @JvmStatic
    fun asInt(value: Float): Int = value.toInt()

    /**
     * Converts a float to a long.
     *
     * @param value the float value
     * @return the long value
     */
    @JvmStatic
    fun asLong(value: Float): Long = value.toLong()

    /**
     * Checks if the float is NaN.
     *
     * @param value the float value
     * @return true if NaN
     */
    @JvmStatic
    fun isNaN(value: Float): Boolean = value.isNaN()

    /**
     * Checks if the float is infinite.
     *
     * @param value the float value
     * @return true if infinite
     */
    @JvmStatic
    fun isInfinite(value: Float): Boolean = value.isInfinite()
}

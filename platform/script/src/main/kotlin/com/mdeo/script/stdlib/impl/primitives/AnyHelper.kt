package com.mdeo.script.stdlib.impl.primitives

import java.text.MessageFormat

/**
 * Helper methods for the Any type (Object) in the script language.
 * These static methods are called with the instance as the first parameter.
 */
object AnyHelper {

    /**
     * Converts an object to a boolean.
     * - Booleans are returned as-is
     * - Strings "true" (case-insensitive) return true
     * - Numbers != 0 return true
     * - Null returns false
     * - Other objects return true
     *
     * @param obj the object to convert
     * @return the boolean representation
     */
    @JvmStatic
    fun asBoolean(obj: Any?): Boolean {
        return when (obj) {
            null -> false
            is Boolean -> obj
            is String -> obj.equals("true", ignoreCase = true)
            is Number -> obj.toDouble() != 0.0
            else -> true
        }
    }

    /**
     * Converts an object to an integer.
     *
     * @param obj the object to convert
     * @return the integer representation
     * @throws NumberFormatException if the object cannot be converted
     */
    @JvmStatic
    fun asInteger(obj: Any?): Int {
        return when (obj) {
            null -> 0
            is Number -> obj.toInt()
            is String -> obj.toInt()
            is Boolean -> if (obj) 1 else 0
            else -> throw NumberFormatException("Cannot convert ${obj::class.simpleName} to Integer")
        }
    }

    /**
     * Converts an object to a double (real number).
     *
     * @param obj the object to convert
     * @return the double representation
     * @throws NumberFormatException if the object cannot be converted
     */
    @JvmStatic
    fun asReal(obj: Any?): Double {
        return asDouble(obj)
    }

    /**
     * Converts an object to a double.
     *
     * @param obj the object to convert
     * @return the double representation
     * @throws NumberFormatException if the object cannot be converted
     */
    @JvmStatic
    fun asDouble(obj: Any?): Double {
        return when (obj) {
            null -> 0.0
            is Number -> obj.toDouble()
            is String -> obj.toDouble()
            is Boolean -> if (obj) 1.0 else 0.0
            else -> throw NumberFormatException("Cannot convert ${obj::class.simpleName} to Double")
        }
    }

    /**
     * Converts an object to a float.
     *
     * @param obj the object to convert
     * @return the float representation
     * @throws NumberFormatException if the object cannot be converted
     */
    @JvmStatic
    fun asFloat(obj: Any?): Float {
        return when (obj) {
            null -> 0.0f
            is Number -> obj.toFloat()
            is String -> obj.toFloat()
            is Boolean -> if (obj) 1.0f else 0.0f
            else -> throw NumberFormatException("Cannot convert ${obj::class.simpleName} to Float")
        }
    }

    /**
     * Converts an object to its string representation.
     *
     * @param obj the object to convert
     * @return the string representation
     */
    @JvmStatic
    fun asString(obj: Any?): String {
        return obj?.toString() ?: "null"
    }

    /**
     * Formats the object using a MessageFormat pattern.
     *
     * @param obj the object to format
     * @param pattern the format pattern
     * @return the formatted string
     */
    @JvmStatic
    fun format(obj: Any?, pattern: String): String {
        return MessageFormat.format(pattern, obj)
    }

    /**
     * Checks if an object has a property with the given name.
     *
     * @param obj the object to check
     * @param name the property name
     * @return true if the object has the property
     */
    @JvmStatic
    fun hasProperty(obj: Any?, name: String): Boolean {
        if (obj == null) return false
        if (obj is Map<*, *>) {
            return obj.containsKey(name)
        }
        return try {
            obj::class.java.getMethod(name) != null ||
                obj::class.java.getMethod("get${name.replaceFirstChar { it.uppercaseChar() }}") != null ||
                obj::class.java.getField(name) != null
        } catch (_: NoSuchMethodException) {
            try {
                obj::class.java.getField(name) != null
            } catch (_: NoSuchFieldException) {
                false
            }
        } catch (_: NoSuchFieldException) {
            false
        }
    }

    /**
     * Checks if an object is an instance of the given type.
     *
     * @param obj the object to check
     * @param type the type to check against
     * @return true if the object is an instance of the type
     */
    @JvmStatic
    fun instanceOf(obj: Any?, type: Class<*>): Boolean {
        if (obj == null) return false
        return type.isInstance(obj)
    }

    /**
     * Checks if an object is exactly of the given type (not a subtype).
     *
     * @param obj the object to check
     * @param type the type to check against
     * @return true if the object is exactly of the type
     */
    @JvmStatic
    fun isTypeOf(obj: Any?, type: Class<*>): Boolean {
        if (obj == null) return false
        return obj::class.java == type
    }

    /**
     * Gets the type (class) of an object.
     *
     * @param obj the object to get the type of
     * @return the class of the object, or null if the object is null
     */
    @JvmStatic
    fun type(obj: Any?): Class<*>? {
        return obj?.javaClass
    }
}

package com.mdeo.script.stdlib.impl.primitives

import com.mdeo.script.stdlib.impl.collections.ListImpl
import com.mdeo.script.stdlib.impl.collections.ScriptList

/**
 * Helper methods for the String type in the script language.
 * These static methods are called with the instance as the first parameter.
 */
object StringHelper {

    /**
     * Returns the character at the specified index.
     *
     * @param str the string
     * @param index the index (0-based)
     * @return the character at the index as a string
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @JvmStatic
    fun characterAt(str: String, index: Int): String {
        if (index < 0 || index >= str.length) {
            throw IndexOutOfBoundsException("Index: $index, Length: ${str.length}")
        }
        return str[index].toString()
    }

    /**
     * Concatenates this string with another string.
     *
     * @param str the first string
     * @param other the string to append
     * @return the concatenated string
     */
    @JvmStatic
    fun concat(str: String, other: String): String = str + other

    /**
     * Checks if the string ends with the specified suffix.
     *
     * @param str the string
     * @param suffix the suffix to check
     * @return true if the string ends with the suffix
     */
    @JvmStatic
    fun endsWith(str: String, suffix: String): Boolean = str.endsWith(suffix)

    /**
     * Converts the first character to lowercase.
     *
     * @param str the string
     * @return the string with the first character in lowercase
     */
    @JvmStatic
    fun firstToLowerCase(str: String): String {
        if (str.isEmpty()) return str
        return str[0].lowercaseChar() + str.substring(1)
    }

    /**
     * Converts the first character to uppercase.
     *
     * @param str the string
     * @return the string with the first character in uppercase
     */
    @JvmStatic
    fun firstToUpperCase(str: String): String {
        if (str.isEmpty()) return str
        return str[0].uppercaseChar() + str.substring(1)
    }

    /**
     * Checks if the string represents a valid integer.
     *
     * @param str the string
     * @return true if the string can be parsed as an integer
     */
    @JvmStatic
    fun isInteger(str: String): Boolean {
        return try {
            str.trim().toInt()
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    /**
     * Checks if the string represents a valid real number (double).
     *
     * @param str the string
     * @return true if the string can be parsed as a real number
     */
    @JvmStatic
    fun isReal(str: String): Boolean {
        return try {
            str.trim().toDouble()
            true
        } catch (_: NumberFormatException) {
            false
        }
    }

    /**
     * Checks if this string is a substring of another string.
     *
     * @param str the string to check
     * @param other the string to search in
     * @return true if this string is found within the other string
     */
    @JvmStatic
    fun isSubstringOf(str: String, other: String): Boolean = other.contains(str)

    /**
     * Returns the length of the string.
     *
     * @param str the string
     * @return the number of characters in the string
     */
    @JvmStatic
    fun length(str: String): Int = str.length

    /**
     * Checks if the string matches the given regular expression.
     *
     * @param str the string
     * @param regex the regular expression pattern
     * @return true if the string matches the pattern
     */
    @JvmStatic
    fun matches(str: String, regex: String): Boolean = str.matches(Regex(regex))

    /**
     * Pads the string to the specified length.
     *
     * @param str the string
     * @param length the target length
     * @param padding the padding string to use
     * @param right true to pad on the right, false to pad on the left
     * @return the padded string
     */
    @JvmStatic
    fun pad(str: String, length: Int, padding: String, right: Boolean): String {
        if (str.length >= length) return str
        val padChar = if (padding.isEmpty()) ' ' else padding[0]
        return if (right) {
            str.padEnd(length, padChar)
        } else {
            str.padStart(length, padChar)
        }
    }

    /**
     * Replaces all occurrences matching the regular expression with the replacement.
     *
     * @param str the string
     * @param regex the regular expression pattern
     * @param replacement the replacement string
     * @return the string with replacements applied
     */
    @JvmStatic
    fun replace(str: String, regex: String, replacement: String): String {
        return str.replace(Regex(regex), replacement)
    }

    /**
     * Splits the string by the given regular expression.
     *
     * @param str the string
     * @param regex the regular expression pattern to split by
     * @return a list of substrings
     */
    @JvmStatic
    fun split(str: String, regex: String): ScriptList<String> {
        val parts = str.split(Regex(regex))
        val list = ListImpl<String>()
        for (part in parts) {
            list.add(part)
        }
        return list
    }

    /**
     * Checks if the string starts with the specified prefix.
     *
     * @param str the string
     * @param prefix the prefix to check
     * @return true if the string starts with the prefix
     */
    @JvmStatic
    fun startsWith(str: String, prefix: String): Boolean = str.startsWith(prefix)

    /**
     * Returns a substring starting at the given index.
     *
     * @param str the string
     * @param startIndex the starting index (0-based)
     * @return the substring from the starting index to the end
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @JvmStatic
    fun substring(str: String, startIndex: Int): String {
        if (startIndex < 0 || startIndex > str.length) {
            throw IndexOutOfBoundsException("Start index: $startIndex, Length: ${str.length}")
        }
        return str.substring(startIndex)
    }

    /**
     * Returns a substring between the given indices.
     *
     * @param str the string
     * @param startIndex the starting index (0-based, inclusive)
     * @param endIndex the ending index (exclusive)
     * @return the substring between the indices
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    @JvmStatic
    fun substring(str: String, startIndex: Int, endIndex: Int): String {
        if (startIndex < 0 || endIndex > str.length || startIndex > endIndex) {
            throw IndexOutOfBoundsException("Start: $startIndex, End: $endIndex, Length: ${str.length}")
        }
        return str.substring(startIndex, endIndex)
    }

    /**
     * Converts the string to a list of single-character strings.
     *
     * @param str the string
     * @return a list of character strings
     */
    @JvmStatic
    fun toCharSequence(str: String): ScriptList<String> {
        val list = ListImpl<String>()
        for (char in str) {
            list.add(char.toString())
        }
        return list
    }

    /**
     * Converts the string to lowercase.
     *
     * @param str the string
     * @return the lowercase string
     */
    @JvmStatic
    fun toLowerCase(str: String): String = str.lowercase()

    /**
     * Converts the string to uppercase.
     *
     * @param str the string
     * @return the uppercase string
     */
    @JvmStatic
    fun toUpperCase(str: String): String = str.uppercase()

    /**
     * Trims whitespace from both ends of the string.
     *
     * @param str the string
     * @return the trimmed string
     */
    @JvmStatic
    fun trim(str: String): String = str.trim()

    /**
     * Converts the string to an integer.
     *
     * @param str the string
     * @return the integer value
     * @throws NumberFormatException if the string is not a valid integer
     */
    @JvmStatic
    fun asInteger(str: String): Int = str.trim().toInt()

    /**
     * Converts the string to a long.
     *
     * @param str the string
     * @return the long value
     * @throws NumberFormatException if the string is not a valid long
     */
    @JvmStatic
    fun asLong(str: String): Long = str.trim().toLong()

    /**
     * Converts the string to a float.
     *
     * @param str the string
     * @return the float value
     * @throws NumberFormatException if the string is not a valid float
     */
    @JvmStatic
    fun asFloat(str: String): Float = str.trim().toFloat()

    /**
     * Converts the string to a double.
     *
     * @param str the string
     * @return the double value
     * @throws NumberFormatException if the string is not a valid double
     */
    @JvmStatic
    fun asDouble(str: String): Double = str.trim().toDouble()

    /**
     * Converts the string to a boolean.
     *
     * @param str the string
     * @return true if the string equals "true" (case-insensitive)
     */
    @JvmStatic
    fun asBoolean(str: String): Boolean = str.trim().equals("true", ignoreCase = true)

    /**
     * Checks if the string contains the specified substring.
     *
     * @param str the string
     * @param other the substring to search for
     * @return true if the string contains the substring
     */
    @JvmStatic
    fun contains(str: String, other: String): Boolean = str.contains(other)

    /**
     * Returns the index of the first occurrence of the specified substring.
     *
     * @param str the string
     * @param other the substring to search for
     * @return the index of the first occurrence, or -1 if not found
     */
    @JvmStatic
    fun indexOf(str: String, other: String): Int = str.indexOf(other)

    /**
     * Returns the index of the last occurrence of the specified substring.
     *
     * @param str the string
     * @param other the substring to search for
     * @return the index of the last occurrence, or -1 if not found
     */
    @JvmStatic
    fun lastIndexOf(str: String, other: String): Int = str.lastIndexOf(other)

    /**
     * Checks if the string is empty.
     *
     * @param str the string
     * @return true if the string has length 0
     */
    @JvmStatic
    fun isEmpty(str: String): Boolean = str.isEmpty()

    /**
     * Checks if the string is blank (empty or contains only whitespace).
     *
     * @param str the string
     * @return true if the string is blank
     */
    @JvmStatic
    fun isBlank(str: String): Boolean = str.isBlank()

    /**
     * Returns the string itself.
     *
     * @param str the string
     * @return the string unchanged
     */
    @JvmStatic
    fun toString(str: String): String = str
}

package com.mdeo.script.compiler.util

import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.util.ASMUtil

/**
 * Utility object for building JVM method descriptors.
 *
 * Centralizes the logic for creating method descriptors from various parameter
 * and return type representations to avoid duplication across the codebase.
 */
object MethodDescriptorUtil {

    /**
     * Builds a JVM method descriptor from parameter types and return type.
     *
     * @param parameterTypes The types of the method parameters.
     * @param returnType The return type of the method.
     * @return The JVM method descriptor (e.g., "(IJ)D" for (int, long) -> double).
     */
    fun buildDescriptor(
        parameterTypes: List<ReturnType>,
        returnType: ReturnType
    ): String {
        val paramDescriptors = parameterTypes.joinToString("") {
            ASMUtil.getTypeDescriptor(it)
        }
        val returnDescriptor = ASMUtil.getTypeDescriptor(returnType)
        return "($paramDescriptors)$returnDescriptor"
    }
}

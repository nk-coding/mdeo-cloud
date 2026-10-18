package com.mdeo.modeltransformation.ast

import com.mdeo.expression.ast.expressions.TypedExpression
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * The signature of a callable transformation: `transformation name(in a: T = d, out b: U)`.
 *
 * @param name The name the transformation is called by.
 * @param parameters The parameters, in declaration order.
 */
@Serializable
data class TypedTransformationSignature(
    val name: String,
    val parameters: List<TypedTransformationParameter> = emptyList()
)

/**
 * A parameter of a signature. The engine binds every parameter at the transformation's own
 * scope level (1) before the first statement: an `in` parameter to its argument (or default),
 * an `out` parameter to `null` until a pattern assigns it.
 *
 * @param name The parameter's name.
 * @param direction `in` or `out`.
 * @param type Index into the transformation's types array.
 * @param isElement Whether the parameter is a model element (a node of the graph) rather than a
 *   scalar value.
 * @param defaultValue The default of an optional input, evaluated in the callee.
 */
@Serializable
data class TypedTransformationParameter(
    val name: String,
    val direction: String,
    val type: Int,
    val isElement: Boolean = false,
    @Contextual val defaultValue: TypedExpression? = null
) {
    /** Whether this is an input. */
    val isInput: Boolean get() = direction == "in"

    /** Whether this is an output. */
    val isOutput: Boolean get() = direction == "out"
}

package com.mdeo.expression.ast.expressions

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Argument for an extension call.
 *
 * @param name Name of the argument.
 * @param value The value expression of the argument.
 */
@Serializable
data class TypedExtensionCallArgument(
    val name: String,
    @Contextual
    val value: TypedExpression
)

/**
 * Extension call expression.
 * Represents a virtual function call for a custom extension expression.
 *
 * @param kind The kind of expression.
 * @param evalType Index into the types array for the type this expression evaluates to.
 * @param name The name of the function being called.
 * @param arguments Array of named arguments. Caution: order matters for extension calls!
 *                  The same name can appear multiple times, it's the responsibility of the
 *                  interpreter to resolve them in the correct order.
 * @param overload Overload identifier for the extension function being called.
 */
@Serializable
data class TypedExtensionCallExpression(
    override val kind: String = "extensionCall",
    override val evalType: Int,
    val name: String,
    val arguments: List<TypedExtensionCallArgument>,
    val overload: String
) : TypedExpression

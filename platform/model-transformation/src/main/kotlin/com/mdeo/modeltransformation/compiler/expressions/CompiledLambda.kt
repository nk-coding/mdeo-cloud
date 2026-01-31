package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression

/**
 * Represents a compiled lambda expression that can be evaluated with argument values.
 *
 * This is a placeholder implementation for the transition to traversal-based compilation.
 * Lambda support in traversal mode is not yet implemented.
 *
 * @param parameters The names of the lambda parameters.
 * @param body The lambda body expression.
 */
class CompiledLambda(
    val parameters: List<String>,
    val body: TypedExpression
) {
    /**
     * The arity (number of parameters) of this lambda.
     */
    val arity: Int get() = parameters.size

    /**
     * Evaluates the lambda with a single argument.
     *
     * @param argument The argument value to pass to the lambda.
     * @return The result of evaluating the lambda body.
     * @throws NotImplementedError Lambda evaluation is not yet implemented for traversal mode.
     */
    fun evaluate(argument: Any?): Any? {
        throw NotImplementedError(
            "Lambda evaluation is not yet implemented in traversal mode. " +
            "Use the legacy expression compiler for lambda support."
        )
    }

    /**
     * Evaluates the lambda with multiple arguments.
     *
     * @param arguments The argument values to pass to the lambda parameters.
     * @return The result of evaluating the lambda body.
     * @throws NotImplementedError Lambda evaluation is not yet implemented for traversal mode.
     */
    fun evaluate(vararg arguments: Any?): Any? {
        throw NotImplementedError(
            "Lambda evaluation is not yet implemented in traversal mode. " +
            "Use the legacy expression compiler for lambda support."
        )
    }
}

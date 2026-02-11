package com.mdeo.modeltransformation.compiler

import com.mdeo.expression.ast.expressions.TypedExpression

/**
 * Exception thrown when expression compilation fails.
 *
 * This exception provides detailed information about compilation failures,
 * including the expression that caused the failure and an optional cause.
 * It is used throughout the expression compiler infrastructure to signal
 * errors in a consistent and informative way.
 *
 * @param message A description of what went wrong during compilation.
 * @param expression The expression that caused the compilation failure, if available.
 * @param cause The underlying exception that caused this compilation failure, if any.
 */
class CompilationException(
    message: String,
    val expression: TypedExpression? = null,
    cause: Throwable? = null
) : RuntimeException(buildMessage(message, expression), cause) {
    
    companion object {
        /**
         * Builds a detailed error message including expression information.
         *
         * @param message The base error message.
         * @param expression The expression that caused the error.
         * @return A formatted error message.
         */
        private fun buildMessage(message: String, expression: TypedExpression?): String {
            return if (expression != null) {
                "$message (expression kind: ${expression.kind}, evalType: ${expression.evalType})"
            } else {
                message
            }
        }
        
        /**
         * Creates a CompilationException for an unsupported expression type.
         *
         * @param expression The unsupported expression.
         * @return A CompilationException describing the unsupported type.
         */
        fun unsupportedExpression(expression: TypedExpression): CompilationException {
            return CompilationException(
                "Unsupported expression type: ${expression.kind}",
                expression
            )
        }
        
        /**
         * Creates a CompilationException for an unsupported operator.
         *
         * @param operator The unsupported operator.
         * @param expression The expression containing the operator.
         * @return A CompilationException describing the unsupported operator.
         */
        fun unsupportedOperator(operator: String, expression: TypedExpression): CompilationException {
            return CompilationException(
                "Unsupported operator: $operator",
                expression
            )
        }
        
        /**
         * Creates a CompilationException for a type resolution failure.
         *
         * @param evalType The type index that could not be resolved.
         * @param expression The expression with the unresolvable type.
         * @return A CompilationException describing the type resolution failure.
         */
        fun typeResolutionFailed(evalType: Int, expression: TypedExpression): CompilationException {
            return CompilationException(
                "Failed to resolve type at index: $evalType",
                expression
            )
        }
        
        /**
         * Creates a CompilationException for an unresolved variable.
         *
         * @param variableName The name of the unresolved variable.
         * @param scopeIndex The scope index where the variable was looked up.
         * @param expression The expression referencing the variable.
         * @return A CompilationException describing the unresolved variable.
         */
        fun unresolvedVariable(
            variableName: String,
            scopeIndex: Int,
            expression: TypedExpression
        ): CompilationException {
            return CompilationException(
                "Unresolved variable '$variableName' in scope $scopeIndex",
                expression
            )
        }
        
    }
}

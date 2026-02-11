package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.expressions.*
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.VariableBinding

/**
 * Analyzes match patterns and expressions to extract referenced instance names.
 *
 * Stateful analyzer that accumulates references across multiple analyze* calls.
 *
 * @param scope The variable scope in which context this is analyzed (used to determine if identifiers are relevant)
 */
class MatchAnalyzer(val scope: VariableScope) {

    private val _referencedInstances: MutableSet<String> = mutableSetOf()

    fun getReferencedInstances(): Set<String> = _referencedInstances.toSet()

    fun analyzeObjectInstance(element: TypedPatternObjectInstanceElement) {
        for (property in element.objectInstance.properties) {
            analyzeExpression(property.value)
        }
    }

    fun analyzeLink(element: TypedPatternLinkElement) {
        _referencedInstances.add(element.link.source.objectName)
        _referencedInstances.add(element.link.target.objectName)
    }

    fun analyzeWhereClause(element: TypedPatternWhereClauseElement) {
        analyzeExpression(element.whereClause.expression)
    }

    fun analyzeVariable(element: TypedPatternVariableElement) {
        analyzeExpression(element.variable.value)
    }

    /**
     * Recursively analyzes an expression to extract all referenced identifiers.
     */
    fun analyzeExpression(expression: TypedExpression) {
        when (expression) {
            is TypedIdentifierExpression -> {
                if (expression.scope <= scope.scopeIndex) {
                    val binding = scope.getVariable(expression.name)
                    if (binding is VariableBinding.InstanceBinding) {
                        _referencedInstances.add(expression.name)
                    }
                }
            }
            is TypedMemberAccessExpression -> {
                analyzeExpression(expression.expression)
            }
            is TypedBinaryExpression -> {
                analyzeExpression(expression.left)
                analyzeExpression(expression.right)
            }
            is TypedUnaryExpression -> {
                analyzeExpression(expression.expression)
            }
            is TypedMemberCallExpression -> {
                analyzeExpression(expression.expression)
                expression.arguments.forEach { analyzeExpression(it) }
            }
            is TypedFunctionCallExpression -> {
                expression.arguments.forEach { analyzeExpression(it) }
            }
            is TypedExpressionCallExpression -> {
                analyzeExpression(expression.expression)
                expression.arguments.forEach { analyzeExpression(it) }
            }
            is TypedExtensionCallExpression -> {
                expression.arguments.forEach { arg ->
                    analyzeExpression(arg.value)
                }
            }
            is TypedTernaryExpression -> {
                analyzeExpression(expression.condition)
                analyzeExpression(expression.trueExpression)
                analyzeExpression(expression.falseExpression)
            }
            is TypedListLiteralExpression -> {
                expression.elements.forEach { analyzeExpression(it) }
            }
            is TypedTypeCheckExpression -> {
                analyzeExpression(expression.expression)
            }
            is TypedTypeCastExpression -> {
                analyzeExpression(expression.expression)
            }
            is TypedAssertNonNullExpression -> {
                analyzeExpression(expression.expression)
            }
            is TypedLambdaExpression -> {
                analyzeExpression(expression.body)
            }
            is TypedStringLiteralExpression,
            is TypedIntLiteralExpression,
            is TypedLongLiteralExpression,
            is TypedFloatLiteralExpression,
            is TypedDoubleLiteralExpression,
            is TypedBooleanLiteralExpression,
            is TypedNullLiteralExpression -> {
                // No references in literals
            }

            else -> {
                throw IllegalArgumentException("Unsupported expression type: ${expression::class}")
            }
        }
    }
}

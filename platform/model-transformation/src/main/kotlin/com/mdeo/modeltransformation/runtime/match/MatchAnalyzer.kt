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

    /**
     * Returns all instance names that have been referenced during analysis.
     *
     * @return An immutable set of instance names that were referenced
     */
    fun getReferencedInstances(): Set<String> = _referencedInstances.toSet()

    /**
     * Analyzes an object instance element to extract referenced instances from property values.
     *
     * Examines all property assignments in the object instance and recursively analyzes
     * their value expressions to find instance references.
     *
     * @param element The object instance element to analyze
     */
    fun analyzeObjectInstance(element: TypedPatternObjectInstanceElement) {
        for (property in element.objectInstance.properties) {
            analyzeExpression(property.value)
        }
    }

    /**
     * Analyzes a link element to extract source and target instance references.
     *
     * Adds both the source and target object names to the referenced instances set.
     *
     * @param element The link element to analyze
     */
    fun analyzeLink(element: TypedPatternLinkElement) {
        _referencedInstances.add(element.link.source.objectName)
        _referencedInstances.add(element.link.target.objectName)
    }

    /**
     * Analyzes a where clause element to extract referenced instances from the condition expression.
     *
     * @param element The where clause element to analyze
     */
    fun analyzeWhereClause(element: TypedPatternWhereClauseElement) {
        analyzeExpression(element.whereClause.expression)
    }

    /**
     * Analyzes a variable element to extract referenced instances from the variable's value expression.
     *
     * @param element The variable element to analyze
     */
    fun analyzeVariable(element: TypedPatternVariableElement) {
        analyzeExpression(element.variable.value)
    }

    /**
     * Recursively analyzes an expression to extract all referenced identifiers.
     *
     * Traverses the expression tree and identifies identifier expressions that reference
     * instance bindings within the current scope. For complex expressions (binary, unary,
     * member access, function calls, etc.), recursively analyzes all sub-expressions.
     *
     * Only identifiers that:
     * 1. Have a scope index <= current scope index
     * 2. Resolve to InstanceBinding in the variable scope
     * are added to the referenced instances set.
     *
     * @param expression The expression to analyze
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

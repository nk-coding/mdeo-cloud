package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.modeltransformation.compiler.CompilationException
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.TraversalCompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompiler
import com.mdeo.modeltransformation.compiler.VariableBinding
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Traversal-based compiler for [TypedIdentifierExpression] nodes.
 *
 * Compiles identifier expressions into [TraversalCompilationResult] containing GraphTraversals
 * that reference variables bound in the match context or from variable scopes.
 *
 * ## Match Context Resolution
 * In match context, identifiers typically reference variables that were bound earlier
 * in the match pattern. These are resolved using `__.select("varName")` to retrieve
 * the value bound to that variable during pattern matching.
 *
 * ## Model Transformation Scope (Scope Level 1)
 * When scope is 1 and a transformation context is available, identifiers are resolved:
 * - Match-defined variables: Use `__.select()` for dynamic binding
 * - Named instances: Resolved via vertex ID as constant
 * - Variables: Resolved via value as constant
 *
 * ## Standard Scope Resolution
 * For other scopes, variables are resolved from the variable scope map:
 * - [VariableBinding.ValueBinding]: Produces a constant traversal
 * - [VariableBinding.TraversalBinding]: Uses `__.select()` to reference the step label
 * - [VariableBinding.InstanceBinding]: Produces a constant with the vertex ID
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided, `select()` is appended to it.
 * Otherwise, a new anonymous traversal is created with `__.select()`.
 *
 * @see TypedIdentifierExpression
 */
class IdentifierCompiler : ExpressionCompiler {

    companion object {
        /**
         * The scope index for model transformation scope.
         * Identifiers at this scope are resolved from the transformation context.
         */
        const val MT_SCOPE_INDEX = 1
    }

    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedIdentifierExpression
    }

    override fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        val identifierExpression = expression as TypedIdentifierExpression
        val binding = resolveVariable(identifierExpression, context)
        return compileBinding(binding, initialTraversal)
    }

    private fun resolveVariable(
        expression: TypedIdentifierExpression,
        context: TraversalCompilationContext
    ): VariableBinding {
        if (expression.scope == MT_SCOPE_INDEX && context.hasMTScope()) {
            return resolveMTScopeIdentifier(expression, context)
        }
        return resolveStandardScope(expression, context)
    }

    /**
     * Resolves an identifier from the model transformation scope.
     *
     * Handles three cases:
     * 1. Match-defined variables: Use dynamic binding via TraversalBinding
     * 2. Named instances: Resolve to InstanceBinding with vertex ID
     * 3. Variables: Resolve to ValueBinding with the stored value
     */
    private fun resolveMTScopeIdentifier(
        expression: TypedIdentifierExpression,
        context: TraversalCompilationContext
    ): VariableBinding {
        val name = expression.name
        val txContext = context.transformationContext!!

        if (context.isMatchDefinedVariable(name)) {
            return VariableBinding.TraversalBinding(name)
        }

        val instanceId = txContext.lookupInstance(name)
        if (instanceId != null) {
            return VariableBinding.InstanceBinding(instanceId, name)
        }

        if (txContext.hasVariable(name)) {
            val value = txContext.lookupVariable(name)
            return VariableBinding.ValueBinding(value)
        }

        throw CompilationException.unresolvedVariable(name, expression.scope, expression)
    }

    /**
     * Resolves an identifier from a standard (non-MT) scope.
     */
    private fun resolveStandardScope(
        expression: TypedIdentifierExpression,
        context: TraversalCompilationContext
    ): VariableBinding {
        val scope = context.getScope(expression.scope)
            ?: throw CompilationException(
                "Scope not found at index ${expression.scope}",
                expression
            )

        return scope.getVariable(expression.name)
            ?: throw CompilationException.unresolvedVariable(
                expression.name,
                expression.scope,
                expression
            )
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileBinding(
        binding: VariableBinding,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<*, *> {
        return when (binding) {
            is VariableBinding.ValueBinding -> {
                TraversalCompilationResult.constant(binding.value, initialTraversal)
            }
            is VariableBinding.TraversalBinding -> {
                compileTraversalBinding(binding.stepLabel, initialTraversal)
            }
            is VariableBinding.InstanceBinding -> {
                TraversalCompilationResult.constant(binding.vertexId, initialTraversal)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun compileTraversalBinding(
        stepLabel: String,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<Any, Any> {
        val traversal: GraphTraversal<Any, Any> = if (initialTraversal != null) {
            initialTraversal.select<Any>(stepLabel) as GraphTraversal<Any, Any>
        } else {
            AnonymousTraversal.select<Any, Any>(stepLabel)
        }
        return TraversalCompilationResult.of(traversal)
    }
}

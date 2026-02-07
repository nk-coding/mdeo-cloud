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
 * ## Scope Resolution
 * Identifiers are resolved from the variable scope map based on the scope index
 * assigned by the TypeScript type checker. The scope index represents nesting depth:
 * - Scope 0: Global scope (built-ins)
 * - Scope 1: Top-level model transformation scope
 * - Scope 2+: Nested scopes (if-match, while-match, for-match conditions, etc.)
 *
 * Variable bindings can be:
 * - [VariableBinding.ValueBinding]: Produces a constant traversal
 * - [VariableBinding.TraversalBinding]: Uses `__.select()` to reference the step label
 * - [VariableBinding.InstanceBinding]: Uses `__.V(id)` to reference a specific vertex
 *
 * ## Important Note on Scope Indices
 * The scope index is determined by the TypeScript type checker based on AST nesting.
 * Match statements can appear at any nesting level (inside if/while/for blocks),
 * so we cannot assume any particular scope index for match variables.
 * The caller (e.g., MatchExecutor.buildCompilationContextWithTransformation) is
 * responsible for setting up the variableScopes map with bindings at all relevant
 * scope levels.
 *
 * ## Initial Traversal Handling
 * When an [initialTraversal] is provided, `select()` is appended to it.
 * Otherwise, a new anonymous traversal is created with `__.select()`.
 *
 * @see TypedIdentifierExpression
 */
class IdentifierCompiler : ExpressionCompiler {

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
        return compileBinding(binding, context, initialTraversal)
    }

    /**
     * Resolves an identifier to its variable binding from the scope.
     *
     * All identifiers are resolved uniformly from the variableScopes map.
     * The caller is responsible for setting up the correct bindings at all
     * scope levels where variables should be accessible.
     */
    private fun resolveVariable(
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
        context: TraversalCompilationContext,
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
                compileInstanceBinding(binding, context, initialTraversal)
            }
        }
    }
    
    /**
     * Compiles an instance binding to a traversal that references the vertex.
     * 
     * Uses __.V(vertexId) to create an ANONYMOUS traversal that starts from the specific vertex.
     * This allows subsequent property access like .values("address") to work correctly.
     * 
     * IMPORTANT: We always use anonymous traversals (__.V) instead of graph-bound (g.V) because:
     * 1. Anonymous traversals can be used in flatMap(), coalesce(), and other steps that expect spawned traversals
     * 2. The calling code (compilePropertyValueWithContext) uses g.inject().flatMap() to execute the result
     * 3. Using g.V() would cause "child traversal was not spawned anonymously" errors
     * 
     * If no traversal source is available, falls back to returning the vertex ID as a constant.
     */
    @Suppress("UNCHECKED_CAST")
    private fun compileInstanceBinding(
        binding: VariableBinding.InstanceBinding,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>?
    ): TraversalCompilationResult<Any, Any> {
        val traversal = if (initialTraversal != null) {
            initialTraversal.flatMap(
                AnonymousTraversal.V<Any>(binding.vertexId)
            ) as GraphTraversal<Any, Any>
        } else {
            AnonymousTraversal.V<Any>(binding.vertexId) as GraphTraversal<Any, Any>
        }
        return TraversalCompilationResult.of(traversal)
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

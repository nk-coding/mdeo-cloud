package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Shared utility for evaluating boolean condition expressions in transformation statements.
 *
 * This class provides common functionality for evaluating conditions in control flow
 * statements like `if` and `while`. It handles:
 * - Compiling expressions using the expression compiler registry
 * - Executing constant expressions directly
 * - Executing dynamic expressions that reference graph instances
 *
 * The implementation uses portable Gremlin patterns (no lambdas) and the g.V(id)
 * pattern for vertex access to ensure compatibility across different Gremlin providers.
 *
 * Usage:
 * ```kotlin
 * val evaluator = ConditionEvaluator(engine)
 * val result = evaluator.evaluate(condition, context)
 * ```
 */
class ConditionEvaluator(private val engine: TransformationEngine) {

    companion object {
        /**
         * Maximum scope depth to register variable bindings at.
         * This should match the value used in MatchExecutor.
         */
        private const val MAX_SCOPE_DEPTH = 5
    }

    /**
     * Evaluates a boolean expression to determine if it's true or false.
     *
     * Compiles the expression using the traversal compiler registry and executes
     * it against the graph database. Handles both constant and dynamic expressions.
     *
     * For constant expressions, the value is returned directly without graph traversal.
     * For dynamic expressions referencing graph instances, the IdentifierCompiler uses
     * __.V(vertexId) to access them directly, avoiding the need for labeling.
     *
     * @param expression The boolean expression to evaluate.
     * @param context The execution context for variable resolution and instance lookup.
     * @return True if the condition evaluates to true, false otherwise.
     * @throws IllegalStateException if no compiler is registered for the expression type
     *         or if the expression evaluation fails.
     */
    fun evaluate(
        expression: TypedExpression,
        context: TransformationExecutionContext
    ): Boolean {
        if (!engine.expressionCompilerRegistry.canCompile(expression)) {
            throw IllegalStateException(
                "Expression compiler not found for expression of type '${expression::class.simpleName}'. " +
                "Ensure a compiler is registered for this expression kind."
            )
        }

        return try {
            val instances = context.getAllInstances()
            
            val bindings = instances.mapValues { (name, vertexId) ->
                VariableBinding.InstanceBinding(vertexId, name)
            }
            val variableScope = VariableScope(bindings)
            
            val variableScopes = (0..MAX_SCOPE_DEPTH).associateWith { variableScope }

            val compilationContext = TraversalCompilationContext(
                types = engine.types,
                traversalSource = engine.traversalSource,
                typeRegistry = engine.typeRegistry,
                variableScopes = variableScopes,
                matchDefinedVariables = instances.keys
            )
            val result = engine.expressionCompilerRegistry.compile(expression, compilationContext)

            if (result.isConstant) {
                return result.constantValue as? Boolean ?: false
            }

            executeConditionTraversal(result.traversal)
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to evaluate condition expression '${expression::class.simpleName}': ${e.message}",
                e
            )
        }
    }

    /**
     * Executes a condition traversal against the graph database.
     *
     * Since we use InstanceBinding with __.V(vertexId), the IdentifierCompiler
     * generates anonymous traversals that directly access vertices by ID.
     * We simply need to inject a starting element and execute the traversal.
     *
     * @param traversal The compiled condition traversal.
     * @return The boolean result of the condition.
     */
    @Suppress("UNCHECKED_CAST")
    private fun executeConditionTraversal(
        traversal: GraphTraversal<*, *>
    ): Boolean {
        val result = engine.traversalSource
            .inject(1 as Any)
            .flatMap(traversal as GraphTraversal<*, Boolean>)
        
        if (result.hasNext()) {
            return result.next()
        } else {
            return false
        }
    }
}

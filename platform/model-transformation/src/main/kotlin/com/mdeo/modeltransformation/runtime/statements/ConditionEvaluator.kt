package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.expression.ast.expressions.*
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.match.MatchAnalyzer
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Shared utility for evaluating boolean condition expressions in transformation statements.
 *
 * This class provides common functionality for evaluating conditions in control flow
 * statements like `if` and `while`. It handles:
 * - Analyzing expressions to extract referenced identifiers using MatchAnalyzer
 * - Setting up a traversal context where instances are available via step labels
 * - Compiling expressions using the expression compiler registry
 * - Executing constant expressions directly
 * - Executing dynamic expressions that reference graph instances
 *
 * The implementation uses portable Gremlin patterns (no lambdas) and follows the
 * same binding approach as MatchExecutor for consistency.
 */
class ConditionEvaluator(private val engine: TransformationEngine) {

    /**
     * Evaluates a boolean expression to determine if it's true or false.
     *
     * Analyzes the expression to find referenced identifiers using MatchAnalyzer,
     * sets up InstanceBindings for them (if they reference instances), then compiles
     * and executes the expression in a traversal context where instances are available
     * via step labels.
     *
     * For constant expressions, the value is returned directly without graph traversal.
     * For dynamic expressions, a traversal is built that:
     * 1. Starts with inject(1)
     * 2. For each referenced instance, adds .V(vertexId).as(stepLabel)
     * 3. Executes the condition expression using flatMap()
     *
     * This ensures that when IdentifierCompiler uses select(stepLabel), the instances
     * are available in the traversal context.
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
            val analyzer = MatchAnalyzer(context.variableScope)
            analyzer.analyzeExpression(expression)
            val referencedIdentifiers = analyzer.getReferencedInstances()
            
            val instances = context.getAllInstances()
            val scope = context.variableScope
            
            for (name in referencedIdentifiers) {
                if (instances.containsKey(name) && scope.getVariable(name) == null) {
                    scope.setBinding(name, VariableBinding.InstanceBinding(vertexId = null))
                }
            }

            val compilationContext = CompilationContext(
                types = engine.types,
                currentScope = context.variableScope,
                traversalSource = engine.traversalSource,
                typeRegistry = engine.typeRegistry
            )
            val result = engine.expressionCompilerRegistry.compile(expression, compilationContext)

            executeConditionTraversal(result.traversal, referencedIdentifiers, instances)
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
     * Builds a traversal that sets up step labels for all referenced instances,
     * then executes the condition expression using flatMap().
     *
     * The traversal structure is:
     * ```
     * g.inject(1)
     *   .V(vertexId1).as("name1")
     *   .V(vertexId2).as("name2")
     *   ...
     *   .flatMap(conditionTraversal)
     * ```
     *
     * This makes instances available via select("name") in the condition traversal.
     *
     * @param traversal The compiled condition traversal.
     * @param referencedIdentifiers Names of identifiers referenced in the expression.
     * @param instances Map of instance names to vertex IDs from the execution context.
     * @return The boolean result of the condition.
     */
    @Suppress("UNCHECKED_CAST")
    private fun executeConditionTraversal(
        traversal: GraphTraversal<*, *>,
        referencedIdentifiers: Set<String>,
        instances: Map<String, Any>
    ): Boolean {
        var setupTraversal: GraphTraversal<Any, Any> = engine.traversalSource.inject(1 as Any) as GraphTraversal<Any, Any>
        
        for (name in referencedIdentifiers) {
            val vertexId = instances[name]
            if (vertexId != null) {
                val stepLabel = VariableBinding.stepLabel(name)
                setupTraversal = setupTraversal.V(vertexId).`as`(stepLabel) as GraphTraversal<Any, Any>
            }
        }
        
        val result = setupTraversal.flatMap(traversal as GraphTraversal<*, Boolean>)
        
        return if (result.hasNext()) {
            result.next()
        } else {
            false
        }
    }
}

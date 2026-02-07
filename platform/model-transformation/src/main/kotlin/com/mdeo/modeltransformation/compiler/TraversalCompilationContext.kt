package com.mdeo.modeltransformation.compiler

import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Context for traversal-based expression compilation.
 *
 * This context provides all necessary information for compiling typed expressions
 * into GraphTraversals. It extends the compilation infrastructure with match-specific
 * settings to support the unified traversal compilation model.
 *
 * ## Match Context
 * When [inMatchContext] is true, expressions are being compiled for use within
 * a Gremlin match() clause. This affects how traversals are constructed:
 * - Traversals typically need to start with `__.as()` to bind to variables
 * - The [currentMatchLabel] provides the label for the current match binding
 *
 * ## Variable Counter
 * The [variableCounter] is used to generate unique variable names for intermediate
 * values in where clauses. This ensures no naming conflicts when multiple
 * expressions use temporary bindings.
 *
 * @param types The list of types referenced by expressions via evalType index.
 *              Indices must be preserved from the TypedAst. Entries may be VoidType or
 *              ValueType subclasses. Only ValueType entries can be used for property lookups.
 * @param variableScopes A map of scope index to variable bindings within that scope
 * @param traversalSource The GraphTraversalSource for building traversals
 * @param transformationContext Optional transformation context for MT scope resolution
 * @param matchDefinedVariables Set of variable names defined in the current match pattern
 * @param typeRegistry The type registry for property and method lookup
 * @param inMatchContext Whether compilation is occurring within a match() clause
 * @param currentMatchLabel The current match label for `__.as()` prefix, if in match
 * @param variableCounter Counter for generating unique intermediate variable names
 */
data class TraversalCompilationContext(
    val types: List<ReturnType>,
    val variableScopes: Map<Int, VariableScope> = emptyMap(),
    val traversalSource: GraphTraversalSource? = null,
    val transformationContext: TransformationExecutionContext? = null,
    val matchDefinedVariables: Set<String> = emptySet(),
    val typeRegistry: GremlinTypeRegistry,
    val inMatchContext: Boolean = false,
    val currentMatchLabel: String? = null,
    val variableCounter: Int = 0
) {
    /**
     * Resolves a type from the types array using the given index.
     *
     * @param evalType The index into the types array
     * @return The [ValueType] at the specified index
     * @throws IndexOutOfBoundsException If the index is out of bounds
     * @throws IllegalStateException If the type at the index is not a ValueType
     */
    fun resolveType(evalType: Int): ValueType {
        val type = types[evalType]
        return type as? ValueType
            ?: throw IllegalStateException(
                "Type at index $evalType is not a ValueType (found ${type::class.simpleName}). " +
                "Only ValueType entries can be resolved for property access."
            )
    }

    /**
     * Resolves a type from the types array, returning null if the index is out of bounds
     * or if the type is not a ValueType.
     *
     * @param evalType The index into the types array
     * @return The [ValueType] at the specified index, or null if the index is invalid
     *         or the type is not a ValueType
     */
    fun resolveTypeOrNull(evalType: Int): ValueType? {
        return types.getOrNull(evalType) as? ValueType
    }

    /**
     * Retrieves a variable scope by its index.
     *
     * @param scopeIndex The index of the scope to retrieve
     * @return The [VariableScope] at the specified index, or null if not found
     */
    fun getScope(scopeIndex: Int): VariableScope? {
        return variableScopes[scopeIndex]
    }

    /**
     * Resolves a variable from a specific scope.
     *
     * @param scopeIndex The index of the scope containing the variable
     * @param name The name of the variable to resolve
     * @return The resolved [VariableBinding], or null if not found
     */
    fun resolveVariable(scopeIndex: Int, name: String): VariableBinding? {
        return variableScopes[scopeIndex]?.getVariable(name)
    }

    /**
     * Checks if a variable is defined in the current match pattern.
     *
     * @param name The variable name to check
     * @return True if the variable is match-defined and should use dynamic binding
     */
    fun isMatchDefinedVariable(name: String): Boolean {
        return matchDefinedVariables.contains(name)
    }

    /**
     * Checks if the model transformation scope is available.
     *
     * @return True if transformation context is set for MT scope resolution
     */
    fun hasMTScope(): Boolean {
        return transformationContext != null
    }

    /**
     * Creates a new context with an additional variable scope.
     *
     * @param scopeIndex The index for the new scope
     * @param scope The variable scope to add
     * @return A new [TraversalCompilationContext] with the additional scope
     */
    fun withScope(scopeIndex: Int, scope: VariableScope): TraversalCompilationContext {
        return copy(variableScopes = variableScopes + (scopeIndex to scope))
    }

    /**
     * Creates a new context configured for match clause compilation.
     *
     * When in match context, traversals need special handling to work
     * correctly within Gremlin's match() step.
     *
     * @param matchLabel The label for the current match binding
     * @return A new [TraversalCompilationContext] configured for match
     */
    fun forMatch(matchLabel: String): TraversalCompilationContext {
        return copy(
            inMatchContext = true,
            currentMatchLabel = matchLabel
        )
    }

    /**
     * Creates a new context not in match mode.
     *
     * @return A new [TraversalCompilationContext] outside match context
     */
    fun outsideMatch(): TraversalCompilationContext {
        return copy(
            inMatchContext = false,
            currentMatchLabel = null
        )
    }

    /**
     * Creates a new context with the specified match-defined variables.
     *
     * @param variableNames The names of variables defined in the current match
     * @return A new [TraversalCompilationContext] with the match-defined variables set
     */
    fun withMatchDefinedVariables(variableNames: Set<String>): TraversalCompilationContext {
        return copy(matchDefinedVariables = variableNames)
    }

    /**
     * Generates a unique variable name and returns both the name and updated context.
     *
     * Used for creating intermediate bindings in where clauses.
     *
     * @param prefix The prefix for the generated name
     * @return A pair of the generated name and the updated context with incremented counter
     */
    fun generateVariableName(prefix: String = "_expr"): Pair<String, TraversalCompilationContext> {
        val name = "${prefix}_$variableCounter"
        return name to copy(variableCounter = variableCounter + 1)
    }

    /**
     * Creates a new context with the specified type registry.
     *
     * @param registry The type registry to use for property and method lookup
     * @return A new [TraversalCompilationContext] with the type registry set
     */
    fun withTypeRegistry(registry: GremlinTypeRegistry): TraversalCompilationContext {
        return copy(typeRegistry = registry)
    }

    /**
     * Creates a new context with the specified transformation context.
     *
     * @param context The transformation execution context for MT scope resolution
     * @return A new [TraversalCompilationContext] with the transformation context set
     */
    fun withTransformationContext(context: TransformationExecutionContext): TraversalCompilationContext {
        return copy(transformationContext = context)
    }

    companion object {
        /**
         * Creates a context from an existing [CompilationContext].
         *
         * This allows gradual migration from the old context to the new one.
         *
         * @param oldContext The existing compilation context
         * @return A new [TraversalCompilationContext] with equivalent settings
         */
        fun from(oldContext: CompilationContext): TraversalCompilationContext {
            return TraversalCompilationContext(
                types = oldContext.types,
                variableScopes = oldContext.variableScopes,
                traversalSource = oldContext.traversalSource,
                transformationContext = oldContext.transformationContext,
                matchDefinedVariables = oldContext.matchDefinedVariables,
                typeRegistry = oldContext.typeRegistry
            )
        }
    }
}

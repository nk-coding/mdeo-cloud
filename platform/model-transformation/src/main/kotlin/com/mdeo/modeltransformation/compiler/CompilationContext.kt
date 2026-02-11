package com.mdeo.modeltransformation.compiler

import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.ReturnType
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
 * ## Scope Chain
 * The [currentScope] represents the current variable scope with access to parent scopes
 * through the scope chain. Variable resolution walks up the chain to find bindings.
 *
 * @param types The list of types referenced by expressions via evalType index.
 *              Indices must be preserved from the TypedAst. Entries may be VoidType or
 *              ValueType subclasses. Only ValueType entries can be used for property lookups.
 * @param currentScope The current variable scope with access to parent scopes
 * @param traversalSource The GraphTraversalSource for building traversals
 * @param transformationContext Optional transformation context for MT scope resolution
 * @param typeRegistry The type registry for property and method lookup
 * @param inMatchContext Whether compilation is occurring within a match() clause
 * @param currentMatchLabel The current match label for `__.as()` prefix, if in match
 * @param variableCounter Counter for generating unique intermediate variable names
 */
data class CompilationContext(
    val types: List<ReturnType>,
    val currentScope: VariableScope = VariableScope.empty(),
    val traversalSource: GraphTraversalSource? = null,
    val transformationContext: TransformationExecutionContext? = null,
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
     * Retrieves a variable scope by its index from the scope chain.
     *
     * @param scopeIndex The index of the scope to retrieve
     * @return The [VariableScope] at the specified index, or null if not found
     */
    fun getScope(scopeIndex: Int): VariableScope? {
        return currentScope.getScopeAtLevel(scopeIndex)
    }

    /**
     * Creates a new context with an additional variable scope as a child of the current scope.
     *
     * @param scopeIndex The index for the new child scope
     * @param childBindings Initial bindings for the child scope
     * @return A new [CompilationContext] with the child scope as current
     */
    fun withChildScope(scopeIndex: Int, childBindings: Map<String, VariableBinding> = emptyMap()): CompilationContext {
        val childScope = currentScope.createChild(scopeIndex, childBindings)
        return copy(currentScope = childScope)
    }
}

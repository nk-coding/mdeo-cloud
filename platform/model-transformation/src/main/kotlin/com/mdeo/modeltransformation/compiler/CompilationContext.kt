package com.mdeo.modeltransformation.compiler

import com.mdeo.expression.ast.types.ValueType
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Context for expression compilation, providing access to type information,
 * variable scopes, and the graph traversal source.
 *
 * The compilation context carries all necessary information for compiling
 * typed expressions into Gremlin traversals. It maintains:
 * - Type resolution via the types array (expressions reference types by index)
 * - Variable scope information for resolving identifiers
 * - Access to the GraphTraversalSource for building traversals
 * - The current compilation mode (filter vs value)
 * - Optional model transformation context for MT scope (scope level 1)
 * - Type registry for looking up properties and methods on types
 *
 * ## Model Transformation Scope (Scope Level 1)
 * When [transformationContext] is provided, identifiers at scope level 1 are resolved
 * using the model transformation's execution context. This includes:
 * - Named instances: Resolved to their vertex IDs via [TransformationExecutionContext.lookupInstance]
 * - Variables: Resolved to their values via [TransformationExecutionContext.lookupVariable]
 * - Match-defined variables: If the variable is in [matchDefinedVariables], it uses dynamic
 *   binding from the pattern (via [VariableBinding.TraversalBinding])
 *
 * @param types The list of types referenced by expressions via evalType index.
 *              Each expression's evalType field is an index into this list.
 * @param variableScopes A map of scope index to variable bindings within that scope.
 *                       Each scope maps variable names to their resolved values or
 *                       traversal references.
 * @param traversalSource The GraphTraversalSource used for building Gremlin traversals.
 *                        Can be null when compiling expressions that don't require
 *                        graph access (e.g., pure value expressions).
 * @param compilationMode The mode in which expressions should be compiled.
 *                        Defaults to [CompilationMode.VALUE].
 * @param transformationContext Optional transformation execution context for resolving
 *                              MT scope identifiers (scope level 1). When set, identifiers
 *                              at scope 1 are resolved from this context.
 * @param matchDefinedVariables Set of variable names defined in the current match pattern.
 *                              Variables in this set use dynamic binding (TraversalBinding)
 *                              instead of static value lookup.
 * @param typeRegistry The type registry for looking up properties and methods.
 *                     Defaults to the global stdlib registry.
 */
data class CompilationContext(
    val types: List<ValueType>,
    val variableScopes: Map<Int, VariableScope> = emptyMap(),
    val traversalSource: GraphTraversalSource? = null,
    val compilationMode: CompilationMode = CompilationMode.VALUE,
    val transformationContext: TransformationExecutionContext? = null,
    val matchDefinedVariables: Set<String> = emptySet(),
    val typeRegistry: GremlinTypeRegistry = GLOBAL_TYPE_REGISTRY
) {
    companion object {
        /**
         * Global type registry with all standard library types pre-registered.
         * This is an alias for [GremlinTypeRegistry.GLOBAL].
         */
        val GLOBAL_TYPE_REGISTRY: GremlinTypeRegistry
            get() = GremlinTypeRegistry.GLOBAL
    }
    
    /**
     * Resolves a type from the types array using the given index.
     *
     * @param evalType The index into the types array.
     * @return The [ValueType] at the specified index.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    fun resolveType(evalType: Int): ValueType {
        return types[evalType]
    }
    
    /**
     * Resolves a type from the types array, returning null if the index is out of bounds.
     *
     * @param evalType The index into the types array.
     * @return The [ValueType] at the specified index, or null if the index is invalid.
     */
    fun resolveTypeOrNull(evalType: Int): ValueType? {
        return types.getOrNull(evalType)
    }
    
    /**
     * Retrieves a variable scope by its index.
     *
     * @param scopeIndex The index of the scope to retrieve.
     * @return The [VariableScope] at the specified index, or null if not found.
     */
    fun getScope(scopeIndex: Int): VariableScope? {
        return variableScopes[scopeIndex]
    }
    
    /**
     * Resolves a variable from a specific scope.
     *
     * @param scopeIndex The index of the scope containing the variable.
     * @param name The name of the variable to resolve.
     * @return The resolved [VariableBinding], or null if not found.
     */
    fun resolveVariable(scopeIndex: Int, name: String): VariableBinding? {
        return variableScopes[scopeIndex]?.getVariable(name)
    }
    
    /**
     * Creates a new context with an updated compilation mode.
     *
     * @param mode The new compilation mode.
     * @return A new [CompilationContext] with the specified mode.
     */
    fun withMode(mode: CompilationMode): CompilationContext {
        return copy(compilationMode = mode)
    }
    
    /**
     * Creates a new context with an additional variable scope.
     *
     * @param scopeIndex The index for the new scope.
     * @param scope The variable scope to add.
     * @return A new [CompilationContext] with the additional scope.
     */
    fun withScope(scopeIndex: Int, scope: VariableScope): CompilationContext {
        return copy(variableScopes = variableScopes + (scopeIndex to scope))
    }
    
    /**
     * Creates a new context with the specified transformation execution context.
     *
     * @param context The transformation execution context for MT scope resolution.
     * @return A new [CompilationContext] with the transformation context set.
     */
    fun withTransformationContext(context: TransformationExecutionContext): CompilationContext {
        return copy(transformationContext = context)
    }
    
    /**
     * Creates a new context with the specified match-defined variables.
     *
     * Variables in this set will use dynamic binding from the pattern
     * instead of static value lookup from the transformation context.
     *
     * @param variableNames The names of variables defined in the current match.
     * @return A new [CompilationContext] with the match-defined variables set.
     */
    fun withMatchDefinedVariables(variableNames: Set<String>): CompilationContext {
        return copy(matchDefinedVariables = variableNames)
    }
    
    /**
     * Checks if a variable is defined in the current match pattern.
     *
     * @param name The variable name to check.
     * @return True if the variable is match-defined and should use dynamic binding.
     */
    fun isMatchDefinedVariable(name: String): Boolean {
        return matchDefinedVariables.contains(name)
    }
    
    /**
     * Checks if the model transformation scope is available.
     *
     * @return True if transformation context is set for MT scope resolution.
     */
    fun hasMTScope(): Boolean {
        return transformationContext != null
    }
    
    /**
     * Creates a new context with the specified type registry.
     *
     * @param registry The type registry to use for property and method lookup.
     * @return A new [CompilationContext] with the type registry set.
     */
    fun withTypeRegistry(registry: GremlinTypeRegistry): CompilationContext {
        return copy(typeRegistry = registry)
    }
}

/**
 * The mode in which expressions are compiled.
 *
 * The compilation mode determines how expressions are transformed into Gremlin:
 * - [FILTER]: Generate traversal steps that filter/match existing graph elements
 * - [VALUE]: Generate values or steps that produce values
 */
enum class CompilationMode {
    /**
     * Filter mode: Generate Gremlin that filters/matches existing nodes.
     *
     * Used when expressions appear in pattern matching contexts where
     * conditions must be satisfied by existing graph elements.
     */
    FILTER,
    
    /**
     * Value mode: Generate Gremlin that produces a value.
     *
     * Used when expressions appear in contexts where a value is needed,
     * such as property assignments or creating new nodes.
     */
    VALUE
}

/**
 * Represents a scope containing variable bindings.
 *
 * Variable scopes are used to track variables defined at different
 * levels of the expression tree, such as loop variables or pattern
 * match bindings.
 *
 * @param bindings A map of variable names to their bindings.
 */
data class VariableScope(
    private val bindings: Map<String, VariableBinding> = emptyMap()
) {
    
    /**
     * Retrieves a variable binding by name.
     *
     * @param name The name of the variable.
     * @return The [VariableBinding] for the variable, or null if not found.
     */
    fun getVariable(name: String): VariableBinding? {
        return bindings[name]
    }
    
    /**
     * Creates a new scope with an additional variable binding.
     *
     * @param name The name of the variable to add.
     * @param binding The binding for the variable.
     * @return A new [VariableScope] with the additional binding.
     */
    fun withVariable(name: String, binding: VariableBinding): VariableScope {
        return copy(bindings = bindings + (name to binding))
    }
    
    companion object {
        /**
         * Creates an empty variable scope.
         *
         * @return An empty [VariableScope].
         */
        fun empty(): VariableScope = VariableScope()
        
        /**
         * Creates a variable scope from a map of variable names to bindings.
         *
         * @param bindings The initial variable bindings.
         * @return A new [VariableScope] with the specified bindings.
         */
        fun of(vararg bindings: Pair<String, VariableBinding>): VariableScope {
            return VariableScope(bindings.toMap())
        }
    }
}

/**
 * Represents a variable binding in a scope.
 *
 * A variable binding can hold either a concrete value or a reference
 * to a graph element in the traversal.
 */
sealed interface VariableBinding {
    
    /**
     * A binding to a concrete value.
     *
     * @param value The bound value.
     */
    data class ValueBinding(
        val value: Any?
    ) : VariableBinding
    
    /**
     * A binding to a graph element accessed via a step label.
     *
     * In Gremlin, step labels are used to reference previously visited
     * elements in a traversal using `select()`.
     *
     * @param stepLabel The step label used to reference this element.
     */
    data class TraversalBinding(
        val stepLabel: String
    ) : VariableBinding
    
    /**
     * A binding to a named instance in the model transformation.
     *
     * Named instances are graph vertices identified by their unique vertex ID.
     * In VALUE mode, this returns the vertex ID for property access or reference.
     * In FILTER mode, this applies a `hasId()` constraint.
     *
     * @param vertexId The unique identifier of the vertex in the graph.
     * @param instanceName The name of the instance in the transformation pattern.
     */
    data class InstanceBinding(
        val vertexId: Any,
        val instanceName: String
    ) : VariableBinding
}

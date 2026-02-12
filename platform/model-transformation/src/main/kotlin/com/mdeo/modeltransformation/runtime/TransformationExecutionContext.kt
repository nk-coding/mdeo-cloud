package com.mdeo.modeltransformation.runtime

import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope

/**
 * Holds the current state during transformation execution.
 *
 * The context delegates all scope management to a [VariableScope], which maintains:
 * - Variable bindings (as ValueBinding)
 * - Instance mappings (as InstanceBinding with vertexId set)
 * - Unresolved instances (as InstanceBinding with vertexId=null during match execution)
 * - Scope nesting with parent chain
 *
 * Scope levels:
 * - Level 0: Global scope
 * - Level 1: ModelTransformation level (starting scope for empty())
 * - Level 2+: Nested scopes (IfMatchConditionAndBlock, WhileMatchStatement, etc.)
 *
 * Note: Match blocks do NOT increment the scope level.
 *
 * @param variableScope The underlying VariableScope that manages all bindings.
 * @param parentContext Optional parent context for tracking scope depth.
 */
class TransformationExecutionContext private constructor(
    val variableScope: VariableScope,
    private val parentContext: TransformationExecutionContext? = null
) {
    
    /**
     * The current scope index.
     */
    val scopeIndex: Int
        get() = variableScope.scopeIndex
    
    /**
     * Creates a new child scope for nested execution.
     *
     * Child scopes inherit all bindings from the parent but can shadow them
     * with new bindings. When the child scope is exited, the parent scope's
     * bindings are restored.
     *
     * The scope index is incremented by 1 for each nesting level.
     *
     * @return A new [TransformationExecutionContext] with this context as its parent.
     */
    fun enterScope(): TransformationExecutionContext {
        val childScope = variableScope.createChild(scopeIndex + 1)
        return TransformationExecutionContext(
            variableScope = childScope,
            parentContext = this
        )
    }
    
    /**
     * Exits the current scope and returns to the parent scope.
     *
     * Any bindings created in the current scope are discarded.
     *
     * @return The parent [TransformationExecutionContext], or this context if no parent exists.
     */
    fun exitScope(): TransformationExecutionContext {
        return parentContext ?: this
    }

    /**
     * Gets all instance mappings from the current scope and all parent scopes.
     * 
     * @return A map of instance names to their vertex IDs.
     */
    fun getAllInstances(): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        collectInstances(variableScope, result)
        return result
    }
    
    /**
     * Recursively collects instance bindings from the given scope and all parent scopes.
     *
     * Traverses the scope chain from parent to child, accumulating instance bindings.
     * Only includes instances with resolved vertex IDs (non-null).
     *
     * @param scope The variable scope to collect instances from
     * @param result Mutable map to accumulate instance name to vertex ID mappings
     */
    private fun collectInstances(scope: VariableScope?, result: MutableMap<String, Any>) {
        if (scope == null) return
        collectInstances(scope.parent, result)
        scope.getAllLocalBindings().forEach { (name, binding) ->
            if (binding is VariableBinding.InstanceBinding) {
                val id = binding.vertexId
                if (id != null) {
                    result[name] = id
                }
            }
        }
    }

    /**
        * Creates a context with a specific variable scope.
        *
        * @param scope The variable scope to use.
        * @return A new [TransformationExecutionContext] with the specified scope.
        */
    fun withScope(scope: VariableScope): TransformationExecutionContext {
        return TransformationExecutionContext(
            variableScope = scope,
            parentContext = this.parentContext
        )
    }
    
    companion object {
        /**
         * Creates an empty root execution context at scope index 1 (ModelTransformation level).
         *
         * @return A new empty [TransformationExecutionContext].
         */
        fun empty(): TransformationExecutionContext {
            return TransformationExecutionContext(
                variableScope = VariableScope(scopeIndex = 1),
                parentContext = null
            )
        }
    }
}

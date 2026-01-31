package com.mdeo.modeltransformation.runtime

/**
 * Holds the current state during transformation execution.
 *
 * The context maintains variable bindings and named instance mappings throughout
 * the execution of a model transformation. It supports nested scopes for match
 * blocks, allowing variables to be shadowed within inner scopes.
 *
 * Variable bindings map variable names to their current values, while instance
 * mappings track the correspondence between named instances in the transformation
 * and their actual graph vertex IDs.
 *
 * @param variableBindings The current variable bindings in the transformation scope.
 * @param instanceMappings Maps instance names to their corresponding vertex IDs.
 * @param parentScope Optional parent scope for nested contexts (e.g., within match blocks).
 */
data class TransformationExecutionContext(
    private val variableBindings: Map<String, Any?> = emptyMap(),
    private val instanceMappings: Map<String, Any> = emptyMap(),
    private val parentScope: TransformationExecutionContext? = null
) {
    
    /**
     * Creates a new child scope for nested execution.
     *
     * Child scopes inherit all bindings from the parent but can shadow them
     * with new bindings. When the child scope is exited, the parent scope's
     * bindings are restored.
     *
     * @return A new [TransformationExecutionContext] with this context as its parent.
     */
    fun enterScope(): TransformationExecutionContext {
        return TransformationExecutionContext(
            variableBindings = emptyMap(),
            instanceMappings = emptyMap(),
            parentScope = this
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
        return parentScope ?: this
    }
    
    /**
     * Binds a variable to a value in the current scope.
     *
     * If a variable with the same name already exists in the current scope,
     * it will be overwritten. Variables in parent scopes are shadowed but not modified.
     *
     * @param name The name of the variable to bind.
     * @param value The value to bind to the variable.
     * @return A new [TransformationExecutionContext] with the variable bound.
     */
    fun bindVariable(name: String, value: Any?): TransformationExecutionContext {
        return copy(variableBindings = variableBindings + (name to value))
    }
    
    /**
     * Binds multiple variables at once in the current scope.
     *
     * @param bindings The variable bindings to add.
     * @return A new [TransformationExecutionContext] with the variables bound.
     */
    fun bindVariables(bindings: Map<String, Any?>): TransformationExecutionContext {
        return copy(variableBindings = variableBindings + bindings)
    }
    
    /**
     * Looks up a variable by name, searching from the current scope up to parent scopes.
     *
     * The lookup starts in the current scope and proceeds to parent scopes if the
     * variable is not found. Returns null if the variable is not found in any scope.
     *
     * @param name The name of the variable to look up.
     * @return The value bound to the variable, or null if not found.
     */
    fun lookupVariable(name: String): Any? {
        return variableBindings[name] ?: parentScope?.lookupVariable(name)
    }
    
    /**
     * Checks if a variable is defined in the current scope or any parent scope.
     *
     * @param name The name of the variable to check.
     * @return True if the variable is defined, false otherwise.
     */
    fun hasVariable(name: String): Boolean {
        return variableBindings.containsKey(name) || parentScope?.hasVariable(name) == true
    }
    
    /**
     * Binds a named instance to a vertex ID.
     *
     * Instance mappings track the correspondence between named instances in the
     * transformation pattern and their actual graph vertex IDs.
     *
     * @param instanceName The name of the instance as declared in the pattern.
     * @param vertexId The vertex ID in the graph.
     * @return A new [TransformationExecutionContext] with the instance bound.
     */
    fun bindInstance(instanceName: String, vertexId: Any): TransformationExecutionContext {
        return copy(instanceMappings = instanceMappings + (instanceName to vertexId))
    }
    
    /**
     * Binds multiple named instances at once.
     *
     * @param bindings The instance bindings to add.
     * @return A new [TransformationExecutionContext] with the instances bound.
     */
    fun bindInstances(bindings: Map<String, Any>): TransformationExecutionContext {
        return copy(instanceMappings = instanceMappings + bindings)
    }
    
    /**
     * Looks up a named instance's vertex ID, searching from current scope to parent scopes.
     *
     * @param instanceName The name of the instance to look up.
     * @return The vertex ID, or null if not found.
     */
    fun lookupInstance(instanceName: String): Any? {
        return instanceMappings[instanceName] ?: parentScope?.lookupInstance(instanceName)
    }
    
    /**
     * Checks if a named instance is defined in the current scope or any parent scope.
     *
     * @param instanceName The name of the instance to check.
     * @return True if the instance is defined, false otherwise.
     */
    fun hasInstance(instanceName: String): Boolean {
        return instanceMappings.containsKey(instanceName) || 
            parentScope?.hasInstance(instanceName) == true
    }
    
    /**
     * Returns all variable bindings visible from the current scope.
     *
     * This includes bindings from the current scope and all parent scopes,
     * with current scope bindings taking precedence.
     *
     * @return A map of all visible variable bindings.
     */
    fun getAllVariables(): Map<String, Any?> {
        val parentVariables = parentScope?.getAllVariables() ?: emptyMap()
        return parentVariables + variableBindings
    }
    
    /**
     * Returns all instance mappings visible from the current scope.
     *
     * This includes mappings from the current scope and all parent scopes,
     * with current scope mappings taking precedence.
     *
     * @return A map of all visible instance mappings.
     */
    fun getAllInstances(): Map<String, Any> {
        val parentInstances = parentScope?.getAllInstances() ?: emptyMap()
        return parentInstances + instanceMappings
    }
    
    /**
     * Returns the depth of the current scope.
     *
     * The root scope has depth 0, and each nested scope increments the depth by 1.
     *
     * @return The depth of the current scope.
     */
    fun getScopeDepth(): Int {
        return if (parentScope == null) 0 else 1 + parentScope.getScopeDepth()
    }
    
    companion object {
        /**
         * Creates an empty root execution context.
         *
         * @return A new empty [TransformationExecutionContext].
         */
        fun empty(): TransformationExecutionContext = TransformationExecutionContext()
    }
}

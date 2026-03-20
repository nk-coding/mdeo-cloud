package com.mdeo.modeltransformation.runtime

import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.graph.VertexRef

/**
 * Test-only helper extension functions for TransformationExecutionContext.
 * These provide convenient access to context state for testing purposes.
 */

/**
 * Binds a variable to a value in the current scope.
 * Test-only helper for creating contexts with variables.
 */
fun TransformationExecutionContext.testBindVariable(name: String, value: Any?): TransformationExecutionContext {
    val newScope = variableScope.withVariable(name, VariableBinding.ValueBinding(value))
    // Use reflection to reconstruct context with same parent
    val constructor = TransformationExecutionContext::class.java.getDeclaredConstructor(
        VariableScope::class.java,
        TransformationExecutionContext::class.java
    )
    constructor.isAccessible = true
    val parentField = TransformationExecutionContext::class.java.getDeclaredField("parentContext")
    parentField.isAccessible = true
    val parent = parentField.get(this) as TransformationExecutionContext?
    return constructor.newInstance(newScope, parent)
}

/**
 * Binds multiple variables at once in the current scope.
 * Test-only helper for creating contexts with multiple variables.
 */
fun TransformationExecutionContext.testBindVariables(bindings: Map<String, Any?>): TransformationExecutionContext {
    var context = this
    for ((name, value) in bindings) {
        context = context.testBindVariable(name, value)
    }
    return context
}

/**
 * Binds a named instance to a vertex ID.
 * Test-only helper for creating contexts with instance bindings.
 */
fun TransformationExecutionContext.testBindInstance(instanceName: String, vertexId: Any): TransformationExecutionContext {
    val newScope = variableScope.withVariable(
        instanceName, 
        VariableBinding.InstanceBinding(VertexRef(vertexId))
    )
    // Use reflection to reconstruct context with same parent
    val constructor = TransformationExecutionContext::class.java.getDeclaredConstructor(
        VariableScope::class.java,
        TransformationExecutionContext::class.java
    )
    constructor.isAccessible = true
    val parentField = TransformationExecutionContext::class.java.getDeclaredField("parentContext")
    parentField.isAccessible = true
    val parent = parentField.get(this) as TransformationExecutionContext?
    return constructor.newInstance(newScope, parent)
}

/**
 * Binds multiple named instances at once.
 * Test-only helper for creating contexts with multiple instance bindings.
 */
fun TransformationExecutionContext.testBindInstances(bindings: Map<String, Any>): TransformationExecutionContext {
    var context = this
    for ((name, vertexId) in bindings) {
        context = context.testBindInstance(name, vertexId)
    }
    return context
}

/**
 * Checks if a named instance is defined in the current scope or any parent scope.
 * Test-only helper for verifying instance bindings.
 */
fun TransformationExecutionContext.testHasInstance(instanceName: String): Boolean {
    return variableScope.getVariable(instanceName) is VariableBinding.InstanceBinding
}

/**
 * Returns all variable bindings visible from the current scope.
 * Test-only helper for verifying variable state.
 */
fun TransformationExecutionContext.testGetAllVariables(): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    collectVariables(variableScope, result)
    return result
}

private fun collectVariables(scope: VariableScope?, result: MutableMap<String, Any?>) {
    if (scope == null) return
    collectVariables(scope.parent, result)
    scope.getAllLocalBindings().forEach { (name, binding) ->
        if (binding is VariableBinding.ValueBinding) {
            result[name] = binding.value
        }
    }
}

/**
 * Returns all instance mappings visible from the current scope.
 * Test-only helper for verifying instance state.
 */
fun TransformationExecutionContext.testGetAllInstances(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    collectInstances(variableScope, result)
    return result
}

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

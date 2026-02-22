package com.mdeo.modeltransformation.runtime

/**
 * Registry for tracking instance names during model transformation execution.
 *
 * This registry maintains the mapping between vertex IDs and their instance names,
 * avoiding the need to store names as properties in the graph (which would conflict
 * with legitimate "name" properties in the metamodel).
 *
 * Features:
 * - Tracks vertex ID to name mappings
 * - Generates unique names for new instances when duplicates exist
 * - Supports name reservation for input model instances
 */
class InstanceNameRegistry {
    private val idToName = mutableMapOf<Any, String>()
    private val nameToId = mutableMapOf<String, Any>()
    private val nameCounters = mutableMapOf<String, Int>()
    
    /**
     * Registers a vertex with a specific name.
     * If the name already exists, throws an exception.
     *
     * @param vertexId The vertex ID
     * @param name The instance name
     * @throws IllegalArgumentException if the name is already registered
     */
    fun register(vertexId: Any, name: String) {
        require(!nameToId.containsKey(name)) {
            "Name '$name' is already registered to vertex ${nameToId[name]}"
        }
        idToName[vertexId] = name
        nameToId[name] = vertexId
    }
    
    /**
     * Registers a vertex with a name, generating a unique name if needed.
     * If the requested name already exists, appends a number suffix to make it unique.
     *
     * @param vertexId The vertex ID
     * @param requestedName The desired instance name
     * @return The actual name assigned (may have a suffix if the requested name was taken)
     */
    fun registerWithUniqueName(vertexId: Any, requestedName: String): String {
        if (!nameToId.containsKey(requestedName)) {
            register(vertexId, requestedName)
            return requestedName
        }
        
        val counter = nameCounters.getOrDefault(requestedName, 1)
        var uniqueName: String
        var attempt = counter
        
        do {
            uniqueName = "${requestedName}${attempt}"
            attempt++
        } while (nameToId.containsKey(uniqueName))
        
        nameCounters[requestedName] = attempt
        register(vertexId, uniqueName)
        return uniqueName
    }
    
    /**
     * Gets the name for a vertex ID.
     *
     * @param vertexId The vertex ID
     * @return The instance name, or null if not registered
     */
    fun getName(vertexId: Any): String? {
        return idToName[vertexId]
    }
    
    /**
     * Gets the vertex ID for a name.
     *
     * @param name The instance name
     * @return The vertex ID, or null if not registered
     */
    fun getVertexId(name: String): Any? {
        return nameToId[name]
    }
    
    /**
     * Checks if a name is registered.
     *
     * @param name The instance name
     * @return True if the name is registered
     */
    fun hasName(name: String): Boolean {
        return nameToId.containsKey(name)
    }
    
    /**
     * Checks if a vertex ID is registered.
     *
     * @param vertexId The vertex ID
     * @return True if the vertex ID is registered
     */
    fun hasVertexId(vertexId: Any): Boolean {
        return idToName.containsKey(vertexId)
    }
    
    /**
     * Gets all registered name mappings.
     *
     * @return Map of vertex ID to name
     */
    fun getAllMappings(): Map<Any, String> {
        return idToName.toMap()
    }
    
    /**
     * Clears all registrations.
     *
     * Removes all vertex ID to name mappings, name to vertex ID mappings,
     * and resets all name counters. After calling this method, the registry
     * will be in the same state as a newly created instance.
     */
    fun clear() {
        idToName.clear()
        nameToId.clear()
        nameCounters.clear()
    }
}

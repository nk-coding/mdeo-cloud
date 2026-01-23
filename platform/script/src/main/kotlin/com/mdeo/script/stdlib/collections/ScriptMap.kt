package com.mdeo.script.stdlib.collections

/**
 * A mutable map that provides key-value pair access with modification capabilities.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param K the type of keys in this map
 * @param V the type of values in this map
 */
interface ScriptMap<K, V> : ReadonlyMap<K, V> {

    /**
     * Removes all key-value pairs from this map.
     */
    fun clear()

    /**
     * Associates the specified value with the specified key.
     *
     * @param key the key to associate with
     * @param value the value to associate
     */
    fun put(key: K, value: V)

    /**
     * Copies all key-value pairs from the specified map to this map.
     *
     * @param map the map to copy from
     */
    fun putAll(map: ReadonlyMap<K, V>)

    /**
     * Removes the mapping for the specified key.
     *
     * @param key the key to remove
     * @return the value that was associated with the key, or null if not found
     */
    fun remove(key: K): V?
}

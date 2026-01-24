package com.mdeo.script.stdlib.impl.collections

/**
 * A readonly map that provides key-value pair access without modification capabilities.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param K the type of keys in this map
 * @param V the type of values in this map
 */
interface ReadonlyMap<K, out V> {

    /**
     * Returns the number of key-value pairs in this map.
     */
    fun size(): Int

    /**
     * Returns true if this map contains no key-value pairs.
     */
    fun isEmpty(): Boolean

    /**
     * Returns true if this map contains the specified key.
     *
     * @param key the key to check for
     */
    fun containsKey(key: K): Boolean

    /**
     * Returns true if this map contains the specified value.
     *
     * @param value the value to check for
     */
    fun containsValue(value: @UnsafeVariance V): Boolean

    /**
     * Returns the value associated with the specified key.
     *
     * @param key the key to look up
     * @return the value associated with the key, or null if not found
     */
    fun get(key: K): V?

    /**
     * Returns a readonly set view of the keys in this map.
     */
    fun keySet(): ReadonlySet<K>

    /**
     * Returns a readonly bag view of the values in this map.
     */
    fun values(): ReadonlyBag<@UnsafeVariance V>
}

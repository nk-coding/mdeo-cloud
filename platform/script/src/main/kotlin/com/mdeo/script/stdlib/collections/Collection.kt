package com.mdeo.script.stdlib.collections

/**
 * A mutable collection that provides both query and modification operations.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this collection
 */
interface Collection<T> : ReadonlyCollection<T> {

    /**
     * Adds an element to this collection.
     *
     * @param item the element to add
     * @return true if the collection was modified
     */
    fun add(item: T): Boolean

    /**
     * Adds all elements from the given collection to this collection.
     *
     * @param col the collection of elements to add
     * @return true if the collection was modified
     */
    fun addAll(col: ReadonlyCollection<T>): Boolean

    /**
     * Removes all elements from this collection.
     */
    fun clear()

    /**
     * Removes the first occurrence of the specified element from this collection.
     *
     * @param item the element to remove
     * @return true if the element was found and removed
     */
    fun remove(item: T): Boolean

    /**
     * Removes all elements that are also contained in the given collection.
     *
     * @param col the collection of elements to remove
     * @return true if the collection was modified
     */
    fun removeAll(col: ReadonlyCollection<T>): Boolean
}

package com.mdeo.script.stdlib.impl.collections

/**
 * A readonly ordered collection that maintains element order.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this collection
 */
interface ReadonlyOrderedCollection<out T> : ReadonlyCollection<T> {

    /**
     * Returns the element at the specified index.
     *
     * @param index the zero-based index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    fun at(index: Int): T

    /**
     * Returns the first element of this collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    fun first(): T

    /**
     * Returns the last element of this collection.
     *
     * @throws NoSuchElementException if the collection is empty
     */
    fun last(): T

    /**
     * Returns the index of the first occurrence of the specified element.
     *
     * @param item the element to search for
     * @return the index of the element, or -1 if not found
     */
    fun indexOf(item: @UnsafeVariance T): Int

    /**
     * Returns a new collection with elements in reverse order.
     */
    fun invert(): ReadonlyOrderedCollection<T>
}

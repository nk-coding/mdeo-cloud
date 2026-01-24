package com.mdeo.script.stdlib.impl.collections

import java.util.function.Function

/**
 * A mutable ordered collection that maintains element order and provides modification operations.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this collection
 */
interface OrderedCollection<T> : ReadonlyOrderedCollection<T>, Collection<T> {

    /**
     * Removes and returns the element at the specified index.
     *
     * @param index the zero-based index
     * @return the removed element
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    fun removeAt(index: Int): T

    /**
     * Sorts this collection in-place by the given key extractor.
     *
     * @param keyExtractor the function to extract the sort key
     * @return this collection after sorting
     */
    fun <U : Comparable<U>> sortBy(keyExtractor: Function<T, U>): OrderedCollection<T>
}

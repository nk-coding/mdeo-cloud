package com.mdeo.script.stdlib.collections

/**
 * A mutable bag (multiset) that allows duplicate elements with count tracking.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this bag
 */
interface Bag<T> : ReadonlyBag<T>, Collection<T>

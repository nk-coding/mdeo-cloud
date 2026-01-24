package com.mdeo.script.stdlib.impl.collections

/**
 * A readonly bag (multiset) that allows duplicate elements with count tracking.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this bag
 */
interface ReadonlyBag<out T> : ReadonlyCollection<T>

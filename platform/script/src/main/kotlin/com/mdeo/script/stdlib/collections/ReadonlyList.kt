package com.mdeo.script.stdlib.collections

/**
 * A readonly list that maintains element order and allows duplicates.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this list
 */
interface ReadonlyList<out T> : ReadonlyOrderedCollection<T>

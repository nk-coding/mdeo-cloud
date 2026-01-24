package com.mdeo.script.stdlib.impl.collections

/**
 * A mutable ordered set that maintains insertion order and contains no duplicates.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this ordered set
 */
interface OrderedSet<T> : ReadonlyOrderedSet<T>, OrderedCollection<T>

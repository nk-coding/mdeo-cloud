package com.mdeo.script.stdlib.impl.collections

/**
 * A readonly ordered set that maintains insertion order and contains no duplicates.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this ordered set
 */
interface ReadonlyOrderedSet<out T> : ReadonlyOrderedCollection<T>

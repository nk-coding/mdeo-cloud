package com.mdeo.script.stdlib.impl.collections

/**
 * A readonly set that contains no duplicate elements.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this set
 */
interface ReadonlySet<out T> : ReadonlyCollection<T>

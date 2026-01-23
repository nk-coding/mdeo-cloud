package com.mdeo.script.stdlib.collections

/**
 * A mutable set that contains no duplicate elements.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this set
 */
interface ScriptSet<T> : ReadonlySet<T>, Collection<T>

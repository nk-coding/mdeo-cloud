package com.mdeo.script.stdlib.collections

/**
 * A mutable list that maintains element order and allows duplicates.
 * This interface follows OCL/EOL collection semantics.
 *
 * @param T the type of elements contained in this list
 */
interface ScriptList<T> : ReadonlyList<T>, OrderedCollection<T>

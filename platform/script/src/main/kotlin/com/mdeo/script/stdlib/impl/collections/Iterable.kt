package com.mdeo.script.stdlib.impl.collections

/**
 * The base interface for all iterable collections in the script language.
 * This mirrors the Iterable concept from OCL/EOL.
 *
 * @param T the type of elements contained in this iterable
 */
interface ScriptIterable<out T> : Iterable<T>

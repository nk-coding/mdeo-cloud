package com.mdeo.script.stdlib.impl.collections

/**
 * Implementation of [ScriptList] backed by an ArrayList.
 * A mutable list that maintains element order and allows duplicates.
 *
 * @param T the type of elements in this list
 */
class ListImpl<T> : AbstractOrderedCollection<T, ArrayList<T>>, ScriptList<T> {

    /**
     * Creates an empty list.
     */
    constructor() : super(ArrayList())

    /**
     * Creates a list containing all elements from the given collection.
     *
     * @param elements the elements to add to the list
     */
    constructor(elements: kotlin.collections.Collection<T>) : super(ArrayList(elements))

    /**
     * Creates a list containing all elements from the given iterable.
     *
     * @param elements the elements to add to the list
     */
    constructor(elements: Iterable<T>) : super(ArrayList<T>().apply { 
        for (element in elements) {
            add(element)
        }
    })

    override fun toList(): ScriptList<T> = ListImpl(backing)

    override fun clone(): ScriptList<T> = ListImpl(backing)

    companion object {
        /**
         * Creates a list containing the specified elements.
         *
         * @param elements the elements to add to the list
         * @return a new list containing the elements
         */
        @JvmStatic
        fun <T> of(vararg elements: T): ScriptList<T> {
            val list = ListImpl<T>()
            for (element in elements) {
                list.add(element)
            }
            return list
        }

        /**
         * Creates an empty list.
         *
         * @return a new empty list
         */
        @JvmStatic
        fun <T> empty(): ScriptList<T> = ListImpl()
    }
}

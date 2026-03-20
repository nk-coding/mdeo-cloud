package com.mdeo.script.stdlib.impl.globals

import com.mdeo.script.runtime.ScriptContext
import com.mdeo.script.stdlib.impl.collections.Bag
import com.mdeo.script.stdlib.impl.collections.BagImpl
import com.mdeo.script.stdlib.impl.collections.ListImpl
import com.mdeo.script.stdlib.impl.collections.OrderedSet
import com.mdeo.script.stdlib.impl.collections.OrderedSetImpl
import com.mdeo.script.stdlib.impl.collections.ScriptList
import com.mdeo.script.stdlib.impl.collections.ScriptSet
import com.mdeo.script.stdlib.impl.collections.SetImpl

/**
 * Global functions available at scope level 0 in the script language.
 *
 * These are stdlib functions that can be called without a receiver,
 * such as println, listOf, setOf, etc.
 */
object GlobalFunctions {

    /**
     * Prints a string to the execution's output stream followed by a newline.
     *
     * Uses the [ScriptContext] to determine the correct output stream.
     * This allows the output to be captured when executing scripts through
     * the ExecutionEnvironment.
     *
     * @param context The script execution context providing the print stream.
     * @param value The string to print.
     */
    @JvmStatic
    fun println(context: ScriptContext, value: String) {
        context.printStream.println(value)
    }

    /**
     * Creates a mutable list containing the specified elements.
     *
     * @param elements The elements to add to the list.
     * @return A new mutable list containing the elements.
     */
    @JvmStatic
    fun listOf(vararg elements: Any?): ScriptList<Any?> {
        return ListImpl.of(*elements)
    }

    /**
     * Creates a mutable set containing the specified elements.
     *
     * @param elements The elements to add to the set.
     * @return A new mutable set containing the elements.
     */
    @JvmStatic
    fun setOf(vararg elements: Any?): ScriptSet<Any?> {
        return SetImpl.of(*elements)
    }

    /**
     * Creates a mutable bag containing the specified elements.
     *
     * @param elements The elements to add to the bag.
     * @return A new mutable bag containing the elements.
     */
    @JvmStatic
    fun bagOf(vararg elements: Any?): Bag<Any?> {
        return BagImpl.of(*elements)
    }

    /**
     * Creates a mutable ordered set containing the specified elements.
     *
     * @param elements The elements to add to the ordered set.
     * @return A new mutable ordered set containing the elements.
     */
    @JvmStatic
    fun orderedSetOf(vararg elements: Any?): OrderedSet<Any?> {
        return OrderedSetImpl.of(*elements)
    }

    /**
     * Creates an empty mutable list.
     *
     * @return A new empty mutable list.
     */
    @JvmStatic
    fun emptyList(): ScriptList<Any?> {
        return ListImpl.empty()
    }

    /**
     * Creates an empty mutable set.
     *
     * @return A new empty mutable set.
     */
    @JvmStatic
    fun emptySet(): ScriptSet<Any?> {
        return SetImpl.empty()
    }

    /**
     * Creates an empty mutable bag.
     *
     * @return A new empty mutable bag.
     */
    @JvmStatic
    fun emptyBag(): Bag<Any?> {
        return BagImpl.empty()
    }

    /**
     * Creates an empty mutable ordered set.
     *
     * @return A new empty mutable ordered set.
     */
    @JvmStatic
    fun emptyOrderedSet(): OrderedSet<Any?> {
        return OrderedSetImpl.empty()
    }
}

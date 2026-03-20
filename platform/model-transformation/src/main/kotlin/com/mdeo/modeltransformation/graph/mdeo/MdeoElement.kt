package com.mdeo.modeltransformation.graph.mdeo

import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper

/**
 * Base class for all elements in an [MdeoGraph].
 *
 * Stores only the integer identifier and the label string. Unlike TinkerElement,
 * there is no version tracking since transactions are not supported.
 *
 * @param id The unique integer identifier for this element.
 * @param label The label string (class name for vertices, edge label for edges).
 */
abstract class MdeoElement(
    @JvmField val id: Int,
    @JvmField val label: String
) : Element {
    @JvmField var removed: Boolean = false

    override fun id(): Any = id
    override fun label(): String = label
    override fun hashCode(): Int = ElementHelper.hashCode(this)

    override fun equals(other: Any?): Boolean = ElementHelper.areEqual(this, other)
}

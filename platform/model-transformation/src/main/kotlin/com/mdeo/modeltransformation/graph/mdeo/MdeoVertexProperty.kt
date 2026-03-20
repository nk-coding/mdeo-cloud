package com.mdeo.modeltransformation.graph.mdeo

import org.apache.tinkerpop.gremlin.structure.Property
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import java.util.Collections

/**
 * A vertex property backed by a [com.mdeo.metamodel.ModelInstance] field.
 *
 * Values are read lazily from the underlying model instance through
 * [MdeoVertex.getBackingInstance] and the metamodel property key. Writes
 * propagate directly to the model instance via [ModelInstance.setPropertyByKey].
 *
 * Meta-properties (properties on properties) are not supported.
 *
 * @param V The value type of this property.
 * @param id The unique integer identifier for this vertex property.
 * @param vertex The owning vertex.
 * @param key The property key (the metamodel property name, NOT the prop_X internal key).
 * @param value The current value.
 */
class MdeoVertexProperty<V>(
    private val id: Int,
    private val vertex: MdeoVertex,
    private val key: String,
    private val value: V
) : VertexProperty<V> {

    override fun id(): Any = id
    override fun label(): String = key
    override fun key(): String = key
    override fun value(): V = value
    override fun isPresent(): Boolean = true
    override fun element(): Vertex = vertex
    override fun graph() = vertex.graph()

    override fun <U : Any?> property(key: String?): Property<U> = Property.empty()
    override fun <U : Any?> property(key: String?, value: U): Property<U> =
        throw UnsupportedOperationException("Meta-properties are not supported")
    override fun keys(): Set<String> = Collections.emptySet()
    override fun <U : Any?> properties(vararg propertyKeys: String?): Iterator<Property<U>> =
        Collections.emptyIterator()

    override fun remove() {
        vertex.removeProperty(key, this)
    }

    override fun toString(): String = StringFactory.propertyString(this)
    override fun equals(other: Any?): Boolean = ElementHelper.areEqual(this, other)
    override fun hashCode(): Int = ElementHelper.hashCode(this as org.apache.tinkerpop.gremlin.structure.Element)
}

package com.mdeo.modeltransformation.graph.mdeo

import org.apache.tinkerpop.gremlin.process.traversal.step.HasContainerHolder
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStep
import org.apache.tinkerpop.gremlin.process.traversal.step.map.GraphStepContract
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer
import org.apache.tinkerpop.gremlin.process.traversal.util.AndP
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Element
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import java.util.Collections

/**
 * Optimized [GraphStep] for [MdeoGraph] that filters results efficiently using arrays.
 *
 * Unlike TinkerGraphStep, this implementation:
 * - Does not support indexes (no index-based lookup)
 * - Uses array-backed iterators instead of list + CloseableIterator
 * - Avoids exception-based flow control in iteration
 * - Only supports integer IDs
 *
 * @param S The start type.
 * @param E The element type (Vertex or Edge).
 */
class MdeoGraphStep<S, E : Element>(
    originalGraphStep: GraphStepContract<S, E>
) : GraphStep<S, E>(
    originalGraphStep.getTraversal<S, E>(),
    originalGraphStep.returnClass,
    originalGraphStep.isStartStep,
    *originalGraphStep.ids
), HasContainerHolder<S, E> {

    private val hasContainers = mutableListOf<HasContainer>()

    init {
        originalGraphStep.labels.forEach(this::addLabel)
        this.setIteratorSupplier {
            @Suppress("UNCHECKED_CAST")
            if (Vertex::class.java.isAssignableFrom(this.returnClass)) vertices() as Iterator<E>
            else edges() as Iterator<E>
        }
    }

    /**
     * Returns vertices from the graph, filtered by IDs and HasContainers.
     *
     * @return An iterator over matching vertices.
     */
    private fun vertices(): Iterator<Vertex> {
        val graph = this.traversal.graph.get() as MdeoGraph

        if (this.ids == null) return Collections.emptyIterator()

        return if (this.ids.isNotEmpty()) {
            filterToArray(graph.vertices(*this.ids))
        } else {
            filterToArray(graph.vertices())
        }
    }

    /**
     * Returns edges from the graph, filtered by IDs and HasContainers.
     *
     * @return An iterator over matching edges.
     */
    private fun edges(): Iterator<Edge> {
        val graph = this.traversal.graph.get() as MdeoGraph

        if (this.ids == null) return Collections.emptyIterator()

        return if (this.ids.isNotEmpty()) {
            filterToArray(graph.edges(*this.ids))
        } else {
            filterToArray(graph.edges())
        }
    }

    /**
     * Filters elements from the source iterator using [hasContainers] and returns
     * an array-backed iterator.
     *
     * The source iterator is fully consumed into a filtered array. Since the array
     * is never modified, its length serves as the natural stop condition, avoiding
     * exception-based flow control entirely.
     *
     * @param T The element type.
     * @param source The source iterator to filter.
     * @return An [ArrayIterator] over the filtered elements, or an empty iterator.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Element> filterToArray(source: Iterator<T>): Iterator<T> {
        val filtered = mutableListOf<T>()
        while (source.hasNext()) {
            val elem = source.next()
            if (HasContainer.testAll(elem, hasContainers)) {
                filtered.add(elem)
            }
        }

        if (filtered.isEmpty()) return Collections.emptyIterator()

        val array = Array<Any>(filtered.size) { filtered[it] as Any }
        return ArrayIterator<T>(array)
    }

    override fun getHasContainers(): List<HasContainer> = Collections.unmodifiableList(hasContainers)

    override fun addHasContainer(hasContainer: HasContainer) {
        if (hasContainer.predicate is AndP<*>) {
            for (predicate in (hasContainer.predicate as AndP<*>).predicates) {
                addHasContainer(HasContainer(hasContainer.key, predicate))
            }
        } else {
            hasContainers.add(hasContainer)
        }
    }

    override fun remove() {
        throw UnsupportedOperationException("remove")
    }

    override fun hashCode(): Int = super.hashCode() xor hasContainers.hashCode()

    override fun toString(): String {
        return if (hasContainers.isEmpty()) super.toString()
        else if (ids == null || ids.isEmpty()) {
            StringFactory.stepString(this, returnClass.simpleName.lowercase(), hasContainers)
        } else {
            StringFactory.stepString(this, returnClass.simpleName.lowercase(), ids.contentToString(), hasContainers)
        }
    }
}

/**
 * A simple array-backed iterator that avoids the overhead of list wrappers
 * and exception-based flow control.
 *
 * The array is never modified after construction, so its length serves as the
 * natural stop condition. This is more efficient than TinkerGraphIterator's
 * tryComputeNext approach which catches NoSuchElementException.
 *
 * @param T The element type.
 * @param array The backing array of elements (stored as Array&lt;Any&gt; to avoid JVM array covariance issues).
 */
class ArrayIterator<T>(
    private val array: Array<Any>
) : Iterator<T> {
    private var index = 0

    override fun hasNext(): Boolean = index < array.size

    @Suppress("UNCHECKED_CAST")
    override fun next(): T {
        if (index >= array.size) throw NoSuchElementException()
        return array[index++] as T
    }
}

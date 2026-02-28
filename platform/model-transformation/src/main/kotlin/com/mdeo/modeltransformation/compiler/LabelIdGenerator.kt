package com.mdeo.modeltransformation.compiler

/**
 * Generates unique labels for use in Gremlin traversal steps.
 *
 * Labels are used as step names (`.as(label)`) and select keys throughout traversal
 * construction. All labels produced during a single traversal build must be globally
 * unique to prevent step-label collisions.
 *
 * A single [LabelIdGenerator] instance should be shared across all components that
 * contribute labels to the same traversal — including expression compilers and the
 * match executor's own temporary bindings — so that the uniqueness invariant holds
 * regardless of component ordering.
 */
interface LabelIdGenerator {
    /**
     * Returns a new unique label string.
     *
     * Successive calls must never return the same value for a given instance.
     */
    fun getUniqueId(): String
}

/**
 * A sequential implementation of [LabelIdGenerator] that produces labels in the form
 * `"id_0"`, `"id_1"`, `"id_2"`, etc.
 *
 * Not thread-safe. Use a separate instance per traversal-build context when
 * concurrency is required.
 */
class SequentialLabelIdGenerator : LabelIdGenerator {
    private var counter = 0

    override fun getUniqueId(): String = "id_${counter++}"
}

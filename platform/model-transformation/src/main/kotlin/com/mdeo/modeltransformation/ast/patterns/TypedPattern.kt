package com.mdeo.modeltransformation.ast.patterns

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * A pattern containing pattern elements for matching and transformation.
 *
 * Patterns are the core construct in model transformation languages. They describe
 * a subgraph structure that should be matched in the model, along with optional
 * creation, deletion, or constraint specifications.
 *
 * @param elements The list of pattern elements that make up this pattern.
 *                 Elements can be variables, object instances, links, or where clauses.
 */
@Serializable
data class TypedPattern(
    val elements: List<@Contextual TypedPatternElement>
)

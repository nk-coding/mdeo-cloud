package com.mdeo.modeltransformation.ast.statements

/**
 * Base interface for all transformation statements in the model transformation language.
 *
 * Transformation statements are the building blocks of model transformation programs.
 * Each statement type represents a different kind of transformation operation, such as
 * pattern matching, conditional execution, or iteration.
 *
 * Unlike general-purpose programming language statements, transformation statements
 * are specifically designed for graph-based pattern matching and model manipulation.
 */
interface TypedTransformationStatement {
    /**
     * The kind of statement, used for polymorphic deserialization.
     *
     * This property identifies the concrete type of the statement and is used
     * by the serializer to determine which data class to instantiate.
     */
    val kind: String
}

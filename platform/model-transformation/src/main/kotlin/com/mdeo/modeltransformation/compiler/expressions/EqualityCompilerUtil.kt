package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.types.ValueType
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import org.apache.tinkerpop.gremlin.structure.VertexProperty
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Utility for compiling equality comparisons into Gremlin traversals.
 * 
 * This utility provides methods to build traversals for equality (==) and inequality (!=)
 * comparisons that work with all types. Unlike numeric comparisons that use math subtraction,
 * equality comparisons use the where() step with predicates.
 */
object EqualityCompilerUtil {
    
    /**
     * Builds a traversal for equality comparison (== and !=) that works with all types.
     *
     * The method compiles equality comparisons using the following pattern:
     * 1. Store the left value with unique ID label
     * 2. Compute the right value and store with unique ID label
     * 3. Use where() to compare them
     * 4. Use choose() to produce a boolean result
     *
     * Pattern for ==:
     * ```
     * leftTraversal.as(leftLabel)
     *     .map(rightTraversal).as(rightLabel)
     *     .choose(
     *       __.where(leftLabel, P.eq(rightLabel)),
     *       __.constant(true),
     *       __.constant(false)
     *     )
     * ```
     * 
     * Pattern for !=:
     * ```
     * leftTraversal.as(leftLabel)
     *     .map(rightTraversal).as(rightLabel)
     *     .choose(
     *       __.where(leftLabel, P.neq(rightLabel)),
     *       __.constant(true),
     *       __.constant(false)
     *     )
     * ```
     *
     * ## Enum Type Handling
     * When comparing an enum value (type starting with "enum.") with a non-enum value,
     * the comparison always returns false for == and true for !=. This prevents
     * accidental matches when comparing enum values with plain strings.
     * 
     * @param operator The equality operator ("==" or "!=")
     * @param leftTraversal The traversal that produces the left operand value
     * @param rightTraversal The traversal that produces the right operand value
     * @param leftType The type of the left operand
     * @param rightType The type of the right operand
     * @param registry The type registry for resolving collection types
     * @param leftLabel The unique label to use for the left operand
     * @param rightLabel The unique label to use for the right operand
     * @return A traversal that produces a boolean result (true or false)
     * @throws IllegalArgumentException if the operator is not "==" or "!="
     */
    @Suppress("UNCHECKED_CAST")
    fun buildEqualityTraversal(
        operator: String,
        leftTraversal: GraphTraversal<Any, Any>,
        rightTraversal: GraphTraversal<Any, Any>,
        leftType: ValueType,
        rightType: ValueType,
        registry: GremlinTypeRegistry,
        leftLabel: String,
        rightLabel: String
    ): GraphTraversal<Any, Boolean> {
        val leftIsEnum = isEnumType(leftType)
        val rightIsEnum = isEnumType(rightType)
        
        if (leftIsEnum != rightIsEnum) {
            val result = when (operator) {
                "==" -> false
                "!=" -> true
                else -> throw IllegalArgumentException("Unsupported equality operator: $operator. Expected '==' or '!='")
            }
            return AnonymousTraversal.constant<Any>(result) as GraphTraversal<Any, Boolean>
        }
        
        val predicate: P<String> = when (operator) {
            "==" -> P.eq(rightLabel)
            "!=" -> P.neq(rightLabel)
            else -> throw IllegalArgumentException("Unsupported equality operator: $operator. Expected '==' or '!='")
        }

        val leftIsCollection = isCollectionType(leftType, registry)
        val rightIsCollection = isCollectionType(rightType, registry)

        return (if (leftIsCollection) leftTraversal.fold() else leftTraversal)
            .`as`(leftLabel)
            .map(if (rightIsCollection) rightTraversal.fold() else rightTraversal)
            .`as`(rightLabel)
            .choose(
                AnonymousTraversal.where<Any>(leftLabel, predicate),
                AnonymousTraversal.constant(true),
                AnonymousTraversal.constant(false)
            ) as GraphTraversal<Any, Boolean>
    }

    /**
     * Checks if a type is an enum value type.
     *
     * Enum value types use the format "enum.EnumName" in the type registry.
     * This is distinct from "enum-container.EnumName" which is the container
     * type used to access enum entries.
     *
     * @param type The value type to check
     * @return true if the type is an enum value type (starts with "enum."), false otherwise
     */
    private fun isEnumType(type: ValueType?): Boolean {
        if (type == null) return false
        if (type !is ClassTypeRef) return false
        return type.type.startsWith("enum.")
    }

    /**
     * Checks if a type is a collection type (list, set, bag, etc.).
     *
     * Uses the type registry to check the type's cardinality property from the
     * type definition. Collection types have LIST or SET cardinality. Returns false
     * for null types or non-ClassTypeRef types.
     * 
     * @param type The value type to check (nullable, may not be a ClassTypeRef)
     * @param registry The Gremlin type registry for looking up type definitions
     * @return true if the type is a collection type (LIST or SET cardinality), false otherwise
     */
    private fun isCollectionType(
        type: ValueType?,
        registry: GremlinTypeRegistry
    ): Boolean {
        if (type == null) return false
        
        if (type !is ClassTypeRef) {
            return false
        }
                
        val typeDefinition = registry.getType(type.type)!!
        val registryCardinality = typeDefinition.cardinality
        return registryCardinality == VertexProperty.Cardinality.list || registryCardinality == VertexProperty.Cardinality.set
    }
}

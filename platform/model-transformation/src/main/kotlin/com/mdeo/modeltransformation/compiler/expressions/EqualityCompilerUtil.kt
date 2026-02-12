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
     * 1. Store the left value with as("_left")
     * 2. Compute the right value and store with as("_right")
     * 3. Use where() to compare them
     * 4. Use choose() to produce a boolean result
     *
     * Pattern for ==:
     * ```
     * leftTraversal.as("_left")
     *     .map(rightTraversal).as("_right")
     *     .choose(
     *       __.where("_left", P.eq("_right")),
     *       __.constant(true),
     *       __.constant(false)
     *     )
     * ```
     * 
     * Pattern for !=:
     * ```
     * leftTraversal.as("_left")
     *     .map(rightTraversal).as("_right")
     *     .choose(
     *       __.where("_left", P.neq("_right")),
     *       __.constant(true),
     *       __.constant(false)
     *     )
     * ```
     * 
     * @param operator The equality operator ("==" or "!=")
     * @param leftTraversal The traversal that produces the left operand value
     * @param rightTraversal The traversal that produces the right operand value
     * @param leftType The type of the left operand
     * @param rightType The type of the right operand
     * @param registry The type registry for resolving collection types
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
        registry: GremlinTypeRegistry
    ): GraphTraversal<Any, Boolean> {
        val predicate: P<String> = when (operator) {
            "==" -> P.eq("_right")
            "!=" -> P.neq("_right")
            else -> throw IllegalArgumentException("Unsupported equality operator: $operator. Expected '==' or '!='")
        }

        val leftIsCollection = isCollectionType(leftType, registry)
        val rightIsCollection = isCollectionType(rightType, registry)

        return (if (leftIsCollection) leftTraversal.fold() else leftTraversal)
            .`as`("_left")
            .map(if (rightIsCollection) rightTraversal.fold() else rightTraversal)
            .`as`("_right")
            .choose(
                AnonymousTraversal.where<Any>("_left", predicate),
                AnonymousTraversal.constant(true),
                AnonymousTraversal.constant(false)
            ) as GraphTraversal<Any, Boolean>
    }

    /**
     * Checks if a type is a collection type (list, set, bag, etc.).
     * 
     * Uses the type registry to check the type's cardinality property.
     * Collection types have LIST or SET cardinality.
     * 
     * @param type The type to check (nullable)
     * @param engine The transformation engine with type registry
     * @return true if the type is a collection type
     */
    private fun isCollectionType(
        type: ValueType?,
        registry: GremlinTypeRegistry
    ): Boolean {
        println("Resolving if type is collection: $type")
        if (type == null) return false
        
        if (type !is ClassTypeRef) {
            return false
        }
                
        val typeDefinition = registry.getType(type.type)!!
        val registryCardinality = typeDefinition.cardinality
        return registryCardinality == VertexProperty.Cardinality.list || registryCardinality == VertexProperty.Cardinality.set
    }
}

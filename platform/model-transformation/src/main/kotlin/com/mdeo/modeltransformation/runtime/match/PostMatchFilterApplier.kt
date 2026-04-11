package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.patterns.TypedPatternObjectInstanceElement
import com.mdeo.modeltransformation.ast.patterns.TypedPatternWhereClauseElement
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.expressions.EqualityCompilerUtil
import com.mdeo.modeltransformation.runtime.TransformationEngine
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal
import org.apache.tinkerpop.gremlin.structure.Vertex

/**
 * Adds post-match filter steps to the traversal:
 * - Injective constraints (distinct vertex binding for type-compatible instances).
 * - Property equality constraints for non-constant or collection-typed properties.
 * - User-defined where-clause expressions.
 */
internal class PostMatchFilterApplier(private val expressionSupport: ExpressionSupport) {

    /**
     * Adds `.where(a, neq(b))` constraints for every pair of type-compatible instances in
     * [instances] to ensure they bind to distinct vertices.
     */
    @Suppress("UNCHECKED_CAST")
    fun applyInjectiveConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instances: List<TypedPatternObjectInstanceElement>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (i in instances.indices) {
            for (j in i + 1 until instances.size) {
                val a = instances[i]
                val b = instances[j]
                if (a.objectInstance.name == b.objectInstance.name) continue
                if (!areTypesCompatible(a.objectInstance.className, b.objectInstance.className)) continue
                result = result.where(
                    VariableBinding.stepLabel(a.objectInstance.name),
                    P.neq(VariableBinding.stepLabel(b.objectInstance.name))
                ) as GraphTraversal<Vertex, Map<String, Any>>
            }
        }
        return result
    }

    /**
     * Adds post-match property `==` constraints for [instances].
     *
     * These are needed for:
     * - No-class instances with constant values (`.has()` cannot be used in match clauses).
     * - Any instance with a non-constant (expression) comparison value.
     */
    @Suppress("UNCHECKED_CAST")
    fun applyPropertyWhereConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        instances: List<TypedPatternObjectInstanceElement>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (instance in instances) {
            val label = VariableBinding.stepLabel(instance.objectInstance.name)
            val className = instance.objectInstance.className
            for (property in instance.objectInstance.properties) {
                if (property.operator != "==") continue
                val graphKey = expressionSupport.engine.resolvePropertyGraphKey(className, property.propertyName)
                val compiled = expressionSupport.compilePropertyExpression(property.value, emptyList())
                val propertyType = expressionSupport.resolveExpressionType(property.value)

                if (compiled is CompilationResult.ValueResult) {
                    result = if (expressionSupport.isCollectionType(propertyType)) {
                        val propTraversal = AnonymousTraversal.select<Any, Any>(label)
                            .values<Any>(graphKey) as GraphTraversal<Any, Any>
                        val exprTraversal = expressionSupport.buildConstantCollectionTraversal(compiled.value)
                        val eq = EqualityCompilerUtil.buildEqualityTraversal(
                            "==", propTraversal, exprTraversal,
                            propertyType ?: throw IllegalStateException(
                                "Cannot resolve type for: ${property.propertyName}"
                            ),
                            propertyType, expressionSupport.engine.typeRegistry,
                            expressionSupport.newCompilationContext().getUniqueId(),
                            expressionSupport.newCompilationContext().getUniqueId()
                        )
                        result.where(eq.`is`(true)) as GraphTraversal<Vertex, Map<String, Any>>
                    } else {
                        result.where(
                            AnonymousTraversal.select<Any, Any>(label).has(graphKey, compiled.value)
                        ) as GraphTraversal<Vertex, Map<String, Any>>
                    }
                } else if (compiled != null) {
                    val propTraversal = AnonymousTraversal.select<Any, Any>(label)
                        .values<Any>(graphKey) as GraphTraversal<Any, Any>
                    val exprTraversal = expressionSupport.compileToTraversal(
                        property.value, AnonymousTraversal.`as`<Any>(MATCH_ANCHOR_LABEL)
                    ) as GraphTraversal<Any, Any>
                    val resolvedType = propertyType
                        ?: throw IllegalStateException("Cannot resolve type for: ${property.propertyName}")
                    val eq = EqualityCompilerUtil.buildEqualityTraversal(
                        "==", propTraversal, exprTraversal,
                        resolvedType, resolvedType,
                        expressionSupport.engine.typeRegistry,
                        expressionSupport.newCompilationContext().getUniqueId(),
                        expressionSupport.newCompilationContext().getUniqueId()
                    )
                    result = result.where(eq.`is`(true)) as GraphTraversal<Vertex, Map<String, Any>>
                }
            }
        }
        return result
    }

    /**
     * Adds `.where(expression.is(true))` filters for each [TypedPatternWhereClauseElement].
     */
    @Suppress("UNCHECKED_CAST")
    fun applyWhereClauseConstraints(
        traversal: GraphTraversal<Vertex, Map<String, Any>>,
        whereClauses: List<TypedPatternWhereClauseElement>
    ): GraphTraversal<Vertex, Map<String, Any>> {
        var result = traversal
        for (clause in whereClauses) {
            val compiled = expressionSupport.compileToTraversal(clause.whereClause.expression)
            result = result.where(compiled.`is`(true)) as GraphTraversal<Vertex, Map<String, Any>>
        }
        return result
    }

    private fun areTypesCompatible(classNameA: String?, classNameB: String?): Boolean {
        if (classNameA == null || classNameB == null) return true
        val engine = expressionSupport.engine
        val refA = ClassTypeRef(`package` = engine.classPackage, type = classNameA, isNullable = false)
        val refB = ClassTypeRef(`package` = engine.classPackage, type = classNameB, isNullable = false)
        return engine.typeRegistry.isSubtypeOf(refA, refB) || engine.typeRegistry.isSubtypeOf(refB, refA)
    }
}

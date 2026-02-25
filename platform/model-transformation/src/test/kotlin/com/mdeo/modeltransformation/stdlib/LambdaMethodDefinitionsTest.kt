package com.mdeo.modeltransformation.stdlib

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry

import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry

import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for lambda-accepting collection methods.
 *
 * Tests cover implemented lambda-accepting methods:
 * - filter
 * - map
 * - exists / any
 * - all
 * - none
 * - one
 * - find
 * - reject
 */
class LambdaMethodDefinitionsTest {

    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        graph = TinkerGraph.open()
        g = graph.traversal()
        
        val types = listOf(
            ClassTypeRef(`package` = "builtin", type = "int", isNullable = false),
            ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false),
            ClassTypeRef(`package` = "builtin", type = "Collection", isNullable = false),
            ClassTypeRef(`package` = "builtin", type = "List", isNullable = false)
        )
        
        context = CompilationContext(
            types = types,
            currentScope = VariableScope.empty(),
            traversalSource = graph.traversal(),
            typeRegistry = TypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Lambda scope index for test lambdas.
     * Since the test context starts at scope 0, lambdas are at scope level 1.
     */
    private val LAMBDA_SCOPE_INDEX = 1

    /**
     * Creates a lambda expression: x => x > threshold
     */
    private fun greaterThanLambda(threshold: Int): TypedLambdaExpression {
        return TypedLambdaExpression(
            evalType = 1,
            parameters = listOf("x"),
            body = TypedBinaryExpression(
                evalType = 1,
                left = TypedIdentifierExpression(
                    evalType = 0,
                    name = "x",
                    scope = LAMBDA_SCOPE_INDEX
                ),
                operator = ">",
                right = TypedIntLiteralExpression(evalType = 0, value = threshold.toString())
            )
        )
    }

    /**
     * Creates an identity lambda: x => x
     */
    private fun identityLambda(): TypedLambdaExpression {
        return TypedLambdaExpression(
            evalType = 0,
            parameters = listOf("x"),
            body = TypedIdentifierExpression(
                evalType = 0,
                name = "x",
                scope = LAMBDA_SCOPE_INDEX
            )
        )
    }

    /**
     * Creates a list literal with int values.
     */
    private fun listLiteral(vararg values: Int): TypedListLiteralExpression {
        return TypedListLiteralExpression(
            evalType = 3,
            elements = values.map { TypedIntLiteralExpression(evalType = 0, value = it.toString()) }
        )
    }

    /**
     * Creates a member call on a list with a lambda argument.
     */
    private fun memberCallWithLambda(
        methodName: String,
        list: TypedListLiteralExpression,
        lambda: TypedLambdaExpression,
        overload: String = "it"
    ): TypedMemberCallExpression {
        return TypedMemberCallExpression(
            evalType = 3,
            expression = list,
            member = methodName,
            arguments = listOf(lambda),
            overload = overload,
            isNullChaining = false
        )
    }

    /**
     * Creates a member call with lambda and additional argument.
     */
    private fun memberCallWithLambdaAndArg(
        methodName: String,
        list: TypedListLiteralExpression,
        lambda: TypedLambdaExpression,
        arg: Int
    ): TypedMemberCallExpression {
        return TypedMemberCallExpression(
            evalType = 1,
            expression = list,
            member = methodName,
            arguments = listOf(lambda, TypedIntLiteralExpression(evalType = 0, value = arg.toString())),
            overload = "",
            isNullChaining = false
        )
    }

    @Nested
    inner class FilterMethodTests {

        @Test
        fun `filter keeps elements matching predicate`() {
            val list = listLiteral(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val filtered = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(6, 7, 8, 9, 10), filtered)
        }

        @Test
        fun `filter returns empty list when no elements match`() {
            val list = listLiteral(1, 2, 3, 4, 5)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val filtered = g.inject(null as Any?).flatMap(result.traversal).toList()
            assertTrue(filtered.isEmpty())
        }

        @Test
        fun `filter returns all elements when all match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val filtered = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(10, 20, 30), filtered)
        }
    }

    @Nested
    inner class MapMethodTests {

        @Test
        fun `map transforms each element`() {
            val list = listLiteral(1, 2, 3)
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("map", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val mapped = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(1, 2, 3), mapped)
        }

        @Test
        fun `map with empty list returns empty list`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("map", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val mapped = g.inject(null as Any?).flatMap(result.traversal).toList()
            assertTrue(mapped.isEmpty())
        }

        @Test
        fun `map can access lambda parameter`() {
            val list = listLiteral(1, 2, 3)
            // Create a lambda: x => x (identity, same as identityLambda)
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("map", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val mapped = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(1, 2, 3), mapped)
        }
    }

    @Nested
    inner class ExistsMethodTests {

        @Test
        fun `exists returns true when any element matches`() {
            val list = listLiteral(1, 2, 10, 4, 5)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("exists", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }

        @Test
        fun `exists returns false when no element matches`() {
            val list = listLiteral(1, 2, 3, 4, 5)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("exists", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(false, actualValue)
        }
    }

    @Nested
    inner class AllMethodTests {

        @Test
        fun `all returns true when all elements match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }

        @Test
        fun `all returns false when some element does not match`() {
            val list = listLiteral(10, 3, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(false, actualValue)
        }

        @Test
        fun `all returns true for empty collection`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }
    }

    @Nested
    inner class NoneMethodTests {

        @Test
        fun `none returns true when no element matches`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("none", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }

        @Test
        fun `none returns false when any element matches`() {
            val list = listLiteral(1, 10, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("none", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(false, actualValue)
        }

        @Test
        fun `none returns true for empty collection`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("none", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }
    }

    @Nested
    inner class OneMethodTests {

        @Test
        fun `one returns true when exactly one element matches`() {
            val list = listLiteral(1, 10, 3, 4)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("one", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }

        @Test
        fun `one returns false when zero elements match`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("one", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(false, actualValue)
        }

        @Test
        fun `one returns false when multiple elements match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("one", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(false, actualValue)
        }
    }

    @Nested
    inner class FindMethodTests {

        @Test
        fun `find returns first matching element`() {
            val list = listLiteral(1, 2, 10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("find", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(10, actualValue)
        }

        @Test
        fun `find returns null when no element matches`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("find", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).tryNext().orElse(null)
            assertNull(actualValue)
        }

        @Test
        fun `find returns first element when multiple match`() {
            val list = listLiteral(10, 20, 30, 40)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("find", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(10, actualValue)  // Should return first matching element
        }
    }

    @Nested
    inner class RejectMethodTests {

        @Test
        fun `reject keeps elements NOT matching predicate`() {
            val list = listLiteral(1, 2, 10, 20, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("reject", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val rejected = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(1, 2, 3), rejected)
        }

        @Test
        fun `reject returns all elements when none match`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("reject", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val rejected = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(1, 2, 3), rejected)
        }

        @Test
        fun `reject returns empty when all elements match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("reject", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            @Suppress("UNCHECKED_CAST")
            val rejected = g.inject(null as Any?).flatMap(result.traversal).toList()
            assertTrue(rejected.isEmpty())
        }
    }


    @Nested
    inner class OuterScopeVariableTests {
        
        /**
         * Creates a lambda expression: x => x > threshold, where threshold is from outer scope.
         */
        private fun greaterThanOuterScopeThresholdLambda(): TypedLambdaExpression {
            return TypedLambdaExpression(
                evalType = 1,
                parameters = listOf("x"),
                body = TypedBinaryExpression(
                    evalType = 1,
                    left = TypedIdentifierExpression(
                        evalType = 0,
                        name = "x",
                        scope = LAMBDA_SCOPE_INDEX
                    ),
                    operator = ">",
                    right = TypedIdentifierExpression(
                        evalType = 0,
                        name = "threshold",
                        scope = 0  // Outer scope
                    )
                )
            )
        }
        
        @Test
        fun `filter with lambda accessing outer scope variable`() {
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(5), scopeIndex = 0)
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = outerScope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            val list = listLiteral(1, 2, 3, 10, 20)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            @Suppress("UNCHECKED_CAST")
            val filtered = g.inject(null as Any?).flatMap(result.traversal).toList() as List<Int>
            assertEquals(listOf(10, 20), filtered)
        }
        
        @Test
        fun `exists with lambda accessing outer scope variable`() {
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(15))
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = outerScope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            val list = listLiteral(1, 10, 20)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("exists", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }
        
        @Test
        fun `all with lambda accessing outer scope variable`() {
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(5))
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = outerScope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(true, actualValue)
        }
    }
}

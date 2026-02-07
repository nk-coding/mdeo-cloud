package com.mdeo.modeltransformation.stdlib

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import com.mdeo.modeltransformation.compiler.TraversalCompilationContext
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry

import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry

import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
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
 * Tests cover all lambda-accepting methods defined in [LambdaMethodDefinitions]:
 * - filter
 * - map
 * - exists / any
 * - all
 * - none
 * - one
 * - find
 * - reject
 * - rejectOne
 * - count (with lambda)
 * - sortedBy
 * - aggregate
 * - associate
 * - atLeastNMatch
 * - atMostNMatch
 * - nMatch
 */
@Disabled("Lambda support is not yet implemented in traversal mode")
class LambdaMethodDefinitionsTest {

    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var graph: TinkerGraph
    private lateinit var context: TraversalCompilationContext

    @BeforeEach
    fun setUp() {
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        graph = TinkerGraph.open()
        
        val types = listOf(
            ClassTypeRef(type = "builtin.int", isNullable = false),
            ClassTypeRef(type = "builtin.boolean", isNullable = false),
            ClassTypeRef(type = "ReadonlyCollection", isNullable = false),
            ClassTypeRef(type = "List", isNullable = false)
        )
        
        context = TraversalCompilationContext(
            types = types,
            traversalSource = graph.traversal(),
            typeRegistry = GremlinTypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Lambda scope index constant (placeholder for disabled test).
     */
    private val LAMBDA_SCOPE_INDEX = 100

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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val filtered = result.constantValue as List<Int>
            assertEquals(listOf(6, 7, 8, 9, 10), filtered)
        }

        @Test
        fun `filter returns empty list when no elements match`() {
            val list = listLiteral(1, 2, 3, 4, 5)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val filtered = result.constantValue as List<*>
            assertTrue(filtered.isEmpty())
        }

        @Test
        fun `filter returns all elements when all match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val filtered = result.constantValue as List<Int>
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val mapped = result.constantValue as List<Int>
            assertEquals(listOf(1, 2, 3), mapped)
        }

        @Test
        fun `map with empty list returns empty list`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("map", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val mapped = result.constantValue as List<*>
            assertTrue(mapped.isEmpty())
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `exists returns false when no element matches`() {
            val list = listLiteral(1, 2, 3, 4, 5)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("exists", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }

        @Test
        fun `any is alias for exists`() {
            val list = listLiteral(1, 10, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("any", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `all returns false when some element does not match`() {
            val list = listLiteral(10, 3, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }

        @Test
        fun `all returns true for empty collection`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `none returns false when any element matches`() {
            val list = listLiteral(1, 10, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("none", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }

        @Test
        fun `none returns true for empty collection`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("none", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `one returns false when zero elements match`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("one", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }

        @Test
        fun `one returns false when multiple elements match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("one", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(10, result.constantValue)
        }

        @Test
        fun `find returns null when no element matches`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("find", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertNull(result.constantValue)
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
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val rejected = result.constantValue as List<Int>
            assertEquals(listOf(1, 2, 3), rejected)
        }

        @Test
        fun `reject returns all elements when none match`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("reject", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val rejected = result.constantValue as List<Int>
            assertEquals(listOf(1, 2, 3), rejected)
        }
    }

    @Nested
    inner class RejectOneMethodTests {

        @Test
        fun `rejectOne removes first matching element`() {
            val list = listLiteral(1, 10, 20, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("rejectOne", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val rejected = result.constantValue as List<Int>
            assertEquals(listOf(1, 20, 3), rejected)
        }

        @Test
        fun `rejectOne returns same list when no element matches`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("rejectOne", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val rejected = result.constantValue as List<Int>
            assertEquals(listOf(1, 2, 3), rejected)
        }
    }

    @Nested
    inner class CountWithLambdaMethodTests {

        @Test
        fun `count returns number of matching elements`() {
            val list = listLiteral(1, 10, 20, 3, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambda("count", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(3, result.constantValue)
        }

        @Test
        fun `count returns zero when no elements match`() {
            val list = listLiteral(1, 2, 3)
            val lambda = greaterThanLambda(10)
            val memberCall = memberCallWithLambda("count", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(0, result.constantValue)
        }
    }

    @Nested
    inner class SortedByMethodTests {

        @Test
        fun `sortedBy sorts by key function`() {
            val list = listLiteral(3, 1, 4, 1, 5, 9, 2, 6)
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("sortedBy", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val sorted = result.constantValue as List<Int>
            assertEquals(listOf(1, 1, 2, 3, 4, 5, 6, 9), sorted)
        }

        @Test
        fun `sortedBy with empty list returns empty list`() {
            val list = TypedListLiteralExpression(evalType = 3, elements = emptyList())
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("sortedBy", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val sorted = result.constantValue as List<*>
            assertTrue(sorted.isEmpty())
        }
    }

    @Nested
    inner class AggregateMethodTests {

        @Test
        fun `aggregate groups elements by key`() {
            val list = listLiteral(1, 2, 3, 1, 2, 1)
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("aggregate", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val grouped = result.constantValue as Map<Int, List<Int>>
            assertEquals(3, grouped[1]?.size)
            assertEquals(2, grouped[2]?.size)
            assertEquals(1, grouped[3]?.size)
        }
    }

    @Nested
    inner class AssociateMethodTests {

        @Test
        fun `associate creates map with key from lambda`() {
            val list = listLiteral(1, 2, 3)
            val lambda = identityLambda()
            val memberCall = memberCallWithLambda("associate", list, lambda)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val associated = result.constantValue as Map<Int, Int>
            assertEquals(1, associated[1])
            assertEquals(2, associated[2])
            assertEquals(3, associated[3])
        }
    }

    @Nested
    inner class AtLeastNMatchMethodTests {

        @Test
        fun `atLeastNMatch returns true when at least n match`() {
            val list = listLiteral(1, 10, 20, 3, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("atLeastNMatch", list, lambda, 3)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `atLeastNMatch returns false when fewer than n match`() {
            val list = listLiteral(1, 10, 20, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("atLeastNMatch", list, lambda, 3)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }
    }

    @Nested
    inner class AtMostNMatchMethodTests {

        @Test
        fun `atMostNMatch returns true when at most n match`() {
            val list = listLiteral(1, 10, 20, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("atMostNMatch", list, lambda, 2)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `atMostNMatch returns false when more than n match`() {
            val list = listLiteral(1, 10, 20, 30, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("atMostNMatch", list, lambda, 2)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }
    }

    @Nested
    inner class NMatchMethodTests {

        @Test
        fun `nMatch returns true when exactly n match`() {
            val list = listLiteral(1, 10, 20, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("nMatch", list, lambda, 2)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }

        @Test
        fun `nMatch returns false when fewer than n match`() {
            val list = listLiteral(1, 10, 3)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("nMatch", list, lambda, 2)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
        }

        @Test
        fun `nMatch returns false when more than n match`() {
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanLambda(5)
            val memberCall = memberCallWithLambdaAndArg("nMatch", list, lambda, 2)
            
            val result = registry.compile(memberCall, context)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(false, result.constantValue)
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
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(5))
            val contextWithScope = context.withScope(0, outerScope)
            
            val list = listLiteral(1, 2, 3, 10, 20)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("filter", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            @Suppress("UNCHECKED_CAST")
            val filtered = result.constantValue as List<Int>
            assertEquals(listOf(10, 20), filtered)
        }
        
        @Test
        fun `exists with lambda accessing outer scope variable`() {
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(15))
            val contextWithScope = context.withScope(0, outerScope)
            
            val list = listLiteral(1, 10, 20)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("exists", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }
        
        @Test
        fun `all with lambda accessing outer scope variable`() {
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(5))
            val contextWithScope = context.withScope(0, outerScope)
            
            val list = listLiteral(10, 20, 30)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("all", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(true, result.constantValue)
        }
        
        @Test
        fun `count with lambda accessing outer scope variable`() {
            val outerScope = VariableScope.of("threshold" to VariableBinding.ValueBinding(5))
            val contextWithScope = context.withScope(0, outerScope)
            
            val list = listLiteral(1, 2, 10, 20, 3)
            val lambda = greaterThanOuterScopeThresholdLambda()
            val memberCall = memberCallWithLambda("count", list, lambda)
            
            val result = registry.compile(memberCall, contextWithScope)
            
            // assertIs<GremlinCompilationResult.ValueResult>(result)
            assertEquals(2, result.constantValue)
        }
    }
}

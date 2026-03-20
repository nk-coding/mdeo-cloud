package com.mdeo.modeltransformation.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedBooleanLiteralExpression
import com.mdeo.expression.ast.expressions.TypedDoubleLiteralExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedFloatLiteralExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedIntLiteralExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.expression.ast.expressions.TypedLongLiteralExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedNullLiteralExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import com.mdeo.expression.ast.expressions.TypedUnaryExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
import com.mdeo.modeltransformation.compiler.CompilationResult
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as AnonymousTraversal

/**
 * Comprehensive tests for traversal-based expression compilers.
 *
 * Tests the compilation of expressions into pure Gremlin traversals that can be
 * executed directly against a TinkerGraph. Each test category validates:
 * - Correct traversal generation
 * - Proper result values when executed
 * - Initial traversal propagation
 * - Constant folding optimization where applicable
 *
 * ## Test Categories
 * - Literal compilers: int, long, double, float, string, boolean, null, list
 * - Binary operators: arithmetic, comparison, logical
 * - Unary operators: negation, logical not
 * - Identifier resolution with variable scopes
 * - Member access (property navigation)
 * - Ternary expressions
 * - Integration with match/where patterns
 */
@DisplayName("Traversal Compiler Tests")
class TraversalCompilerTest {

    private lateinit var graph: TinkerGraph
    private lateinit var g: GraphTraversalSource
    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var context: CompilationContext

    // Type indices for expressions
    companion object {
        const val INT_TYPE = 0
        const val LONG_TYPE = 1
        const val DOUBLE_TYPE = 2
        const val FLOAT_TYPE = 3
        const val STRING_TYPE = 4
        const val BOOLEAN_TYPE = 5
        const val ANY_TYPE = 6
        const val LIST_TYPE = 7
    }

    // Type list for compilation context
    private val types = listOf(
        ClassTypeRef(`package` = "builtin", type = "int", isNullable = false),      // 0
        ClassTypeRef(`package` = "builtin", type = "long", isNullable = false),     // 1
        ClassTypeRef(`package` = "builtin", type = "double", isNullable = false),   // 2
        ClassTypeRef(`package` = "builtin", type = "float", isNullable = false),    // 3
        ClassTypeRef(`package` = "builtin", type = "string", isNullable = false),   // 4
        ClassTypeRef(`package` = "builtin", type = "boolean", isNullable = false),  // 5
        ClassTypeRef(`package` = "builtin", type = "Any", isNullable = true),       // 6
        ClassTypeRef(`package` = "builtin", type = "List", isNullable = false)              // 7
    )

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        g = graph.traversal()
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        context = CompilationContext(
            types = types,
            currentScope = VariableScope.empty(),
            traversalSource = g,
            typeRegistry = TypeRegistry.GLOBAL
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    // =====================================================================
    // Helper Functions for Expression Construction
    // =====================================================================

    private fun intLiteral(value: Int) = TypedIntLiteralExpression(evalType = INT_TYPE, value = value.toString())
    private fun longLiteral(value: Long) = TypedLongLiteralExpression(evalType = LONG_TYPE, value = value.toString())
    private fun doubleLiteral(value: Double) = TypedDoubleLiteralExpression(evalType = DOUBLE_TYPE, value = value.toString())
    private fun floatLiteral(value: Float) = TypedFloatLiteralExpression(evalType = FLOAT_TYPE, value = value.toString())
    private fun stringLiteral(value: String) = TypedStringLiteralExpression(evalType = STRING_TYPE, value = value)
    private fun boolLiteral(value: Boolean) = TypedBooleanLiteralExpression(evalType = BOOLEAN_TYPE, value = value)
    private fun nullLiteral() = TypedNullLiteralExpression(evalType = ANY_TYPE)
    private fun listLiteral(vararg elements: TypedExpression) = TypedListLiteralExpression(evalType = LIST_TYPE, elements = elements.toList())

    // Binary helper that infers result type based on operands and operator
    private fun binary(operator: String, left: TypedExpression, right: TypedExpression): TypedBinaryExpression {
        val resultType = when (operator) {
            "+", "-", "*", "/", "%" -> {
                // String concatenation if either operand is string
                if (left.evalType == STRING_TYPE || right.evalType == STRING_TYPE) STRING_TYPE
                // Numeric operations return double (math step behavior)
                else DOUBLE_TYPE
            }
            "<", ">", "<=", ">=", "==", "!=", "&&", "||" -> BOOLEAN_TYPE
            else -> ANY_TYPE
        }
        return TypedBinaryExpression(evalType = resultType, operator = operator, left = left, right = right)
    }

    private fun unary(operator: String, expression: TypedExpression): TypedUnaryExpression {
        val resultType = when (operator) {
            "!" -> BOOLEAN_TYPE
            "-" -> expression.evalType
            else -> expression.evalType
        }
        return TypedUnaryExpression(evalType = resultType, operator = operator, expression = expression)
    }

    private fun identifier(name: String, scope: Int = 0, evalType: Int = ANY_TYPE) =
        TypedIdentifierExpression(evalType = evalType, name = name, scope = scope)

    private fun memberAccess(expression: TypedExpression, member: String, isNullChaining: Boolean = false, evalType: Int = ANY_TYPE) =
        TypedMemberAccessExpression(evalType = evalType, expression = expression, member = member, isNullChaining = isNullChaining)

    private fun ternary(condition: TypedExpression, trueExpr: TypedExpression, falseExpr: TypedExpression) =
        TypedTernaryExpression(evalType = trueExpr.evalType, condition = condition, trueExpression = trueExpr, falseExpression = falseExpr)

    /**
     * Helper to execute a traversal by providing an input element.
     * Anonymous traversals like __.constant(x) need an input to produce output.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> executeConstantTraversal(result: CompilationResult): T? {
        // Execute the traversal with a null input
        val traversal = g.inject(null as Any?).flatMap(result.traversal as GraphTraversal<Any, T>)
        return if (traversal.hasNext()) traversal.next() else null
    }

    /**
     * Helper to execute a traversal starting from a vertex.
     * This is needed for anonymous traversals like __.constant(x).
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> executeWithVertex(result: CompilationResult): T? {
        // Add a dummy vertex as starting point for the traversal
        val vertex = graph.addVertex(T.label, "dummy")
        val traversal = g.V(vertex).flatMap(result.traversal as GraphTraversal<Any, T>)
        return if (traversal.hasNext()) traversal.next() else null
    }

    /**
     * Helper to execute a traversal and get all results.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> executeTraversalList(result: CompilationResult): List<T> {
        val traversal = result.traversal as GraphTraversal<Any, T>
        return traversal.toList()
    }

    // =====================================================================
    // LITERAL COMPILER TESTS
    // =====================================================================

    @Nested
    @DisplayName("Literal Compilers")
    inner class LiteralCompilerTests {

        @Nested
        @DisplayName("Integer Literals")
        inner class IntLiteralTests {

            @Test
            fun `compiles positive integer literal`() {
                val expr = intLiteral(42)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(42, actualValue)
                // Verify traversal can be executed with an input
                assertEquals(42, executeWithVertex<Int>(result))
            }

            @Test
            fun `compiles negative integer literal`() {
                val expr = intLiteral(-123)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(-123, actualValue)
            }

            @Test
            fun `compiles zero integer literal`() {
                val expr = intLiteral(0)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(0, actualValue)
            }

            @Test
            fun `compiles with initial traversal`() {
                val vertex = graph.addVertex(T.label, "test")
                val initialTraversal = g.V(vertex)
                
                val expr = intLiteral(99)
                val result = registry.compile(expr, context, initialTraversal)

                // The traversal is bound to the vertex, execute directly
                @Suppress("UNCHECKED_CAST")
                val actualValue = (result.traversal as GraphTraversal<Any, Int>).next()
                assertEquals(99, actualValue)
            }
        }

        @Nested
        @DisplayName("Long Literals")
        inner class LongLiteralTests {

            @Test
            fun `compiles long literal`() {
                val expr = longLiteral(Long.MAX_VALUE)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(Long.MAX_VALUE, actualValue)
                assertEquals(Long.MAX_VALUE, executeWithVertex<Long>(result))
            }

            @Test
            fun `compiles negative long literal`() {
                val expr = longLiteral(Long.MIN_VALUE)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(Long.MIN_VALUE, actualValue)
            }
        }

        @Nested
        @DisplayName("Double Literals")
        inner class DoubleLiteralTests {

            @Test
            fun `compiles double literal`() {
                val expr = doubleLiteral(3.14159)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(3.14159, actualValue)
                assertEquals(3.14159, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles negative double literal`() {
                val expr = doubleLiteral(-2.718)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(-2.718, actualValue)
            }
        }

        @Nested
        @DisplayName("Float Literals")
        inner class FloatLiteralTests {

            @Test
            fun `compiles float literal`() {
                val expr = floatLiteral(1.5f)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(1.5f, actualValue)
                assertEquals(1.5f, executeWithVertex<Float>(result))
            }
        }

        @Nested
        @DisplayName("String Literals")
        inner class StringLiteralTests {

            @Test
            fun `compiles string literal`() {
                val expr = stringLiteral("Hello, World!")
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals("Hello, World!", actualValue)
                assertEquals("Hello, World!", executeWithVertex<String>(result))
            }

            @Test
            fun `compiles empty string literal`() {
                val expr = stringLiteral("")
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals("", actualValue)
                assertEquals("", executeWithVertex<String>(result))
            }

            @Test
            fun `compiles string with special characters`() {
                val expr = stringLiteral("Line1\nLine2\tTab")
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals("Line1\nLine2\tTab", actualValue)
            }
        }

        @Nested
        @DisplayName("Boolean Literals")
        inner class BooleanLiteralTests {

            @Test
            fun `compiles true literal`() {
                val expr = boolLiteral(true)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(true, actualValue)
                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles false literal`() {
                val expr = boolLiteral(false)
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertEquals(false, actualValue)
                assertEquals(false, executeWithVertex<Boolean>(result))
            }
        }

        @Nested
        @DisplayName("Null Literals")
        inner class NullLiteralTests {

            @Test
            fun `compiles null literal`() {
                val expr = nullLiteral()
                val result = registry.compile(expr, context)

                val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
                assertNull(actualValue)
                // Null constant traversal still produces null when executed
                assertNull(executeWithVertex<Any>(result))
            }
        }

        @Nested
        @DisplayName("List Literals")
        inner class ListLiteralTests {

            @Test
            fun `compiles empty list literal`() {
                val expr = listLiteral()
                val result = registry.compile(expr, context)

                // List literals emit individual elements, fold to collect them
                @Suppress("UNCHECKED_CAST")
                val actualValue = g.inject(1).flatMap(result.traversal as GraphTraversal<Any, Any>).fold().next()
                assertEquals(emptyList<Any>(), actualValue)
            }

            @Test
            fun `compiles list of integers`() {
                val expr = listLiteral(intLiteral(1), intLiteral(2), intLiteral(3))
                val result = registry.compile(expr, context)

                // List literals emit individual elements, fold to collect them
                @Suppress("UNCHECKED_CAST")
                val actualValue = g.inject(1).flatMap(result.traversal as GraphTraversal<Any, Any>).fold().next()
                assertEquals(listOf(1, 2, 3), actualValue)
            }

            @Test
            fun `compiles mixed type list`() {
                val expr = listLiteral(intLiteral(1), stringLiteral("two"), boolLiteral(true))
                val result = registry.compile(expr, context)

                // List literals emit individual elements, fold to collect them
                @Suppress("UNCHECKED_CAST")
                val actualValue = g.inject(1).flatMap(result.traversal as GraphTraversal<Any, Any>).fold().next()
                assertEquals(listOf(1, "two", true), actualValue)
            }

            @Test
            fun `compiles nested list`() {
                val innerList = listLiteral(intLiteral(1), intLiteral(2))
                val expr = listLiteral(innerList, intLiteral(3))
                val result = registry.compile(expr, context)

                // List literals emit individual elements - nested lists get flattened
                @Suppress("UNCHECKED_CAST")
                val actualValue = g.inject(1).flatMap(result.traversal as GraphTraversal<Any, Any>).fold().next()
                // Due to union() semantics, nested list elements are emitted individually
                assertEquals(listOf(1, 2, 3), actualValue)
            }
        }
    }

    // =====================================================================
    // BINARY OPERATOR TESTS
    // =====================================================================

    @Nested
    @DisplayName("Binary Operators")
    inner class BinaryOperatorTests {

        @Nested
        @DisplayName("Arithmetic Operators")
        inner class ArithmeticTests {

            @Test
            fun `compiles addition of constants`() {
                val expr = binary("+", intLiteral(5), intLiteral(3))
                val result = registry.compile(expr, context)

                // Execute traversal and verify result - arithmetic returns Double
                assertEquals(8.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles subtraction of constants`() {
                val expr = binary("-", intLiteral(10), intLiteral(4))
                val result = registry.compile(expr, context)

                assertEquals(6.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles multiplication of constants`() {
                val expr = binary("*", intLiteral(7), intLiteral(6))
                val result = registry.compile(expr, context)

                assertEquals(42.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles division of constants`() {
                val expr = binary("/", intLiteral(20), intLiteral(4))
                val result = registry.compile(expr, context)

                assertEquals(5.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles modulo of constants`() {
                val expr = binary("%", intLiteral(17), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(2.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles double arithmetic`() {
                val expr = binary("+", doubleLiteral(1.5), doubleLiteral(2.5))
                val result = registry.compile(expr, context)

                assertEquals(4.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles nested arithmetic expressions`() {
                // (2 + 3) * (10 - 4)
                val add = binary("+", intLiteral(2), intLiteral(3))
                val sub = binary("-", intLiteral(10), intLiteral(4))
                val expr = binary("*", add, sub)
                val result = registry.compile(expr, context)

                assertEquals(30.0, executeWithVertex<Double>(result)) // 5.0 * 6.0 = 30.0
            }
        }

        @Nested
        @DisplayName("Comparison Operators")
        inner class ComparisonTests {

            @Test
            fun `compiles less than - true case`() {
                val expr = binary("<", intLiteral(3), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles less than - false case`() {
                val expr = binary("<", intLiteral(5), intLiteral(3))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles greater than - true case`() {
                val expr = binary(">", intLiteral(10), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles greater than - false case`() {
                val expr = binary(">", intLiteral(3), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles less than or equal - equal case`() {
                val expr = binary("<=", intLiteral(5), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles less than or equal - less case`() {
                val expr = binary("<=", intLiteral(3), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles greater than or equal - equal case`() {
                val expr = binary(">=", intLiteral(5), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles greater than or equal - greater case`() {
                val expr = binary(">=", intLiteral(7), intLiteral(5))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }
        }

        @Nested
        @DisplayName("Equality Operators")
        inner class EqualityTests {

            @Test
            fun `compiles equals - true case`() {
                val expr = binary("==", intLiteral(42), intLiteral(42))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles equals - false case`() {
                val expr = binary("==", intLiteral(42), intLiteral(43))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles not equals - true case`() {
                val expr = binary("!=", intLiteral(42), intLiteral(43))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles not equals - false case`() {
                val expr = binary("!=", intLiteral(42), intLiteral(42))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles string equality`() {
                val expr = binary("==", stringLiteral("hello"), stringLiteral("hello"))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles string inequality`() {
                val expr = binary("!=", stringLiteral("hello"), stringLiteral("world"))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }
        }

        @Nested
        @DisplayName("Logical Operators")
        inner class LogicalTests {

            @Test
            fun `compiles logical AND - true and true`() {
                val expr = binary("&&", boolLiteral(true), boolLiteral(true))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical AND - true and false`() {
                val expr = binary("&&", boolLiteral(true), boolLiteral(false))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical AND - false and true`() {
                val expr = binary("&&", boolLiteral(false), boolLiteral(true))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical AND - false and false`() {
                val expr = binary("&&", boolLiteral(false), boolLiteral(false))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical OR - true or true`() {
                val expr = binary("||", boolLiteral(true), boolLiteral(true))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical OR - true or false`() {
                val expr = binary("||", boolLiteral(true), boolLiteral(false))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical OR - false or true`() {
                val expr = binary("||", boolLiteral(false), boolLiteral(true))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles logical OR - false or false`() {
                val expr = binary("||", boolLiteral(false), boolLiteral(false))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles complex logical expression`() {
                // (true && false) || true = true
                val and = binary("&&", boolLiteral(true), boolLiteral(false))
                val expr = binary("||", and, boolLiteral(true))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }
        }
    }

    // =====================================================================
    // UNARY OPERATOR TESTS
    // =====================================================================

    @Nested
    @DisplayName("Unary Operators")
    inner class UnaryOperatorTests {

        @Nested
        @DisplayName("Numeric Negation")
        inner class NegationTests {

            @Test
            fun `compiles negation of positive integer`() {
                val expr = unary("-", intLiteral(42))
                val result = registry.compile(expr, context)

                assertEquals(-42.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles negation of negative integer`() {
                val expr = unary("-", intLiteral(-10))
                val result = registry.compile(expr, context)

                assertEquals(10.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles negation of zero`() {
                val expr = unary("-", intLiteral(0))
                val result = registry.compile(expr, context)

                assertEquals(0.0, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles negation of double`() {
                val expr = unary("-", doubleLiteral(3.14))
                val result = registry.compile(expr, context)

                assertEquals(-3.14, executeWithVertex<Double>(result))
            }

            @Test
            fun `compiles double negation`() {
                val inner = unary("-", intLiteral(5))
                val expr = unary("-", inner)
                val result = registry.compile(expr, context)

                assertEquals(5.0, executeWithVertex<Double>(result))
            }
        }

        @Nested
        @DisplayName("Unary Plus")
        inner class UnaryPlusTests {

            @Test
            fun `compiles unary plus as identity`() {
                val expr = unary("+", intLiteral(42))
                val result = registry.compile(expr, context)

                assertEquals(42, executeWithVertex<Int>(result))
            }

            @Test
            fun `compiles unary plus on negative`() {
                val expr = unary("+", intLiteral(-10))
                val result = registry.compile(expr, context)

                assertEquals(-10, executeWithVertex<Int>(result))
            }
        }

        @Nested
        @DisplayName("Logical Not")
        inner class LogicalNotTests {

            @Test
            fun `compiles not true`() {
                val expr = unary("!", boolLiteral(true))
                val result = registry.compile(expr, context)

                assertEquals(false, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles not false`() {
                val expr = unary("!", boolLiteral(false))
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }

            @Test
            fun `compiles double negation of boolean`() {
                val inner = unary("!", boolLiteral(true))
                val expr = unary("!", inner)
                val result = registry.compile(expr, context)

                assertEquals(true, executeWithVertex<Boolean>(result))
            }
        }
    }

    // =====================================================================
    // IDENTIFIER RESOLUTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("Identifier Resolution")
    inner class IdentifierTests {

        @Test
        fun `resolves value binding from variable scope`() {
            val scope = VariableScope.of("myVar" to VariableBinding.ValueBinding(42), scopeIndex = 0)
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )

            val expr = identifier("myVar", scope = 0)
            val result = registry.compile(expr, contextWithScope)

            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(42, actualValue)
        }

        @Test
        fun `resolves string value binding`() {
            val scope = VariableScope.of("name" to VariableBinding.ValueBinding("Alice"), scopeIndex = 0)
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )

            val expr = identifier("name", scope = 0)
            val result = registry.compile(expr, contextWithScope)

            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals("Alice", actualValue)
        }

        @Test
        fun `resolves traversal binding with select`() {
            // Add a vertex and label it
            val vertex = graph.addVertex(T.label, "Person", "name", "Bob")
            
            val scope = VariableScope.of("person" to VariableBinding.InstanceBinding(vertexRef = null), scopeIndex = 0)
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )

            val expr = identifier("person", scope = 0)
            val result = registry.compile(expr, contextWithScope)

            // Should produce a select traversal
            assertNotNull(result.traversal)
        }

        @Test
        fun `resolves match-defined variable`() {
            graph.addVertex(T.label, "Person", "name", "Charlie")
            
            val scope = VariableScope.of("p" to VariableBinding.InstanceBinding(vertexRef = null), scopeIndex = 0)
            val contextWithMatch = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )

            val expr = identifier("p", scope = 0)
            val result = registry.compile(expr, contextWithMatch)

            // Should produce a dynamic select traversal
            assertNotNull(result.traversal)
        }
    }

    // =====================================================================
    // TERNARY EXPRESSION TESTS
    // =====================================================================

    @Nested
    @DisplayName("Ternary Expressions")
    inner class TernaryExpressionTests {

        @Test
        fun `compiles ternary with true condition`() {
            val expr = ternary(boolLiteral(true), intLiteral(1), intLiteral(2))
            val result = registry.compile(expr, context)

            // Execute traversal to verify the true branch is selected
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(1, actualValue)
        }

        @Test
        fun `compiles ternary with false condition`() {
            val expr = ternary(boolLiteral(false), intLiteral(1), intLiteral(2))
            val result = registry.compile(expr, context)

            // Execute traversal to verify the false branch is selected
            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(2, actualValue)
        }

        @Test
        fun `compiles ternary with string results`() {
            val expr = ternary(boolLiteral(true), stringLiteral("yes"), stringLiteral("no"))
            val result = registry.compile(expr, context)

            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals("yes", actualValue)
        }

        @Test
        fun `compiles nested ternary expressions`() {
            // true ? (false ? 1 : 2) : 3 => 2
            val inner = ternary(boolLiteral(false), intLiteral(1), intLiteral(2))
            val expr = ternary(boolLiteral(true), inner, intLiteral(3))
            val result = registry.compile(expr, context)

            val actualValue = g.inject(null as Any?).flatMap(result.traversal).next()
            assertEquals(2, actualValue)
        }

        @Test
        fun `compiles ternary with comparison condition`() {
            // (5 > 3) ? 10 : 20 => 10
            val condition = binary(">", intLiteral(5), intLiteral(3))
            val expr = ternary(condition, intLiteral(10), intLiteral(20))
            val result = registry.compile(expr, context)

            // Since comparison is no longer constant folded, execute the traversal
            assertEquals(10, executeWithVertex<Int>(result))
        }
    }

    // =====================================================================
    // INITIAL TRAVERSAL PROPAGATION TESTS
    // =====================================================================

    @Nested
    @DisplayName("Initial Traversal Propagation")
    inner class InitialTraversalTests {

        @Test
        fun `propagates initial traversal for constants`() {
            val vertex = graph.addVertex(T.label, "Test", "value", 100)
            val initialTraversal = g.V(vertex)

            val expr = intLiteral(42)
            val result = registry.compile(expr, context, initialTraversal)

            // Execute and verify constant is produced - traversal is bound to vertex
            @Suppress("UNCHECKED_CAST")
            val actualValue = (result.traversal as GraphTraversal<Any, Int>).next()
            assertEquals(42, actualValue)
        }

        @Test
        fun `propagates initial traversal through binary expression`() {
            val vertex = graph.addVertex(T.label, "Test")
            val initialTraversal = g.V(vertex)

            val expr = binary("+", intLiteral(10), intLiteral(5))
            val result = registry.compile(expr, context, initialTraversal)

            // Execute and verify result - arithmetic returns Double, traversal is bound to vertex
            @Suppress("UNCHECKED_CAST")
            val actualValue = (result.traversal as GraphTraversal<Any, Double>).next()
            assertEquals(15.0, actualValue)
        }

        @Test
        fun `propagates initial traversal through unary expression`() {
            val vertex = graph.addVertex(T.label, "Test")
            val initialTraversal = g.V(vertex)

            val expr = unary("-", intLiteral(7))
            val result = registry.compile(expr, context, initialTraversal)

            // Execute and verify result - negation returns Double, traversal is bound to vertex
            @Suppress("UNCHECKED_CAST")
            val actualValue = (result.traversal as GraphTraversal<Any, Double>).next()
            assertEquals(-7.0, actualValue)
        }
    }

    // =====================================================================
    // INTEGRATION TESTS WITH GRAPH PATTERNS
    // =====================================================================

}


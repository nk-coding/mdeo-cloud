package com.mdeo.modeltransformation.stdlib

import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests verifying that the Gremlin math() expressions used in
 * DoubleTypeDefinition and FloatTypeDefinition for sqrt and
 * trigonometric methods evaluate correctly via TinkerGraph.
 */
class NumericMathMethodsGremlinTest {

    private lateinit var graph: TinkerGraph

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    // ==================== sqrt ====================

    @Nested
    inner class SqrtMethod {

        @Test
        fun `sqrt of 4 returns 2`() {
            val result = graph.traversal().inject(4.0).math("sqrt(_)").next() as Double
            assertEquals(2.0, result, 0.0001)
        }

        @Test
        fun `sqrt of 9 returns 3`() {
            val result = graph.traversal().inject(9.0).math("sqrt(_)").next() as Double
            assertEquals(3.0, result, 0.0001)
        }

        @Test
        fun `sqrt of 2 returns approximately 1_414`() {
            val result = graph.traversal().inject(2.0).math("sqrt(_)").next() as Double
            assertEquals(1.41421356, result, 0.0001)
        }

        @Test
        fun `sqrt of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("sqrt(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `sqrt of 25 returns 5`() {
            val result = graph.traversal().inject(25.0).math("sqrt(_)").next() as Double
            assertEquals(5.0, result, 0.0001)
        }
    }

    // ==================== sin ====================

    @Nested
    inner class SinMethod {

        @Test
        fun `sin of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("sin(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `sin of pi over 2 returns 1`() {
            val result = graph.traversal().inject(PI / 2).math("sin(_)").next() as Double
            assertEquals(1.0, result, 0.0001)
        }

        @Test
        fun `sin of pi returns approximately 0`() {
            val result = graph.traversal().inject(PI).math("sin(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `sin of negative pi over 2 returns -1`() {
            val result = graph.traversal().inject(-PI / 2).math("sin(_)").next() as Double
            assertEquals(-1.0, result, 0.0001)
        }

        @Test
        fun `sin result is between -1 and 1`() {
            val result = graph.traversal().inject(1.0).math("sin(_)").next() as Double
            assertTrue(result >= -1.0 && result <= 1.0)
        }
    }

    // ==================== cos ====================

    @Nested
    inner class CosMethod {

        @Test
        fun `cos of 0 returns 1`() {
            val result = graph.traversal().inject(0.0).math("cos(_)").next() as Double
            assertEquals(1.0, result, 0.0001)
        }

        @Test
        fun `cos of pi over 2 returns approximately 0`() {
            val result = graph.traversal().inject(PI / 2).math("cos(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `cos of pi returns -1`() {
            val result = graph.traversal().inject(PI).math("cos(_)").next() as Double
            assertEquals(-1.0, result, 0.0001)
        }

        @Test
        fun `cos of 2pi returns 1`() {
            val result = graph.traversal().inject(2 * PI).math("cos(_)").next() as Double
            assertEquals(1.0, result, 0.0001)
        }

        @Test
        fun `cos result is between -1 and 1`() {
            val result = graph.traversal().inject(1.0).math("cos(_)").next() as Double
            assertTrue(result >= -1.0 && result <= 1.0)
        }
    }

    // ==================== tan ====================

    @Nested
    inner class TanMethod {

        @Test
        fun `tan of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("tan(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `tan of pi over 4 returns 1`() {
            val result = graph.traversal().inject(PI / 4).math("tan(_)").next() as Double
            assertEquals(1.0, result, 0.0001)
        }

        @Test
        fun `tan of negative pi over 4 returns -1`() {
            val result = graph.traversal().inject(-PI / 4).math("tan(_)").next() as Double
            assertEquals(-1.0, result, 0.0001)
        }

        @Test
        fun `tan of pi returns approximately 0`() {
            val result = graph.traversal().inject(PI).math("tan(_)").next() as Double
            assertEquals(0.0, result, 0.001)
        }
    }

    // ==================== asin ====================

    @Nested
    inner class AsinMethod {

        @Test
        fun `asin of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("asin(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `asin of 1 returns pi over 2`() {
            val result = graph.traversal().inject(1.0).math("asin(_)").next() as Double
            assertEquals(PI / 2, result, 0.0001)
        }

        @Test
        fun `asin of -1 returns negative pi over 2`() {
            val result = graph.traversal().inject(-1.0).math("asin(_)").next() as Double
            assertEquals(-PI / 2, result, 0.0001)
        }

        @Test
        fun `asin of 0_5 returns approximately pi over 6`() {
            val result = graph.traversal().inject(0.5).math("asin(_)").next() as Double
            assertEquals(PI / 6, result, 0.0001)
        }
    }

    // ==================== acos ====================

    @Nested
    inner class AcosMethod {

        @Test
        fun `acos of 1 returns 0`() {
            val result = graph.traversal().inject(1.0).math("acos(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `acos of 0 returns pi over 2`() {
            val result = graph.traversal().inject(0.0).math("acos(_)").next() as Double
            assertEquals(PI / 2, result, 0.0001)
        }

        @Test
        fun `acos of -1 returns pi`() {
            val result = graph.traversal().inject(-1.0).math("acos(_)").next() as Double
            assertEquals(PI, result, 0.0001)
        }

        @Test
        fun `acos of 0_5 returns approximately pi over 3`() {
            val result = graph.traversal().inject(0.5).math("acos(_)").next() as Double
            assertEquals(PI / 3, result, 0.0001)
        }
    }

    // ==================== atan ====================

    @Nested
    inner class AtanMethod {

        @Test
        fun `atan of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("atan(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `atan of 1 returns pi over 4`() {
            val result = graph.traversal().inject(1.0).math("atan(_)").next() as Double
            assertEquals(PI / 4, result, 0.0001)
        }

        @Test
        fun `atan of -1 returns negative pi over 4`() {
            val result = graph.traversal().inject(-1.0).math("atan(_)").next() as Double
            assertEquals(-PI / 4, result, 0.0001)
        }

        @Test
        fun `atan result is within valid range`() {
            val result = graph.traversal().inject(100.0).math("atan(_)").next() as Double
            assertTrue(result > -PI / 2 && result < PI / 2)
        }
    }

    // ==================== sinh ====================

    @Nested
    inner class SinhMethod {

        @Test
        fun `sinh of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("sinh(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `sinh of 1 is approximately 1_175`() {
            val result = graph.traversal().inject(1.0).math("sinh(_)").next() as Double
            assertEquals(1.1752012, result, 0.0001)
        }

        @Test
        fun `sinh is odd function`() {
            val pos = graph.traversal().inject(1.0).math("sinh(_)").next() as Double
            val neg = graph.traversal().inject(-1.0).math("sinh(_)").next() as Double
            assertEquals(-pos, neg, 0.0001)
        }

        @Test
        fun `sinh grows with input`() {
            val s1 = graph.traversal().inject(1.0).math("sinh(_)").next() as Double
            val s2 = graph.traversal().inject(2.0).math("sinh(_)").next() as Double
            assertTrue(s2 > s1)
        }
    }

    // ==================== cosh ====================

    @Nested
    inner class CoshMethod {

        @Test
        fun `cosh of 0 returns 1`() {
            val result = graph.traversal().inject(0.0).math("cosh(_)").next() as Double
            assertEquals(1.0, result, 0.0001)
        }

        @Test
        fun `cosh of 1 is approximately 1_543`() {
            val result = graph.traversal().inject(1.0).math("cosh(_)").next() as Double
            assertEquals(1.5430806, result, 0.0001)
        }

        @Test
        fun `cosh is even function`() {
            val pos = graph.traversal().inject(1.0).math("cosh(_)").next() as Double
            val neg = graph.traversal().inject(-1.0).math("cosh(_)").next() as Double
            assertEquals(pos, neg, 0.0001)
        }

        @Test
        fun `cosh is always at least 1`() {
            val result = graph.traversal().inject(5.0).math("cosh(_)").next() as Double
            assertTrue(result >= 1.0)
        }
    }

    // ==================== tanh ====================

    @Nested
    inner class TanhMethod {

        @Test
        fun `tanh of 0 returns 0`() {
            val result = graph.traversal().inject(0.0).math("tanh(_)").next() as Double
            assertEquals(0.0, result, 0.0001)
        }

        @Test
        fun `tanh approaches 1 for large positive values`() {
            val result = graph.traversal().inject(100.0).math("tanh(_)").next() as Double
            assertTrue(result > 0.999)
        }

        @Test
        fun `tanh approaches -1 for large negative values`() {
            val result = graph.traversal().inject(-100.0).math("tanh(_)").next() as Double
            assertTrue(result < -0.999)
        }

        @Test
        fun `tanh result is between -1 and 1`() {
            val result = graph.traversal().inject(2.0).math("tanh(_)").next() as Double
            assertTrue(result > -1.0 && result < 1.0)
        }

        @Test
        fun `tanh is odd function`() {
            val pos = graph.traversal().inject(1.0).math("tanh(_)").next() as Double
            val neg = graph.traversal().inject(-1.0).math("tanh(_)").next() as Double
            assertEquals(-pos, neg, 0.0001)
        }
    }
}

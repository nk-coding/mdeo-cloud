package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.expressions.*
import com.mdeo.modeltransformation.ast.expressions.TypedLambdaExpression
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressionNodeAnalyzerTest {

    private val matchNodes = setOf("nodeA", "nodeB", "nodeC")

    private fun analyzer(scopeIndex: Int = 1) =
        ExpressionNodeAnalyzer(matchNodes, scopeIndex)

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun ident(name: String, scope: Int = 0) =
        TypedIdentifierExpression(evalType = 0, name = name, scope = scope)

    private fun strLit(value: String = "hello") =
        TypedStringLiteralExpression(evalType = 0, value = value)

    private fun intLit(value: String = "42") =
        TypedIntLiteralExpression(evalType = 0, value = value)

    private fun boolLit(value: Boolean = true) =
        TypedBooleanLiteralExpression(evalType = 0, value = value)

    private fun nullLit() =
        TypedNullLiteralExpression(evalType = 0)

    private fun memberAccess(expr: TypedExpression, member: String) =
        TypedMemberAccessExpression(evalType = 0, expression = expr, member = member, isNullChaining = false)

    private fun binary(left: TypedExpression, op: String, right: TypedExpression) =
        TypedBinaryExpression(evalType = 0, operator = op, left = left, right = right)

    private fun unary(op: String, expr: TypedExpression) =
        TypedUnaryExpression(evalType = 0, operator = op, expression = expr)

    private fun arg(expr: TypedExpression) =
        TypedCallArgument(value = expr, parameterType = 0)

    private fun memberCall(expr: TypedExpression, member: String, args: List<TypedExpression> = emptyList()) =
        TypedMemberCallExpression(
            evalType = 0, expression = expr, member = member,
            isNullChaining = false, overload = "", arguments = args.map { arg(it) }
        )

    private fun functionCall(name: String, args: List<TypedExpression> = emptyList()) =
        TypedFunctionCallExpression(
            evalType = 0, name = name, overload = "", arguments = args.map { arg(it) }
        )

    private fun expressionCall(expr: TypedExpression, args: List<TypedExpression> = emptyList()) =
        TypedExpressionCallExpression(
            evalType = 0, expression = expr, arguments = args.map { arg(it) }
        )

    private fun extensionCall(name: String, args: List<Pair<String, TypedExpression>> = emptyList()) =
        TypedExtensionCallExpression(
            evalType = 0, name = name, overload = "",
            arguments = args.map { TypedExtensionCallArgument(name = it.first, value = it.second) }
        )

    private fun ternary(cond: TypedExpression, t: TypedExpression, f: TypedExpression) =
        TypedTernaryExpression(evalType = 0, condition = cond, trueExpression = t, falseExpression = f)

    private fun listLit(vararg elements: TypedExpression) =
        TypedListLiteralExpression(evalType = 0, elements = elements.toList())

    private fun typeCheck(expr: TypedExpression) =
        TypedTypeCheckExpression(evalType = 0, expression = expr, checkType = 0, isNegated = false)

    private fun typeCast(expr: TypedExpression) =
        TypedTypeCastExpression(evalType = 0, expression = expr, targetType = 0, isSafe = false)

    private fun assertNonNull(expr: TypedExpression) =
        TypedAssertNonNullExpression(evalType = 0, expression = expr)

    private fun lambda(body: TypedExpression, params: List<String> = listOf("it")) =
        TypedLambdaExpression(evalType = 0, parameters = params, body = body)

    // ── Tests ────────────────────────────────────────────────────────────

    @Nested
    inner class Literals {
        @Test
        fun `string literal has no node references`() {
            assertEquals(emptySet(), analyzer().findReferencedNodes(strLit()))
        }

        @Test
        fun `int literal has no node references`() {
            assertEquals(emptySet(), analyzer().findReferencedNodes(intLit()))
        }

        @Test
        fun `boolean literal has no node references`() {
            assertEquals(emptySet(), analyzer().findReferencedNodes(boolLit()))
        }

        @Test
        fun `null literal has no node references`() {
            assertEquals(emptySet(), analyzer().findReferencedNodes(nullLit()))
        }
    }

    @Nested
    inner class IdentifierExpressions {
        @Test
        fun `identifier matching a node at matching scope is detected`() {
            val result = analyzer(scopeIndex = 1).findReferencedNodes(ident("nodeA", scope = 0))
            assertEquals(setOf("nodeA"), result)
        }

        @Test
        fun `identifier matching a node at exact scope boundary is detected`() {
            val result = analyzer(scopeIndex = 1).findReferencedNodes(ident("nodeA", scope = 1))
            assertEquals(setOf("nodeA"), result)
        }

        @Test
        fun `identifier at higher scope than currentScopeIndex is NOT detected`() {
            val result = analyzer(scopeIndex = 2).findReferencedNodes(ident("nodeA", scope = 3))
            assertEquals(emptySet(), result)
        }

        @Test
        fun `identifier not in matchNodeNames is NOT detected even if scope matches`() {
            val result = analyzer(scopeIndex = 1).findReferencedNodes(ident("someVariable", scope = 0))
            assertEquals(emptySet(), result)
        }

        @Test
        fun `identifier at scope 0 with currentScopeIndex 0 is detected`() {
            val result = analyzer(scopeIndex = 0).findReferencedNodes(ident("nodeB", scope = 0))
            assertEquals(setOf("nodeB"), result)
        }
    }

    @Nested
    inner class MemberAccessExpressions {
        @Test
        fun `member access on a node identifier detects the node`() {
            val expr = memberAccess(ident("nodeA", scope = 0), "property")
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `member access on a non-node identifier returns empty`() {
            val expr = memberAccess(ident("variable", scope = 0), "property")
            assertEquals(emptySet(), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `chained member access detects the root node`() {
            // nodeA.items.size → should find nodeA
            val inner = memberAccess(ident("nodeA", scope = 0), "items")
            val outer = memberAccess(inner, "size")
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(outer))
        }

        @Test
        fun `deeply nested member access chain detects the root node`() {
            // nodeA.a.b.c.d.e → should find nodeA
            var expr: TypedExpression = ident("nodeA", scope = 0)
            for (m in listOf("a", "b", "c", "d", "e")) {
                expr = memberAccess(expr, m)
            }
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class BinaryExpressions {
        @Test
        fun `binary expression with two different nodes finds both`() {
            val expr = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "+",
                memberAccess(ident("nodeB", scope = 0), "y")
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `binary expression with node and literal finds only the node`() {
            val expr = binary(memberAccess(ident("nodeA", scope = 0), "x"), "+", intLit("5"))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `binary expression with two literals finds nothing`() {
            val expr = binary(intLit("1"), "+", intLit("2"))
            assertEquals(emptySet(), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `binary expression with same node on both sides returns one entry`() {
            val expr = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "==",
                memberAccess(ident("nodeA", scope = 0), "y")
            )
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class UnaryExpressions {
        @Test
        fun `unary expression on node reference detects it`() {
            val expr = unary("!", memberAccess(ident("nodeA", scope = 0), "active"))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `unary expression on literal returns empty`() {
            val expr = unary("-", intLit("1"))
            assertEquals(emptySet(), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class CallExpressions {
        @Test
        fun `member call on node detects the node`() {
            val expr = memberCall(ident("nodeA", scope = 0), "toString")
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `member call with node arguments detects all referenced nodes`() {
            val expr = memberCall(
                ident("nodeA", scope = 0), "compareTo",
                listOf(memberAccess(ident("nodeB", scope = 0), "name"))
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `function call with node arguments detects referenced nodes`() {
            val expr = functionCall("max", listOf(
                memberAccess(ident("nodeA", scope = 0), "x"),
                memberAccess(ident("nodeB", scope = 0), "x")
            ))
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `function call with no node arguments returns empty`() {
            val expr = functionCall("max", listOf(intLit("1"), intLit("2")))
            assertEquals(emptySet(), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `expression call detects target and argument nodes`() {
            val expr = expressionCall(
                ident("nodeA", scope = 0),
                listOf(memberAccess(ident("nodeB", scope = 0), "val"))
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `extension call detects nodes in arguments`() {
            val expr = extensionCall("ext", listOf(
                "first" to memberAccess(ident("nodeA", scope = 0), "x"),
                "second" to intLit("5")
            ))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `nested member call chain detects root node`() {
            // nodeA.items.size() → should find nodeA
            val inner = memberAccess(ident("nodeA", scope = 0), "items")
            val expr = memberCall(inner, "size")
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class TernaryExpressions {
        @Test
        fun `ternary detects nodes in all three branches`() {
            val expr = ternary(
                memberAccess(ident("nodeA", scope = 0), "flag"),
                memberAccess(ident("nodeB", scope = 0), "x"),
                memberAccess(ident("nodeC", scope = 0), "y")
            )
            assertEquals(setOf("nodeA", "nodeB", "nodeC"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `ternary with literals in branches only detects condition node`() {
            val expr = ternary(
                memberAccess(ident("nodeA", scope = 0), "flag"),
                intLit("1"),
                intLit("0")
            )
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class ListLiteralExpressions {
        @Test
        fun `list literal with node references detects all`() {
            val expr = listLit(
                memberAccess(ident("nodeA", scope = 0), "x"),
                intLit("5"),
                memberAccess(ident("nodeB", scope = 0), "y")
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `empty list literal returns empty`() {
            val expr = listLit()
            assertEquals(emptySet(), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class TypeExpressions {
        @Test
        fun `type check on node reference detects it`() {
            val expr = typeCheck(ident("nodeA", scope = 0))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `type cast on node reference detects it`() {
            val expr = typeCast(ident("nodeA", scope = 0))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `assert non-null on node reference detects it`() {
            val expr = assertNonNull(ident("nodeA", scope = 0))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class LambdaExpressions {
        @Test
        fun `lambda body referencing a node detects it`() {
            val expr = lambda(memberAccess(ident("nodeA", scope = 0), "name"))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `lambda body with no node references returns empty`() {
            val expr = lambda(intLit("42"))
            assertEquals(emptySet(), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `lambda body with multiple node references detects all`() {
            val body = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "+",
                memberAccess(ident("nodeB", scope = 0), "y")
            )
            val expr = lambda(body)
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class ScopeLevels {
        @Test
        fun `identifiers at scopes 0 and 1 detected with currentScopeIndex 1`() {
            val expr = binary(
                ident("nodeA", scope = 0),
                "+",
                ident("nodeB", scope = 1)
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer(scopeIndex = 1).findReferencedNodes(expr))
        }

        @Test
        fun `identifier at scope 2 NOT detected with currentScopeIndex 1`() {
            val expr = binary(
                ident("nodeA", scope = 0),
                "+",
                ident("nodeB", scope = 2)
            )
            assertEquals(setOf("nodeA"), analyzer(scopeIndex = 1).findReferencedNodes(expr))
        }

        @Test
        fun `all identifiers above currentScopeIndex are excluded`() {
            val expr = binary(
                ident("nodeA", scope = 3),
                "+",
                ident("nodeB", scope = 4)
            )
            assertEquals(emptySet(), analyzer(scopeIndex = 2).findReferencedNodes(expr))
        }

        @Test
        fun `mixed scope levels with currentScopeIndex 2`() {
            // scope 0 → included, scope 1 → included, scope 2 → included, scope 3 → excluded
            val list = listLit(
                ident("nodeA", scope = 0),
                ident("nodeB", scope = 2),
                ident("nodeC", scope = 3)
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer(scopeIndex = 2).findReferencedNodes(list))
        }
    }

    @Nested
    inner class AllNodesAreBound {
        @Test
        fun `true when all referenced nodes are in bound set`() {
            val expr = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "==",
                memberAccess(ident("nodeB", scope = 0), "y")
            )
            assertTrue(analyzer().allNodesAreBound(expr, setOf("nodeA", "nodeB")))
        }

        @Test
        fun `true when bound set is superset of referenced nodes`() {
            val expr = memberAccess(ident("nodeA", scope = 0), "x")
            assertTrue(analyzer().allNodesAreBound(expr, setOf("nodeA", "nodeB", "nodeC")))
        }

        @Test
        fun `false when a referenced node is not in bound set`() {
            val expr = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "==",
                memberAccess(ident("nodeB", scope = 0), "y")
            )
            assertFalse(analyzer().allNodesAreBound(expr, setOf("nodeA")))
        }

        @Test
        fun `true for expression with no node references`() {
            assertTrue(analyzer().allNodesAreBound(intLit("5"), emptySet()))
        }

        @Test
        fun `false when bound set is empty but expression references nodes`() {
            val expr = memberAccess(ident("nodeA", scope = 0), "x")
            assertFalse(analyzer().allNodesAreBound(expr, emptySet()))
        }
    }

    @Nested
    inner class IsNodeFree {
        @Test
        fun `true for pure literals`() {
            assertTrue(analyzer().isNodeFree(intLit("5")))
            assertTrue(analyzer().isNodeFree(strLit("abc")))
            assertTrue(analyzer().isNodeFree(boolLit(false)))
            assertTrue(analyzer().isNodeFree(nullLit()))
        }

        @Test
        fun `true for identifier not in matchNodeNames`() {
            assertTrue(analyzer().isNodeFree(ident("localVar", scope = 0)))
        }

        @Test
        fun `true for identifier above currentScopeIndex even if name matches`() {
            assertTrue(analyzer(scopeIndex = 0).isNodeFree(ident("nodeA", scope = 1)))
        }

        @Test
        fun `false when expression references a match node`() {
            assertFalse(analyzer().isNodeFree(memberAccess(ident("nodeA", scope = 0), "x")))
        }

        @Test
        fun `false for binary with one node reference`() {
            val expr = binary(intLit("1"), "+", memberAccess(ident("nodeB", scope = 0), "y"))
            assertFalse(analyzer().isNodeFree(expr))
        }
    }

    @Nested
    inner class ComplexExpressions {
        @Test
        fun `nested binary and member access detects all nodes`() {
            // (nodeA.x + nodeB.y) * nodeC.z
            val inner = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "+",
                memberAccess(ident("nodeB", scope = 0), "y")
            )
            val expr = binary(inner, "*", memberAccess(ident("nodeC", scope = 0), "z"))
            assertEquals(setOf("nodeA", "nodeB", "nodeC"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `member call inside ternary detects all nodes`() {
            val expr = ternary(
                boolLit(true),
                memberCall(ident("nodeA", scope = 0), "getName"),
                memberAccess(ident("nodeB", scope = 0), "fallback")
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `lambda inside member call argument detects nodes`() {
            val lambdaBody = memberAccess(ident("nodeB", scope = 0), "name")
            val expr = memberCall(
                ident("nodeA", scope = 0), "filter",
                listOf(lambda(lambdaBody))
            )
            assertEquals(setOf("nodeA", "nodeB"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `list literal with mixed expressions detects only node references`() {
            val expr = listLit(
                intLit("1"),
                strLit("hello"),
                memberAccess(ident("nodeA", scope = 0), "x"),
                boolLit(true),
                ident("localVar", scope = 0)
            )
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `assert non-null on member access chain detects node`() {
            val expr = assertNonNull(memberAccess(ident("nodeA", scope = 0), "nullable"))
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }

        @Test
        fun `type cast inside binary expression detects node`() {
            val expr = binary(
                typeCast(ident("nodeA", scope = 0)),
                "==",
                strLit("expected")
            )
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `empty matchNodeNames means no identifiers are ever detected`() {
            val emptyAnalyzer = ExpressionNodeAnalyzer(emptySet(), currentScopeIndex = 10)
            val expr = ident("nodeA", scope = 0)
            assertEquals(emptySet(), emptyAnalyzer.findReferencedNodes(expr))
        }

        @Test
        fun `allNodesAreBound is true for literal with empty bound set`() {
            assertTrue(analyzer().allNodesAreBound(strLit(), emptySet()))
        }

        @Test
        fun `isNodeFree is true when matchNodeNames is empty`() {
            val emptyAnalyzer = ExpressionNodeAnalyzer(emptySet(), currentScopeIndex = 10)
            assertTrue(emptyAnalyzer.isNodeFree(ident("nodeA", scope = 0)))
        }

        @Test
        fun `same node referenced multiple times still appears once in result`() {
            val expr = binary(
                memberAccess(ident("nodeA", scope = 0), "x"),
                "+",
                memberAccess(ident("nodeA", scope = 0), "y")
            )
            assertEquals(setOf("nodeA"), analyzer().findReferencedNodes(expr))
        }
    }
}

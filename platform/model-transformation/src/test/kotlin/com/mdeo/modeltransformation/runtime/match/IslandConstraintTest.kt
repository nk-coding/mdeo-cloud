package com.mdeo.modeltransformation.runtime.match

import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedStringLiteralExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.runtime.*
import com.mdeo.modeltransformation.runtime.statements.MatchStatementExecutor
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for the island-based forbid and require evaluation model.
 *
 * Forbid and require constraints that reference nodes not part of the main match
 * pattern are grouped into "islands" — connected components of constraint nodes
 * and links. Each island is evaluated independently:
 *
 * - **Forbid islands** generate `where(not(...))` clauses: the match succeeds
 *   only when the entire island subpattern does NOT exist.
 * - **Require islands** generate existential checks: the match succeeds only
 *   when the entire island subpattern DOES exist. Require islands are pure
 *   filters and do not multiply the result set.
 *
 * Islands may be connected to matched nodes (via links whose source or target
 * is a matched node) or completely disconnected (no links to any matched node).
 */
class IslandConstraintTest {

    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    private lateinit var context: TransformationExecutionContext
    private lateinit var executor: MatchStatementExecutor

    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        engine = TransformationEngine(
            traversalSource = graph.traversal(),
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
            expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
            statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
        )
        context = TransformationExecutionContext.empty()
        executor = MatchStatementExecutor()
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    // ========================================================================
    // 1. Forbid Island Tests
    // ========================================================================

    @Nested
    inner class ForbidIslandTests {

        @Test
        fun `1a - disconnected forbid node succeeds when forbidden class absent`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `1a - disconnected forbid node fails when forbidden class present`() {
            graph.addVertex("House")
            graph.addVertex("Room")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `1b - connected forbid island succeeds when no forbidden target exists`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", null)
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `1b - connected forbid island fails when forbidden target exists`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", null), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", null)
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `1c - forbid chain succeeds when neither chained node exists`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidInstance("garden", "Garden"),
                        forbidLink("house", "rooms", "room", "house"),
                        forbidLink("room", "gardens", "garden", "room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `1c - forbid chain succeeds when only partial chain exists`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidInstance("garden", "Garden"),
                        forbidLink("house", "rooms", "room", "house"),
                        forbidLink("room", "gardens", "garden", "room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `1c - forbid chain fails when full chain exists`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            val garden = graph.addVertex("Garden")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)
            room.addEdge(EdgeLabelUtils.computeEdgeLabel("gardens", "room"), garden)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidInstance("garden", "Garden"),
                        forbidLink("house", "rooms", "room", "house"),
                        forbidLink("room", "gardens", "garden", "room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `1d - multiple separate forbid islands both absent succeeds`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", null),
                        forbidInstance("garage", "Garage")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `1d - multiple forbid islands fails when connected island matches`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", null), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", null),
                        forbidInstance("garage", "Garage")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `1d - multiple forbid islands fails when disconnected island matches`() {
            graph.addVertex("House")
            graph.addVertex("Garage")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        forbidInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", null),
                        forbidInstance("garage", "Garage")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `1e - forbid link between two matched nodes succeeds when no edge`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        matchInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", "house")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `1e - forbid link between two matched nodes fails when edge exists`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        matchInstance("room", "Room"),
                        forbidLink("house", "rooms", "room", "house")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }
    }

    // ========================================================================
    // 1f. Branching Forbid Island Tests
    //
    // Reproduces the bug in buildIslandChainTraversal where a branching (tree-shaped)
    // forbid pattern requires BFS backtracking via .select() to a forbid node that was
    // never labeled with .as() — because the label was only applied when
    // matchableNames.contains(toNode).
    //
    // Scenario topology:
    //   Match:  A
    //   Forbid: C, D, E
    //   Links:  A->C, C->D, C->E
    //
    // BFS produces orderedLinks = [(A->C), (C->D), (C->E)].
    // After walking A->C->D, the chain attempts to backtrack to C via
    //   chain.select(step_C)
    // but C was never labeled. This causes a Gremlin error.
    // ========================================================================

    @Nested
    inner class ForbidBranchingIslandTests {

        /**
         * Full forbid branch present: the match on A should be blocked because
         * both C->D and C->E sub-branches of the forbidden tree exist.
         *
         * Before the fix this test throws a Gremlin exception caused by the missing
         * .as() label on the intermediate forbid node C.
         */
        @Test
        fun `1f - branching forbid island fails when full branch exists`() {
            val a = graph.addVertex("NodeA")
            val c = graph.addVertex("NodeC")
            val d = graph.addVertex("NodeD")
            val e = graph.addVertex("NodeE")
            a.addEdge(EdgeLabelUtils.computeEdgeLabel("children", "parent"), c)
            c.addEdge(EdgeLabelUtils.computeEdgeLabel("left", "owner"), d)
            c.addEdge(EdgeLabelUtils.computeEdgeLabel("right", "owner"), e)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("a", "NodeA"),
                        forbidInstance("c", "NodeC"),
                        forbidInstance("d", "NodeD"),
                        forbidInstance("e", "NodeE"),
                        forbidLink("a", "children", "c", "parent"),
                        forbidLink("c", "left", "d", "owner"),
                        forbidLink("c", "right", "e", "owner")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        /**
         * Only the first branch (C->D) exists, but not the second (C->E).
         * The forbid pattern requires all three nodes C, D, E, so the partial match
         * should NOT trigger the forbid — the match on A should succeed.
         *
         * Before the fix this test also throws a Gremlin exception.
         */
        @Test
        fun `1f - branching forbid island succeeds when second branch is absent`() {
            val a = graph.addVertex("NodeA")
            val c = graph.addVertex("NodeC")
            val d = graph.addVertex("NodeD")
            // NodeE is absent; the second branch does not exist
            a.addEdge(EdgeLabelUtils.computeEdgeLabel("children", "parent"), c)
            c.addEdge(EdgeLabelUtils.computeEdgeLabel("left", "owner"), d)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("a", "NodeA"),
                        forbidInstance("c", "NodeC"),
                        forbidInstance("d", "NodeD"),
                        forbidInstance("e", "NodeE"),
                        forbidLink("a", "children", "c", "parent"),
                        forbidLink("c", "left", "d", "owner"),
                        forbidLink("c", "right", "e", "owner")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        /**
         * No forbidden nodes exist at all. The match on A should succeed trivially.
         *
         * Before the fix this test is expected to also throw.
         */
        @Test
        fun `1f - branching forbid island succeeds when no forbidden nodes exist`() {
            graph.addVertex("NodeA")
            // No NodeC, NodeD, or NodeE in the graph

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("a", "NodeA"),
                        forbidInstance("c", "NodeC"),
                        forbidInstance("d", "NodeD"),
                        forbidInstance("e", "NodeE"),
                        forbidLink("a", "children", "c", "parent"),
                        forbidLink("c", "left", "d", "owner"),
                        forbidLink("c", "right", "e", "owner")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }
    }

    // ========================================================================
    // 2. Require Island Tests
    // ========================================================================

    @Nested
    inner class RequireIslandTests {

        @Test
        fun `2a - connected require island succeeds when required target exists`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireLink("house", "rooms", "room", "house")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `2a - connected require island fails when required target absent`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireLink("house", "rooms", "room", "house")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `2b - disconnected require node succeeds when required class exists`() {
            graph.addVertex("House")
            graph.addVertex("Room")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `2b - disconnected require node fails when required class absent`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `2c - connected require chain succeeds when full chain exists`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            val window = graph.addVertex("Window")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)
            room.addEdge(EdgeLabelUtils.computeEdgeLabel("windows", "room"), window)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireInstance("window", "Window"),
                        requireLink("house", "rooms", "room", "house"),
                        requireLink("room", "windows", "window", "room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `2c - connected require chain fails when tail of chain missing`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireInstance("window", "Window"),
                        requireLink("house", "rooms", "room", "house"),
                        requireLink("room", "windows", "window", "room")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `2d - require does not multiply matches`() {
            val house = graph.addVertex("House")
            val room1 = graph.addVertex("Room")
            val room2 = graph.addVertex("Room")
            val room3 = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room1)
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room2)
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room3)

            val pattern = TypedPattern(
                elements = listOf(
                    matchInstance("house", "House"),
                    requireInstance("room", "Room"),
                    requireLink("house", "rooms", "room", "house")
                )
            )

            val matchExecutor = MatchExecutor()
            val results = matchExecutor.executeMatchAll(pattern, context, engine)
            assertEquals(1, results.size)
        }
    }

    // ========================================================================
    // 3. Mixed Forbid + Require + Match
    // ========================================================================

    @Nested
    inner class MixedConstraintTests {

        @Test
        fun `3a - require satisfied and forbid absent succeeds`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireLink("house", "rooms", "room", "house"),
                        forbidInstance("garage", "Garage")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }

        @Test
        fun `3a - require satisfied but forbid present fails`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)
            graph.addVertex("Garage")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireLink("house", "rooms", "room", "house"),
                        forbidInstance("garage", "Garage")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `3a - require absent and forbid absent fails`() {
            graph.addVertex("House")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        requireInstance("room", "Room"),
                        requireLink("house", "rooms", "room", "house"),
                        forbidInstance("garage", "Garage")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `mixed forbid and require islands with match links`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        matchInstance("room", "Room"),
                        matchLink("house", "rooms", "room", "house"),
                        forbidInstance("pool", "Pool"),
                        forbidLink("house", "pools", "pool", null),
                        requireInstance("garden", "Garden")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Failure>(result)
        }

        @Test
        fun `mixed forbid and require islands with all satisfied`() {
            val house = graph.addVertex("House")
            val room = graph.addVertex("Room")
            house.addEdge(EdgeLabelUtils.computeEdgeLabel("rooms", "house"), room)
            graph.addVertex("Garden")

            val statement = TypedMatchStatement(
                pattern = TypedPattern(
                    elements = listOf(
                        matchInstance("house", "House"),
                        matchInstance("room", "Room"),
                        matchLink("house", "rooms", "room", "house"),
                        forbidInstance("pool", "Pool"),
                        forbidLink("house", "pools", "pool", null),
                        requireInstance("garden", "Garden")
                    )
                )
            )

            val result = engine.executeStatement(statement, context)
            assertIs<TransformationExecutionResult.Success>(result)
        }
    }

    // ========================================================================
    // 1g. Forbid Island with Property Condition Tests
    //
    // Reproduces the bug where a forbid island connected to a matched node ignores
    // the property conditions on the forbid instance.
    //
    // Scenario (mirrors the user-reported bug):
    //   Match:  node : Node
    //   Forbid: node2 : Node { value == "thisValueNeverOccurs" }
    //   Forbid link: node.to -- node2.from
    //
    // Graph:
    //   nodeA (Node, value="old") --[to_from]--> nodeB (Node, value="old")
    //   nodeC (Node, value="old")   (no outgoing edge)
    //
    // The forbid condition can NEVER match because no Node has value=="thisValueNeverOccurs".
    // Therefore executeMatchAll should return 3 results (one per node).
    //
    // BUG: buildIslandChainTraversal builds the chain
    //   __.as(step_node).out("to_from").hasLabel("Node")
    // and wraps it in not(...). It never adds .has("value", "thisValueNeverOccurs").
    // This causes:
    //   - nodeA: not( nodeA.out("to_from").hasLabel("Node") ) = not(nodeB) = FALSE → excluded
    //   - nodeB,C: not(empty) = TRUE → included
    // Result: only 2 of 3 matches, not 3.
    // ========================================================================

    @Nested
    inner class ForbidIslandWithPropertyConditionTests {

        /**
         * 1g-FAILS: property condition on forbid island is currently ignored.
         *
         * The forbid instance has `value == "thisValueNeverOccurs"`.  No node in the
         * graph has that value, so the forbid should NEVER trigger.  All three Node
         * vertices must match.
         *
         * Bug: the generated chain is
         *   `__.as(step_node).out("to_from").hasLabel("Node")`
         * — the `.has("value","thisValueNeverOccurs")` step is missing.
         * For nodeA (which has an outgoing "to_from" edge to another Node) the
         * `not(...)` evaluates to `false`, incorrectly excluding it.
         */
        @Test
        fun `1g - forbid with property condition never matching allows all nodes to match`() {
            // Three Node vertices, all with value="old"
            val nodeA = graph.addVertex("Node")
            nodeA.property("value", "old")
            val nodeB = graph.addVertex("Node")
            nodeB.property("value", "old")
            val nodeC = graph.addVertex("Node")
            nodeC.property("value", "old")

            // nodeA has an outgoing "to_from" edge to nodeB
            nodeA.addEdge(EdgeLabelUtils.computeEdgeLabel("to", "from"), nodeB)

            val pattern = TypedPattern(
                elements = listOf(
                    matchInstance("node", "Node"),
                    forbidInstanceWithStringProperty("node2", "Node", "value", "thisValueNeverOccurs"),
                    forbidLink("node", "to", "node2", "from")
                )
            )

            val matchExecutor = MatchExecutor()
            val results = matchExecutor.executeMatchAll(pattern, context, engine)

            // All three nodes must match because the forbid condition never applies
            assertEquals(3, results.size,
                "Expected 3 matches (forbid condition never matches any node), but got ${results.size}. " +
                "This indicates the forbid island is not applying its property conditions.")
        }

        /**
         * 1g-PASSES: sanity check — when the forbid condition CAN match, the rule correctly
         * excludes the matched node.
         *
         * nodeA→nodeB edge exists and nodeB has value="forbidden". For nodeA the forbid
         * island (node2:Node{value=="forbidden"} linked from node.to) does match, so nodeA
         * should be excluded. nodeB and nodeC have no such outgoing edge, so they are included.
         * Expected: 2 matches (nodeB, nodeC).
         */
        @Test
        fun `1g - forbid with property condition that matches excludes the correct node`() {
            val nodeA = graph.addVertex("Node")
            nodeA.property("value", "old")
            val nodeB = graph.addVertex("Node")
            nodeB.property("value", "forbidden")  // <-- this value IS forbidden
            val nodeC = graph.addVertex("Node")
            nodeC.property("value", "old")

            nodeA.addEdge(EdgeLabelUtils.computeEdgeLabel("to", "from"), nodeB)

            val pattern = TypedPattern(
                elements = listOf(
                    matchInstance("node", "Node"),
                    forbidInstanceWithStringProperty("node2", "Node", "value", "forbidden"),
                    forbidLink("node", "to", "node2", "from")
                )
            )

            val matchExecutor = MatchExecutor()
            val results = matchExecutor.executeMatchAll(pattern, context, engine)

            // nodeA is excluded (it links to nodeB which has value=="forbidden")
            // nodeB and nodeC have no outgoing "to_from" edge, so their forbid chains are empty
            assertEquals(2, results.size,
                "Expected 2 matches (nodeA excluded because it links to a Node with value='forbidden').")
        }

        /**
         * 1g - non-static expression in forbid condition: all nodes must match.
         *
         * The forbid instance has `value == "thisValue" + "NeverOccurs"`.
         * The concatenated string ("thisValueNeverOccurs") never occurs on any vertex,
         * so the forbid should NEVER trigger and all 3 nodes must match.
         *
         * Previously the non-static TraversalResult branch was silently skipped, causing
         * the forbid to trigger on nodeA (which has an outgoing edge) even though the
         * target value never matched — incorrectly excluding nodeA.
         */
        @Test
        fun `1g - non-static expression in forbid condition never matches - all nodes must match`() {
            // Three Node vertices, all with value="old" (never equals "thisValue"+"NeverOccurs")
            val nodeA = graph.addVertex("Node")
            nodeA.property("value", "old")
            val nodeB = graph.addVertex("Node")
            nodeB.property("value", "old")
            val nodeC = graph.addVertex("Node")
            nodeC.property("value", "old")

            // nodeA has an outgoing "to_from" edge to nodeB
            nodeA.addEdge(EdgeLabelUtils.computeEdgeLabel("to", "from"), nodeB)

            // Build a local engine that has the string type registered at index 0
            // so that BinaryOperatorCompiler can resolve the evalType of the operands.
            val stringType = ClassTypeRef(`package` = "builtin", type = "string", isNullable = false)
            val localEngine = TransformationEngine(
                traversalSource = graph.traversal(),
                ast = TypedAst(
                    types = listOf(stringType),
                    metamodelPath = "test://model",
                    statements = emptyList()
                ),
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
            )

            val pattern = TypedPattern(
                elements = listOf(
                    matchInstance("node", "Node"),
                    forbidInstanceWithStringConcatExpression("node2", "Node", "value", "thisValue", "NeverOccurs"),
                    forbidLink("node", "to", "node2", "from")
                )
            )

            val matchExecutor = MatchExecutor()
            val results = matchExecutor.executeMatchAll(pattern, context, localEngine)

            // All three nodes must match: the forbid target value ("thisValueNeverOccurs")
            // never exists, so the forbid island never triggers.
            assertEquals(3, results.size,
                "Expected 3 matches because the forbid non-static expression never matches any node, " +
                "but got ${results.size}. This indicates the TraversalResult branch is still broken.")
        }
    }

    // ========================================================================
    // Helper factory methods for constructing pattern elements
    // ========================================================================

    companion object {

        /**
         * Creates a match (no modifier) object instance element.
         */
        fun matchInstance(name: String, className: String): TypedPatternObjectInstanceElement {
            return TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = null,
                    name = name,
                    className = className,
                    properties = emptyList()
                )
            )
        }

        /**
         * Creates a forbid object instance element.
         */
        fun forbidInstance(name: String, className: String): TypedPatternObjectInstanceElement {
            return TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = "forbid",
                    name = name,
                    className = className,
                    properties = emptyList()
                )
            )
        }

        /**
         * Creates a forbid object instance element with a single String equality condition.
         *
         * Produces an instance equivalent to:
         *   `forbid <name> : <className> { <propertyName> == "<propertyValue>" }`
         *
         * Used to reproduce bugs where the island chain traversal ignores property
         * conditions on forbid instances.
         */
        fun forbidInstanceWithStringProperty(
            name: String,
            className: String,
            propertyName: String,
            propertyValue: String
        ): TypedPatternObjectInstanceElement {
            return TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = "forbid",
                    name = name,
                    className = className,
                    properties = listOf(
                        TypedPatternPropertyAssignment(
                            propertyName = propertyName,
                            operator = "==",
                            value = TypedStringLiteralExpression(evalType = 0, value = propertyValue)
                        )
                    )
                )
            )
        }

        /**
         * Creates a forbid object instance element with a string-concatenation expression
         * as the equality condition.
         *
         * Produces an instance equivalent to:
         *   `forbid <name> : <className> { <propertyName> == <left> + <right> }`
         *
         * The expression is a non-static `TypedBinaryExpression` (operator "+"), so it
         * compiles to a `CompilationResult.TraversalResult` — exercising the code path
         * that was previously silently skipped.
         */
        fun forbidInstanceWithStringConcatExpression(
            name: String,
            className: String,
            propertyName: String,
            left: String,
            right: String
        ): TypedPatternObjectInstanceElement {
            return TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = "forbid",
                    name = name,
                    className = className,
                    properties = listOf(
                        TypedPatternPropertyAssignment(
                            propertyName = propertyName,
                            operator = "==",
                            value = TypedBinaryExpression(
                                evalType = 0,
                                operator = "+",
                                left = TypedStringLiteralExpression(evalType = 0, value = left),
                                right = TypedStringLiteralExpression(evalType = 0, value = right)
                            )
                        )
                    )
                )
            )
        }

        /**
         * Creates a require object instance element.
         */
        fun requireInstance(name: String, className: String): TypedPatternObjectInstanceElement {
            return TypedPatternObjectInstanceElement(
                objectInstance = TypedPatternObjectInstance(
                    modifier = "require",
                    name = name,
                    className = className,
                    properties = emptyList()
                )
            )
        }

        /**
         * Creates a match (no modifier) link element.
         */
        fun matchLink(
            sourceName: String,
            sourceProperty: String?,
            targetName: String,
            targetProperty: String?
        ): TypedPatternLinkElement {
            return TypedPatternLinkElement(
                link = TypedPatternLink(
                    modifier = null,
                    source = TypedPatternLinkEnd(objectName = sourceName, propertyName = sourceProperty),
                    target = TypedPatternLinkEnd(objectName = targetName, propertyName = targetProperty)
                )
            )
        }

        /**
         * Creates a forbid link element.
         */
        fun forbidLink(
            sourceName: String,
            sourceProperty: String?,
            targetName: String,
            targetProperty: String?
        ): TypedPatternLinkElement {
            return TypedPatternLinkElement(
                link = TypedPatternLink(
                    modifier = "forbid",
                    source = TypedPatternLinkEnd(objectName = sourceName, propertyName = sourceProperty),
                    target = TypedPatternLinkEnd(objectName = targetName, propertyName = targetProperty)
                )
            )
        }

        /**
         * Creates a require link element.
         */
        fun requireLink(
            sourceName: String,
            sourceProperty: String?,
            targetName: String,
            targetProperty: String?
        ): TypedPatternLinkElement {
            return TypedPatternLinkElement(
                link = TypedPatternLink(
                    modifier = "require",
                    source = TypedPatternLinkEnd(objectName = sourceName, propertyName = sourceProperty),
                    target = TypedPatternLinkEnd(objectName = targetName, propertyName = targetProperty)
                )
            )
        }
    }
}

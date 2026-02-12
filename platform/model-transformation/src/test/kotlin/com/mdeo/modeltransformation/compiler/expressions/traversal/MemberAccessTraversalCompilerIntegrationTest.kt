package com.mdeo.modeltransformation.compiler.expressions.traversal

import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
import com.mdeo.modeltransformation.compiler.VariableScope
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for MemberAccessCompiler with GremlinTypeRegistry integration.
 *
 * Tests the compilation of member access expressions using the type registry
 * to resolve properties and associations:
 * - Graph properties: compiled to `.values(propertyName)`
 * - Outgoing associations: compiled to `.out(edgeLabel)`
 * - Incoming associations: compiled to `.in(edgeLabel)`
 */
@DisplayName("MemberAccessCompiler Integration Tests")
class MemberAccessTraversalCompilerIntegrationTest {

    private lateinit var graph: TinkerGraph
    private lateinit var registry: ExpressionCompilerRegistry
    private lateinit var context: CompilationContext

    @BeforeEach
    fun setUp() {
        graph = TinkerGraph.open()
        registry = ExpressionCompilerRegistry.createDefaultRegistry()
        
        // Create type registry with metamodel types
        val typeRegistry = createTestTypeRegistry()
        
        context = CompilationContext(
            types = listOf(
                ClassTypeRef("Person", isNullable = false),
                ClassTypeRef("builtin.string", isNullable = false),
                ClassTypeRef("Address", isNullable = false),
                ClassTypeRef("builtin.int", isNullable = false)
            ),
            currentScope = VariableScope.empty(),
            traversalSource = graph.traversal(),
            typeRegistry = typeRegistry
        )
    }

    @AfterEach
    fun tearDown() {
        graph.close()
    }

    /**
     * Creates a type registry with test metamodel types.
     */
    private fun createTestTypeRegistry(): GremlinTypeRegistry {
        val parent = GremlinTypeRegistry.GLOBAL
        val registry = GremlinTypeRegistry(parent)
        
        // Define Person type with properties and associations
        val personType = gremlinType("Person")
            .graphProperty("name")
            .graphProperty("age")
            .association("address", "lives_at", isOutgoing = true, isNullable = false)
            .build()
        
        // Define Address type with properties and incoming association
        val addressType = gremlinType("Address")
            .graphProperty("city")
            .graphProperty("street")
            .association("residents", "lives_at", isOutgoing = false, isNullable = false)
            .build()
        
        registry.register(personType)
        registry.register(addressType)
        
        return registry
    }

    private fun identifier(name: String, evalType: Int = 0, scope: Int = 0) =
        TypedIdentifierExpression(evalType = evalType, name = name, scope = scope)

    private fun memberAccess(receiver: TypedIdentifierExpression, member: String, receiverEvalType: Int = 0, resultEvalType: Int = 1) =
        TypedMemberAccessExpression(
            evalType = resultEvalType,
            expression = receiver,
            member = member,
            isNullChaining = false
        )

    @Nested
    @DisplayName("Property Access with Type Registry")
    inner class PropertyAccessTests {

        @Test
        fun `accesses graph property from vertex`() {
            // Create test data
            val person = graph.addVertex(T.label, "Person", "name", "Alice", "age", 30)
            
            // Create context with person variable in scope
            val scope = VariableScope(scopeIndex = 0, bindings = mutableMapOf(
                "p" to VariableBinding.InstanceBinding(vertexId = null)
            ))
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            // Compile p.name using an initial traversal from the person vertex
            val personExpr = identifier("p", evalType = 0, scope = 0)
            val nameAccess = memberAccess(personExpr, "name", receiverEvalType = 0, resultEvalType = 1)
            
            // Use select() to start from the named step and access property
            val g = graph.traversal()
            val traversal = g.V(person).`as`(VariableBinding.stepLabel("p"))
            val compiled = registry.compile(nameAccess, contextWithScope, traversal)
            
            // Execute the traversal
            @Suppress("UNCHECKED_CAST")
            val result = (compiled.traversal as GraphTraversal<Any, String>).next()
            
            assertEquals("Alice", result)
        }

        @Test
        fun `accesses numeric property from vertex`() {
            // Create test data
            val person = graph.addVertex(T.label, "Person", "name", "Bob", "age", 25)
            
            // Create context with person variable in scope
            val scope = VariableScope(scopeIndex = 0, bindings = mutableMapOf(
                "p" to VariableBinding.InstanceBinding(vertexId = null)
            ))
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            // Compile p.age
            val personExpr = identifier("p", evalType = 0, scope = 0)
            val ageAccess = memberAccess(personExpr, "age", receiverEvalType = 0, resultEvalType = 3)
            
            val g = graph.traversal()
            val traversal = g.V(person).`as`(VariableBinding.stepLabel("p"))
            val compiled = registry.compile(ageAccess, contextWithScope, traversal)
            
            @Suppress("UNCHECKED_CAST")
            val result = (compiled.traversal as GraphTraversal<Any, Int>).next()
            
            assertEquals(25, result)
        }
    }

    @Nested
    @DisplayName("Association Traversal with Type Registry")
    inner class AssociationTraversalTests {

        @Test
        fun `traverses outgoing association`() {
            // Create test data: Person -[lives_at]-> Address
            val person = graph.addVertex(T.label, "Person", "name", "Charlie", "age", 35)
            val address = graph.addVertex(T.label, "Address", "city", "London", "street", "Baker St")
            person.addEdge("lives_at", address)
            
            // Create context with person variable in scope
            val scope = VariableScope(scopeIndex = 0, bindings = mutableMapOf(
                "p" to VariableBinding.InstanceBinding(vertexId = null)
            ))
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            // Compile p.address (should traverse via outgoing edge)
            val personExpr = identifier("p", evalType = 0, scope = 0)
            val addressAccess = memberAccess(personExpr, "address", receiverEvalType = 0, resultEvalType = 2)
            
            val g = graph.traversal()
            val traversal = g.V(person).`as`(VariableBinding.stepLabel("p"))
            val compiled = registry.compile(addressAccess, contextWithScope, traversal)
            
            @Suppress("UNCHECKED_CAST")
            val result = (compiled.traversal as GraphTraversal<Any, Any>).next()
            
            assertEquals(address, result)
        }

        @Test
        fun `traverses incoming association`() {
            // Create test data: Person -[lives_at]-> Address
            val person1 = graph.addVertex(T.label, "Person", "name", "Diana", "age", 28)
            val person2 = graph.addVertex(T.label, "Person", "name", "Edward", "age", 32)
            val address = graph.addVertex(T.label, "Address", "city", "Paris", "street", "Champs Elysees")
            person1.addEdge("lives_at", address)
            person2.addEdge("lives_at", address)
            
            // Create context with address variable in scope
            val addressType = ClassTypeRef("Address", isNullable = false)
            val contextWithAddressType = CompilationContext(
                types = listOf(addressType),
                currentScope = context.currentScope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            val scope = VariableScope(scopeIndex = 0, bindings = mutableMapOf(
                "a" to VariableBinding.InstanceBinding(vertexId = null)
            ))
            val contextWithScope = CompilationContext(
                types = contextWithAddressType.types,
                currentScope = scope,
                traversalSource = contextWithAddressType.traversalSource,
                typeRegistry = contextWithAddressType.typeRegistry
            )
            
            // Compile a.residents (should traverse via incoming edge)
            val addressExpr = identifier("a", evalType = 0, scope = 0)
            val residentsAccess = memberAccess(addressExpr, "residents", receiverEvalType = 0, resultEvalType = 0)
            
            val g = graph.traversal()
            val traversal = g.V(address).`as`(VariableBinding.stepLabel("a"))
            val compiled = registry.compile(residentsAccess, contextWithScope, traversal)
            
            @Suppress("UNCHECKED_CAST")
            val results = (compiled.traversal as GraphTraversal<Any, Any>).toList()
            
            assertTrue(results.size == 2)
            assertTrue(results.contains(person1))
            assertTrue(results.contains(person2))
        }
    }

    @Nested
    @DisplayName("Chained Member Access")
    inner class ChainedMemberAccessTests {

        @Test
        fun `chains association then property access`() {
            // Create test data: Person -[lives_at]-> Address
            val person = graph.addVertex(T.label, "Person", "name", "Frank", "age", 40)
            val address = graph.addVertex(T.label, "Address", "city", "Berlin", "street", "Unter den Linden")
            person.addEdge("lives_at", address)
            
            // Create context with person variable in scope
            val scope = VariableScope(scopeIndex = 0, bindings = mutableMapOf(
                "p" to VariableBinding.InstanceBinding(vertexId = null)
            ))
            val contextWithScope = CompilationContext(
                types = context.types,
                currentScope = scope,
                traversalSource = context.traversalSource,
                typeRegistry = context.typeRegistry
            )
            
            // Build p.address.city
            // First, compile p.address
            val personExpr = identifier("p", evalType = 0, scope = 0)
            val addressAccess = TypedMemberAccessExpression(
                evalType = 2, // Address type
                expression = personExpr,
                member = "address",
                isNullChaining = false
            )
            
            // Then, compile (p.address).city
            val cityAccess = TypedMemberAccessExpression(
                evalType = 1, // String type
                expression = addressAccess,
                member = "city",
                isNullChaining = false
            )
            
            val g = graph.traversal()
            val traversal = g.V(person).`as`(VariableBinding.stepLabel("p"))
            val compiled = registry.compile(cityAccess, contextWithScope, traversal)
            
            @Suppress("UNCHECKED_CAST")
            val result = (compiled.traversal as GraphTraversal<Any, String>).next()
            
            assertEquals("Berlin", result)
        }
    }
}

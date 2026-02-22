package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.expressions.*
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.patterns.*
import com.mdeo.modeltransformation.ast.statements.TypedMatchStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.VariableBinding
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for the instance name registry.
 * 
 * Demonstrates that the name property handling fix works correctly:
 * - Input model nodes with "name" properties are preserved
 * - Created nodes get registered with unique names
 * - The name registry properly tracks all nodes
 */
class InstanceNameRegistryIntegrationTest {
    
    private lateinit var graph: TinkerGraph
    private lateinit var engine: TransformationEngine
    
    @BeforeEach
    fun setup() {
        graph = TinkerGraph.open()
        val g = graph.traversal()
        
        val expressionRegistry = ExpressionCompilerRegistry.createDefaultRegistry()
        val statementRegistry = StatementExecutorRegistry.createDefaultRegistry()
        
        engine = TransformationEngine(
            traversalSource = g,
            ast = TypedAst(types = emptyList(), metamodelPath = "test://model", statements = emptyList()),
            expressionCompilerRegistry = expressionRegistry,
            statementExecutorRegistry = statementRegistry
        )
        
        // Set up the types array that would normally come from a TypedAst
        // Type 0: builtin.string (used in expressions)
        // Type 1: builtin.int (used in expressions)
        val stringType = ClassTypeRef(type = "builtin.string", isNullable = false)
        val intType = ClassTypeRef(type = "builtin.int", isNullable = false)
        
        // Use reflection to set the types field since it has a private setter
        val typesField = TransformationEngine::class.java.getDeclaredField("types")
        typesField.isAccessible = true
        typesField.set(engine, listOf(stringType, intType))
    }
    
    @Test
    fun `input model node with name property gets registered in name registry`() {
        val g = engine.traversalSource
        
        // Create an input model node with a "name" property
        val inputVertex = g.addV("Person")
            .property("name", "Alice")
            .property("age", 30)
            .next()
        
        // Register it in the name registry as if it came from the input model
        engine.instanceNameRegistry.register(inputVertex.id(), "person1")
        
        // Verify the registry has the mapping
        assertEquals("person1", engine.instanceNameRegistry.getName(inputVertex.id()))
        assertEquals(inputVertex.id(), engine.instanceNameRegistry.getVertexId("person1"))
        
        // Verify the "name" property is still in the graph
        val vertex = g.V(inputVertex.id()).next()
        assertEquals("Alice", vertex.property<String>("name").value())
    }
    
    @Test
    fun `created nodes with duplicate instance names get unique suffixes`() {
        val g = engine.traversalSource
        
        // Register vertex1 with name "house"
        val vertex1 = g.addV("House").next()
        val actualName1 = engine.instanceNameRegistry.registerWithUniqueName(vertex1.id(), "house")
        assertEquals("house", actualName1)
        
        // Register vertex2 with the same requested name "house"
        val vertex2 = g.addV("House").next()
        val actualName2 = engine.instanceNameRegistry.registerWithUniqueName(vertex2.id(), "house")
        assertEquals("house1", actualName2, "Second node should get suffix")
        
        // Register vertex3 with the same requested name "house"
        val vertex3 = g.addV("House").next()
        val actualName3 = engine.instanceNameRegistry.registerWithUniqueName(vertex3.id(), "house")
        assertEquals("house2", actualName3, "Third node should get next suffix")
        
        // Verify all mappings are correct
        assertEquals("house", engine.instanceNameRegistry.getName(vertex1.id()))
        assertEquals("house1", engine.instanceNameRegistry.getName(vertex2.id()))
        assertEquals("house2", engine.instanceNameRegistry.getName(vertex3.id()))
    }
    
    @Test
    fun `transformation creating nodes registers them in name registry`() {
        // Pattern that creates two Person nodes
        val statement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            name = "p1",
                            className = "Person",
                            modifier = "create",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "name",
                                    operator = "=",
                                    value = TypedStringLiteralExpression(
                                        evalType = 0,
                                        value = "Bob"
                                    )
                                )
                            )
                        )
                    ),
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            name = "p2",
                            className = "Person",
                            modifier = "create",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "name",
                                    operator = "=",
                                    value = TypedStringLiteralExpression(
                                        evalType = 0,
                                        value = "Charlie"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = engine.executeStatement(statement, context)
        
        assertTrue(result is TransformationExecutionResult.Success)
        result as TransformationExecutionResult.Success
        
        // Verify both nodes are in the name registry
        val p1Id = (context.variableScope.getVariable("p1") as? VariableBinding.InstanceBinding)?.vertexId
        val p2Id = (context.variableScope.getVariable("p2") as? VariableBinding.InstanceBinding)?.vertexId
        
        assertNotNull(p1Id)
        assertNotNull(p2Id)
        
        assertEquals("p1", engine.instanceNameRegistry.getName(p1Id))
        assertEquals("p2", engine.instanceNameRegistry.getName(p2Id))
        
        // Verify the "name" properties are preserved in the graph
        val g = engine.traversalSource
        val v1 = g.V(p1Id).next()
        val v2 = g.V(p2Id).next()
        
        assertEquals("Bob", v1.property<String>("name").value())
        assertEquals("Charlie", v2.property<String>("name").value())
    }
    
    @Test
    fun `name registry can handle both input and created nodes together`() {
        val g = engine.traversalSource
        
        // Create input model node with "name" property
        val inputVertex = g.addV("Person")
            .property("name", "Diana")
            .property("age", 28)
            .next()
        
        // Register input node
        engine.instanceNameRegistry.register(inputVertex.id(), "existingPerson")
        
        // Pattern that creates a new Person node
        val statement = TypedMatchStatement(
            pattern = TypedPattern(
                elements = listOf(
                    TypedPatternObjectInstanceElement(
                        objectInstance = TypedPatternObjectInstance(
                            name = "newPerson",
                            className = "Person",
                            modifier = "create",
                            properties = listOf(
                                TypedPatternPropertyAssignment(
                                    propertyName = "name",
                                    operator = "=",
                                    value = TypedStringLiteralExpression(
                                        evalType = 0,
                                        value = "Edward"
                                    )
                                ),
                                TypedPatternPropertyAssignment(
                                    propertyName = "age",
                                    operator = "=",
                                    value = TypedIntLiteralExpression(
                                        evalType = 0,
                                        value = "32"
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val context = TransformationExecutionContext.empty()
        val result = engine.executeStatement(statement, context)
        
        assertTrue(result is TransformationExecutionResult.Success)
        result as TransformationExecutionResult.Success
        
        // Verify both nodes are in the registry
        assertEquals("existingPerson", engine.instanceNameRegistry.getName(inputVertex.id()))
        
        val newPersonId = (context.variableScope.getVariable("newPerson") as? VariableBinding.InstanceBinding)?.vertexId
        assertNotNull(newPersonId)
        assertEquals("newPerson", engine.instanceNameRegistry.getName(newPersonId))
        
        // Verify both have their "name" properties
        val inputV = g.V(inputVertex.id()).next()
        val newV = g.V(newPersonId).next()
        
        assertEquals("Diana", inputV.property<String>("name").value())
        assertEquals("Edward", newV.property<String>("name").value())
    }
}

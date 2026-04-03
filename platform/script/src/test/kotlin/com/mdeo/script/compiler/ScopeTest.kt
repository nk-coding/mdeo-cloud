package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the Scope data structure.
 */
class ScopeTest {
    private fun type(name: String): ReturnType = ClassTypeRef("builtin", "$name", false)
    

    
    @Test
    fun `create root scope with level 0`() {
        val scope = Scope(level = 0)
        assertEquals(0, scope.level)
        assertNull(scope.parent)
        assertTrue(scope.children.isEmpty())
    }
    
    @Test
    fun `create child scope increments level`() {
        val parent = Scope(level = 2)
        val child = parent.createChild()
        
        assertEquals(3, child.level)
        assertEquals(parent, child.parent)
        assertTrue(parent.children.contains(child))
    }
    
    @Test
    fun `declare variable in scope`() {
        val scope = Scope(level = 3)
        val variable = scope.declareVariable("x", type("int"))
        
        assertEquals("x", variable.name)
        // Type is now ReturnType, not int
        assertTrue(scope.declaredVariables.containsKey("x"))
    }
    
    @Test
    fun `lookup variable in current scope`() {
        val scope = Scope(level = 3)
        scope.declareVariable("x", type("int"))
        
        val found = scope.lookupVariable("x", 3)
        assertNotNull(found)
        assertEquals("x", found.name)
    }
    
    @Test
    fun `lookup variable in parent scope`() {
        val parent = Scope(level = 3)
        parent.declareVariable("x", type("int"))
        
        val child = parent.createChild()
        
        val found = child.lookupVariable("x", 3)
        assertNotNull(found)
        assertEquals("x", found.name)
    }
    
    @Test
    fun `lookup variable returns null when not found`() {
        val scope = Scope(level = 3)
        scope.declareVariable("x", type("int"))
        
        val found = scope.lookupVariable("y", 3)
        assertNull(found)
    }
    
    @Test
    fun `lookup variable by name and scope level`() {
        val parent = Scope(level = 3)
        parent.declareVariable("x", type("int"))
        
        val child = parent.createChild()
        child.declareVariable("x", type("int"))
        
        val parentVar = child.lookupVariable("x", 3)
        assertNotNull(parentVar)
        // Type is now ReturnType, not int
        
        val childVar = child.lookupVariable("x", 4)
        assertNotNull(childVar)
        // Type is now ReturnType, not int
    }
    
    @Test
    fun `record write in scope`() {
        val scope = Scope(level = 3)
        scope.recordWrite("x", 3)
        
        assertTrue(scope.writtenVariables.contains("x" to 3))
    }
    
    @Test
    fun `record read in scope`() {
        val scope = Scope(level = 3)
        scope.recordRead("x", 2)
        
        assertTrue(scope.readVariables.contains("x" to 2))
    }
    
    @Test
    fun `record multiple reads in scope`() {
        val scope = Scope(level = 4)
        scope.recordRead("x", 2)
        scope.recordRead("y", 3)
        scope.recordRead("x", 2)
        
        assertEquals(2, scope.readVariables.size)
        assertTrue(scope.readVariables.contains("x" to 2))
        assertTrue(scope.readVariables.contains("y" to 3))
    }
    
    @Test
    fun `read variables are scope-local only`() {
        val parent = Scope(level = 3)
        parent.recordRead("x", 2)
        
        val child = parent.createChild()
        child.recordRead("y", 2)
        
        assertEquals(1, parent.readVariables.size)
        assertTrue(parent.readVariables.contains("x" to 2))
        
        assertEquals(1, child.readVariables.size)
        assertTrue(child.readVariables.contains("y" to 2))
    }
    
    @Test
    fun `statically nested scope by default`() {
        val scope = Scope(level = 3)
        assertTrue(scope.isStaticallyNested)
    }
    
    @Test
    fun `lambda scope is a LambdaScope`() {
        val parent = Scope(level = 3)
        val lambdaScope = LambdaScope(parent)
        
        assertFalse(lambdaScope.isStaticallyNested)
        assertEquals(4, lambdaScope.level)
    }
    
    @Test
    fun `hasLambdaScopeBetween returns false for static scopes`() {
        val level3 = Scope(level = 3)
        val level4 = level3.createChild()
        val level5 = level4.createChild()
        
        assertFalse(level5.hasLambdaScopeBetween(3))
    }
    
    @Test
    fun `hasLambdaScopeBetween returns true when lambda scope exists`() {
        val level3 = Scope(level = 3)
        val lambdaScope = LambdaScope(level3)
        val level5 = lambdaScope.createChild()
        
        assertTrue(level5.hasLambdaScopeBetween(3))
    }
    
    @Test
    fun `multiple children can be created`() {
        val parent = Scope(level = 3)
        val child1 = parent.createChild()
        val child2 = parent.createChild()
        val child3 = parent.createChild()
        
        assertEquals(3, parent.children.size)
        assertTrue(parent.children.containsAll(listOf(child1, child2, child3)))
    }
    
    @Test
    fun `scope tree navigation works correctly`() {
        val level2 = Scope(level = 2)
        level2.declareVariable("param", type("int"))
        
        val level3 = level2.createChild()
        level3.declareVariable("local", type("int"))
        
        val level4 = level3.createChild()
        level4.declareVariable("inner", type("int"))
        
        val fromLevel4 = level4.lookupVariable("param", 2)
        assertNotNull(fromLevel4)
        assertEquals("param", fromLevel4.name)
        
        val localFromLevel4 = level4.lookupVariable("local", 3)
        assertNotNull(localFromLevel4)
        
        val innerFromLevel3 = level3.lookupVariable("inner", 4)
        assertNull(innerFromLevel3)
    }

    @Test
    fun `collectCapturedVariables returns empty when no reads from outer scope`() {
        val functionScope = Scope(level = 2)
        val lambdaParamsScope = LambdaScope(functionScope)
        val lambdaBodyScope = lambdaParamsScope.createChild()
        
        lambdaBodyScope.recordRead("localVar", 4)
        
        val captured = lambdaBodyScope.collectCapturedVariables(lambdaParamsLevel = 3)
        assertTrue(captured.isEmpty())
    }
    
    @Test
    fun `collectCapturedVariables returns variables from outer scope`() {
        val functionScope = Scope(level = 2)
        functionScope.declareVariable("outerVar", type("int"))
        
        val lambdaParamsScope = LambdaScope(functionScope)
        val lambdaBodyScope = lambdaParamsScope.createChild()
        
        lambdaBodyScope.recordRead("outerVar", 2)
        
        val captured = lambdaBodyScope.collectCapturedVariables(lambdaParamsLevel = 3)
        assertEquals(1, captured.size)
        assertTrue(captured.contains("outerVar" to 2))
    }
    
    @Test
    fun `collectCapturedVariables includes written variables from outer scope`() {
        val functionScope = Scope(level = 2)
        functionScope.declareVariable("counter", type("int"))
        
        val lambdaParamsScope = LambdaScope(functionScope)
        val lambdaBodyScope = lambdaParamsScope.createChild()
        
        lambdaBodyScope.recordWrite("counter", 2)
        
        val captured = lambdaBodyScope.collectCapturedVariables(lambdaParamsLevel = 3)
        assertEquals(1, captured.size)
        assertTrue(captured.contains("counter" to 2))
    }
    
    @Test
    fun `collectCapturedVariables collects from nested scopes`() {
        val functionScope = Scope(level = 2)
        functionScope.declareVariable("x", type("int"))
        functionScope.declareVariable("y", type("int"))
        
        val lambdaParamsScope = LambdaScope(functionScope)
        val lambdaBodyScope = lambdaParamsScope.createChild()
        val nestedWhileScope = lambdaBodyScope.createChild()
        
        lambdaBodyScope.recordRead("x", 2)
        nestedWhileScope.recordRead("y", 2)
        
        val captured = lambdaBodyScope.collectCapturedVariables(lambdaParamsLevel = 3)
        assertEquals(2, captured.size)
        assertTrue(captured.contains("x" to 2))
        assertTrue(captured.contains("y" to 2))
    }
    
    @Test
    fun `collectCapturedVariables handles nested lambdas`() {
        val functionScope = Scope(level = 2)
        functionScope.declareVariable("outerVar", type("int"))
        
        val lambda1ParamsScope = LambdaScope(functionScope)
        val lambda1BodyScope = lambda1ParamsScope.createChild()
        
        /* Nested lambda inside lambda1 */
        val lambda2ParamsScope = LambdaScope(lambda1BodyScope)
        val lambda2BodyScope = lambda2ParamsScope.createChild()
        
        lambda2BodyScope.recordRead("outerVar", 2)
        
        /*
         * Lambda1 needs to capture outerVar because lambda2 uses it.
         * The captured variables should include outerVar transitively.
         */
        val captured = lambda1BodyScope.collectCapturedVariables(lambdaParamsLevel = 3)
        assertEquals(1, captured.size)
        assertTrue(captured.contains("outerVar" to 2))
    }
    
    @Test
    fun `findNearestLambdaScope returns lambda scope`() {
        val functionScope = Scope(level = 2)
        val lambdaScope = LambdaScope(functionScope)
        val bodyScope = lambdaScope.createChild()
        
        assertEquals(lambdaScope, bodyScope.findNearestLambdaScope())
    }
    
    @Test
    fun `findNearestLambdaScope returns null for static scopes`() {
        val functionScope = Scope(level = 2)
        val bodyScope = functionScope.createChild()
        
        assertNull(bodyScope.findNearestLambdaScope())
    }
}

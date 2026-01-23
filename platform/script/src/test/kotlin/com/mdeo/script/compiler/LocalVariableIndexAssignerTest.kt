package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for the LocalVariableIndexAssigner.
 */
class LocalVariableIndexAssignerTest {
    
    private fun createContext(vararg types: String): CompilationContext {
        val typeList = types.map { 
            when (it) {
                "int" -> ClassTypeRef("builtin.int", false)
                "long" -> ClassTypeRef("builtin.long", false)
                "float" -> ClassTypeRef("builtin.float", false)
                "double" -> ClassTypeRef("builtin.double", false)
                "boolean" -> ClassTypeRef("builtin.boolean", false)
                "string" -> ClassTypeRef("builtin.string", false)
                else -> ClassTypeRef("builtin.any", true)
            }
        }
        val ast = TypedAst(types = typeList, imports = emptyList(), functions = emptyList())
        return CompilationContext(ast, "TestClass", emptyList(), emptyList())
    }
    
    private fun type(name: String): ReturnType = when (name) {
        "int" -> ClassTypeRef("builtin.int", false)
        "long" -> ClassTypeRef("builtin.long", false)
        "float" -> ClassTypeRef("builtin.float", false)
        "double" -> ClassTypeRef("builtin.double", false)
        "boolean" -> ClassTypeRef("builtin.boolean", false)
        "string" -> ClassTypeRef("builtin.string", false)
        else -> ClassTypeRef("builtin.any", true)
    }

    
    @Test
    fun `int variable gets slot 0 in static method`() {
        val context = createContext("int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("x", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["x"]!!.slotIndex)
    }
    
    @Test
    fun `multiple int variables get sequential slots`() {
        val context = createContext("int", "int", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("a", type("int"))
        bodyScope.declareVariable("b", type("int"))
        bodyScope.declareVariable("c", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["a"]!!.slotIndex)
        assertEquals(1, bodyScope.declaredVariables["b"]!!.slotIndex)
        assertEquals(2, bodyScope.declaredVariables["c"]!!.slotIndex)
    }
    
    @Test
    fun `long variable takes 2 slots`() {
        val context = createContext("long", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("l", type("long"))
        bodyScope.declareVariable("i", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["l"]!!.slotIndex)
        assertEquals(2, bodyScope.declaredVariables["i"]!!.slotIndex)
    }
    
    @Test
    fun `double variable takes 2 slots`() {
        val context = createContext("double", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("d", type("double"))
        bodyScope.declareVariable("i", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["d"]!!.slotIndex)
        assertEquals(2, bodyScope.declaredVariables["i"]!!.slotIndex)
    }
    
    @Test
    fun `mixed types get correct slots`() {
        val context = createContext("int", "long", "float", "double", "boolean")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("i", type("int"))
        bodyScope.declareVariable("l", type("long"))
        bodyScope.declareVariable("f", type("float"))
        bodyScope.declareVariable("d", type("double"))
        bodyScope.declareVariable("b", type("boolean"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["i"]!!.slotIndex)
        assertEquals(1, bodyScope.declaredVariables["l"]!!.slotIndex)
        assertEquals(3, bodyScope.declaredVariables["f"]!!.slotIndex)
        assertEquals(4, bodyScope.declaredVariables["d"]!!.slotIndex)
        assertEquals(6, bodyScope.declaredVariables["b"]!!.slotIndex)
    }
    
    @Test
    fun `nested scope variables continue from parent`() {
        val context = createContext("int", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("outer", type("int"))
        
        val nestedScope = bodyScope.createChild()
        nestedScope.declareVariable("inner", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["outer"]!!.slotIndex)
        assertEquals(1, nestedScope.declaredVariables["inner"]!!.slotIndex)
    }
    
    @Test
    fun `sibling scopes can reuse slots`() {
        val context = createContext("int", "int", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("shared", type("int"))
        
        val scope1 = bodyScope.createChild()
        scope1.declareVariable("a", type("int"))
        
        val scope2 = bodyScope.createChild()
        scope2.declareVariable("b", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["shared"]!!.slotIndex)
        assertEquals(1, scope1.declaredVariables["a"]!!.slotIndex)
        assertEquals(1, scope2.declaredVariables["b"]!!.slotIndex)
    }
    
    @Test
    fun `wrapped variable takes 1 slot regardless of type`() {
        val context = createContext("long")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        val variable = bodyScope.declareVariable("l", type("long"))
        variable.isWrittenByLambda = true
        
        bodyScope.declareVariable("after", type("long"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["l"]!!.slotIndex)
        assertEquals(1, bodyScope.declaredVariables["after"]!!.slotIndex)
    }
    
    @Test
    fun `function parameters get slots before body variables`() {
        val context = createContext("int", "int", "int")
        val paramsScope = Scope(level = 2)
        paramsScope.declareVariable("param1", type("int"))
        paramsScope.declareVariable("param2", type("int"))
        
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("local", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, paramsScope.declaredVariables["param1"]!!.slotIndex)
        assertEquals(1, paramsScope.declaredVariables["param2"]!!.slotIndex)
        assertEquals(2, bodyScope.declaredVariables["local"]!!.slotIndex)
    }
    
    @Test
    fun `non-static method reserves slot 0 for this`() {
        val context = createContext("int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("x", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = false)
        
        assertEquals(1, bodyScope.declaredVariables["x"]!!.slotIndex)
    }
    
    @Test
    fun `deeply nested scopes work correctly`() {
        val context = createContext("int", "int", "int", "int", "int")
        val paramsScope = Scope(level = 2)
        val level3 = paramsScope.createChild()
        level3.declareVariable("a", type("int"))
        
        val level4 = level3.createChild()
        level4.declareVariable("b", type("int"))
        
        val level5 = level4.createChild()
        level5.declareVariable("c", type("int"))
        
        val level6 = level5.createChild()
        level6.declareVariable("d", type("int"))
        
        val level7 = level6.createChild()
        level7.declareVariable("e", type("int"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, level3.declaredVariables["a"]!!.slotIndex)
        assertEquals(1, level4.declaredVariables["b"]!!.slotIndex)
        assertEquals(2, level5.declaredVariables["c"]!!.slotIndex)
        assertEquals(3, level6.declaredVariables["d"]!!.slotIndex)
        assertEquals(4, level7.declaredVariables["e"]!!.slotIndex)
    }
    
    @Test
    fun `complex slot reuse with different types in siblings`() {
        val context = createContext("long", "int", "double", "float")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("base", type("long"))
        
        val scope1 = bodyScope.createChild()
        scope1.declareVariable("temp", type("int"))
        
        val scope2 = bodyScope.createChild()
        scope2.declareVariable("other", type("double"))
        scope2.declareVariable("more", type("float"))
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        assertEquals(0, bodyScope.declaredVariables["base"]!!.slotIndex)
        assertEquals(2, scope1.declaredVariables["temp"]!!.slotIndex)
        assertEquals(2, scope2.declaredVariables["other"]!!.slotIndex)
        assertEquals(4, scope2.declaredVariables["more"]!!.slotIndex)
    }
    
    @Test
    fun `getMaxLocals returns correct value after sibling scope reuse`() {
        // This test verifies that getMaxLocals() tracks the high-water mark correctly
        // when sibling scopes reuse slots. The max locals should be the highest slot
        // ever used, not just the current nextSlot after processing.
        //
        // Structure:
        // - bodyScope: base (long, slots 0-1)
        // - scope1: temp1 (int, slot 2), temp2 (long, slots 3-4) - uses up to slot 4
        // - scope2: other (int, slot 2) - reuses slot 2
        // 
        // After processing, nextSlot is reset to 2 (after bodyScope's variables end at slot 2).
        // But the max locals should be 5 (slots 0,1,2,3,4 = 5 slots needed).
        val context = createContext("long", "int", "long", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("base", type("long"))  // long takes slots 0-1
        
        val scope1 = bodyScope.createChild()
        scope1.declareVariable("temp1", type("int"))    // int takes slot 2
        scope1.declareVariable("temp2", type("long"))    // long takes slots 3-4
        
        val scope2 = bodyScope.createChild()
        scope2.declareVariable("other", type("int"))    // int reuses slot 2
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        // Verify slots are assigned correctly
        assertEquals(0, bodyScope.declaredVariables["base"]!!.slotIndex)
        assertEquals(2, scope1.declaredVariables["temp1"]!!.slotIndex)
        assertEquals(3, scope1.declaredVariables["temp2"]!!.slotIndex)
        assertEquals(2, scope2.declaredVariables["other"]!!.slotIndex)
        
        // getMaxLocals should return 5 (slots 0,1,2,3,4), not 2 (current nextSlot)
        assertEquals(5, assigner.getMaxLocals())
    }
    
    @Test
    fun `getMaxLocals with deeply nested sibling scopes`() {
        // Tests that getMaxLocals tracks max across all branches, not just the last one
        val context = createContext("int", "int", "int", "int", "int")
        val paramsScope = Scope(level = 2)
        val bodyScope = paramsScope.createChild()
        bodyScope.declareVariable("x", type("int"))  // slot 0
        
        // First branch: uses slots 1, 2, 3
        val branch1 = bodyScope.createChild()
        branch1.declareVariable("a1", type("int"))  // slot 1
        val nested1 = branch1.createChild()
        nested1.declareVariable("b1", type("int"))  // slot 2
        val deep1 = nested1.createChild()
        deep1.declareVariable("c1", type("int"))    // slot 3
        
        // Second branch: only uses slot 1
        val branch2 = bodyScope.createChild()
        branch2.declareVariable("a2", type("int"))  // slot 1 (reused)
        
        val assigner = LocalVariableIndexAssigner(context)
        assigner.assignIndices(paramsScope, isStatic = true)
        
        // Max locals should be 4 (slots 0,1,2,3), not 2
        assertEquals(4, assigner.getMaxLocals())
    }
}

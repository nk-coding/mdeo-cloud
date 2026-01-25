package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.statements.TypedStatement
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the CompilationContext class.
 */
class CompilationContextTest {
    
    @Test
    fun `getType returns correct type for index`() {
        val ast = buildTypedAst {
            intType()
            stringType()
            booleanType()
        }
        
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        assertNotNull(context.getType(0))
        assertNotNull(context.getType(1))
        assertNotNull(context.getType(2))
    }
    
    @Test
    fun `isVoid returns true for void type`() {
        val ast = buildTypedAst {
            voidType()
            intType()
        }
        
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        assert(context.isVoid(0))
        assert(!context.isVoid(1))
    }
    
    @Test
    fun `getTypeDescriptor returns correct descriptors for primitive types`() {
        val ast = buildTypedAst {
            intType()       // 0
            longType()      // 1
            floatType()     // 2
            doubleType()    // 3
            booleanType()   // 4
        }
        
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        assertEquals("I", context.getTypeDescriptor(ast.types[0]))
        assertEquals("J", context.getTypeDescriptor(ast.types[1]))
        assertEquals("F", context.getTypeDescriptor(ast.types[2]))
        assertEquals("D", context.getTypeDescriptor(ast.types[3]))
        assertEquals("Z", context.getTypeDescriptor(ast.types[4]))
    }
    
    @Test
    fun `getTypeDescriptor returns correct descriptors for nullable types`() {
        val ast = buildTypedAst {
            intNullableType()       // 0
            longNullableType()      // 1
            floatNullableType()     // 2
            doubleNullableType()    // 3
            booleanNullableType()   // 4
        }
        
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        assertEquals("Ljava/lang/Integer;", context.getTypeDescriptor(ast.types[0]))
        assertEquals("Ljava/lang/Long;", context.getTypeDescriptor(ast.types[1]))
        assertEquals("Ljava/lang/Float;", context.getTypeDescriptor(ast.types[2]))
        assertEquals("Ljava/lang/Double;", context.getTypeDescriptor(ast.types[3]))
        assertEquals("Ljava/lang/Boolean;", context.getTypeDescriptor(ast.types[4]))
    }
    
    @Test
    fun `getTypeDescriptor returns correct descriptor for string type`() {
        val ast = buildTypedAst {
            stringType()
            stringNullableType()
        }
        
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        assertEquals("Ljava/lang/String;", context.getTypeDescriptor(ast.types[0]))
        assertEquals("Ljava/lang/String;", context.getTypeDescriptor(ast.types[1]))
    }
    
    @Test
    fun `getTypeDescriptor returns V for void type`() {
        val ast = buildTypedAst {
            voidType()
        }
        
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        assertEquals("V", context.getTypeDescriptor(ast.types[0]))
    }
    
    @Test
    fun `compileExpression throws for unsupported expression`() {
        val ast = buildTypedAst { intType() }
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        val unsupportedExpression = object : TypedExpression {
            override val kind = TypedExpressionKind.Binary
            override val evalType = 0
        }
        
        assertThrows<CompilationException> {
            context.compileExpression(unsupportedExpression, DummyMethodVisitor(), ast.types[0])
        }
    }
    
    @Test
    fun `compileStatement throws for unsupported statement`() {
        val ast = buildTypedAst { voidType() }
        val context = CompilationContext(ast, "test/Test", emptyList(), emptyList())
        
        val unsupportedStatement = object : TypedStatement {
            override val kind = TypedStatementKind.While
        }
        
        assertThrows<CompilationException> {
            context.compileStatement(unsupportedStatement, DummyMethodVisitor())
        }
    }
}

/**
 * Dummy MethodVisitor for testing.
 */
private class DummyMethodVisitor : MethodVisitor(Opcodes.ASM9)

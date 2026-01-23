package com.mdeo.script.compiler

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for nullable primitive types and boxing behavior.
 * When a nullable primitive type has a non-null value, it should be boxed.
 * 
 * BUG FOUND: The compiler does not box primitive values when the return type
 * is a nullable primitive type (e.g., Int?, Long?, Float?, Double?, Boolean?).
 * 
 * The fix requires modifications to either:
 * 1. The literal compilers to check if the target type is nullable and emit boxing code
 * 2. The return statement compiler to box the value before ARETURN
 * 
 * Boxing is done by calling static valueOf methods:
 * - Integer.valueOf(int) for Int?
 * - Long.valueOf(long) for Long?
 * - Float.valueOf(float) for Float?
 * - Double.valueOf(double) for Double?
 * - Boolean.valueOf(boolean) for Boolean?
 */
class NullableBoxingTest {
    
    private val helper = CompilerTestHelper()
    
    /**
     * BUG: When returning a non-null value from a nullable primitive type function,
     * the value needs to be boxed. Currently, the compiler just pushes the primitive
     * value and uses ARETURN, which causes a VerifyError.
     * 
     * Error message: "Bad type on operand stack - Type integer is not assignable to reference type"
     * 
     * For example: A function returning Int? with value 42 should:
     * 1. Push 42 using BIPUSH/ICONST
     * 2. Box it using INVOKESTATIC java/lang/Integer.valueOf(I)Ljava/lang/Integer;
     * 3. Use ARETURN
     * 
     * But currently it just does BIPUSH 42 followed by ARETURN, which is invalid.
     */
    @Test
    fun `return non-null int from nullable int function requires boxing - BUG missing boxing`() {
        val ast = buildTypedAst {
            val nullableIntType = intNullableType()
            function(
                name = "testFunction",
                returnType = nullableIntType,
                body = listOf(returnStmt(intLiteral(42, nullableIntType)))
            )
        }
        
        // BUG: This will likely fail with VerifyError because:
        // - intLiteral pushes an int (primitive) on the stack
        // - But return type is Integer (object) so ARETURN is used
        // - JVM verifier will reject this mismatch
        val result = helper.compileAndInvoke(ast)
        assertEquals(42, result)
    }
    
    @Test
    fun `return non-null long from nullable long function requires boxing - BUG missing boxing`() {
        val ast = buildTypedAst {
            val nullableLongType = longNullableType()
            function(
                name = "testFunction",
                returnType = nullableLongType,
                body = listOf(returnStmt(longLiteral(123456789012L, nullableLongType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(123456789012L, result)
    }
    
    @Test
    fun `return non-null float from nullable float function requires boxing - BUG missing boxing`() {
        val ast = buildTypedAst {
            val nullableFloatType = floatNullableType()
            function(
                name = "testFunction",
                returnType = nullableFloatType,
                body = listOf(returnStmt(floatLiteral(3.14f, nullableFloatType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(3.14f, result)
    }
    
    @Test
    fun `return non-null double from nullable double function requires boxing - BUG missing boxing`() {
        val ast = buildTypedAst {
            val nullableDoubleType = doubleNullableType()
            function(
                name = "testFunction",
                returnType = nullableDoubleType,
                body = listOf(returnStmt(doubleLiteral(2.71828, nullableDoubleType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(2.71828, result)
    }
    
    @Test
    fun `return non-null boolean from nullable boolean function requires boxing - BUG missing boxing`() {
        val ast = buildTypedAst {
            val nullableBoolType = booleanNullableType()
            function(
                name = "testFunction",
                returnType = nullableBoolType,
                body = listOf(returnStmt(booleanLiteral(true, nullableBoolType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertEquals(true, result)
    }
    
    // These should still work - null for nullable types
    @Test
    fun `return null from nullable int function works`() {
        val ast = buildTypedAst {
            val nullableIntType = intNullableType()
            function(
                name = "testFunction",
                returnType = nullableIntType,
                body = listOf(returnStmt(nullLiteral(nullableIntType)))
            )
        }
        
        val result = helper.compileAndInvoke(ast)
        assertNull(result)
    }
}

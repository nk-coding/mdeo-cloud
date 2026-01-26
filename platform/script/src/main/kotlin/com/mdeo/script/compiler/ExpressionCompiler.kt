package com.mdeo.script.compiler

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.util.CoercionUtil
import org.objectweb.asm.MethodVisitor

/**
 * Abstract base class for compiling expressions to bytecode.
 * Implementations handle specific expression types and generate
 * the appropriate bytecode instructions.
 * 
 * Each expression compiler is responsible for leaving the result
 * of the expression on top of the operand stack, coerced to the
 * expected type.
 * 
 * Coercion is centralized in the [compile] method, which calls
 * [compileInternal] to generate the expression's natural result,
 * then applies type coercion to match the expected type.
 */
abstract class ExpressionCompiler {
    /**
     * Checks if this compiler can handle the given expression.
     * 
     * @param expression The expression to check.
     * @return true if this compiler can compile the expression.
     */
    abstract fun canCompile(expression: TypedExpression): Boolean
    
    /**
     * Compiles the expression to bytecode, coercing the result to the expected type.
     * 
     * This method calls [compileInternal] to generate the expression's natural result,
     * then applies type coercion using [CoercionUtil] to match the expected type.
     * 
     * Subclasses may override this method to provide custom coercion behavior,
     * but should typically only override [compileInternal].
     * 
     * @param expression The expression to compile.
     * @param context The compilation context with type information and utilities.
     * @param mv The method visitor to emit bytecode to.
     * @param expectedType The type the expression result should be coerced to.
     */
    open fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        expectedType: ReturnType
    ) {
        compileInternal(expression, context, mv)
        val actualType = context.getType(expression.evalType)
        CoercionUtil.emitCoercion(actualType, expectedType, mv, context)
    }
    
    /**
     * Compiles the expression to bytecode, leaving its natural result on the stack.
     * 
     * This is the main method that subclasses must implement. The result should be
     * the expression's natural type (as indicated by expression.evalType), without
     * any coercion applied.
     * 
     * @param expression The expression to compile.
     * @param context The compilation context with type information and utilities.
     * @param mv The method visitor to emit bytecode to.
     */
    protected abstract fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor)
}

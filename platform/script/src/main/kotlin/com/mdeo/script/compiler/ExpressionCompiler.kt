package com.mdeo.script.compiler

import com.mdeo.script.ast.expressions.TypedExpression
import org.objectweb.asm.MethodVisitor

/**
 * Interface for compiling expressions to bytecode.
 * Implementations handle specific expression types and generate
 * the appropriate bytecode instructions.
 * 
 * Each expression compiler is responsible for leaving the result
 * of the expression on top of the operand stack.
 */
interface ExpressionCompiler {
    /**
     * Checks if this compiler can handle the given expression.
     * 
     * @param expression The expression to check.
     * @return true if this compiler can compile the expression.
     */
    fun canCompile(expression: TypedExpression): Boolean
    
    /**
     * Compiles the expression to bytecode.
     * After this method returns, the result of the expression
     * should be on top of the operand stack.
     * 
     * @param expression The expression to compile.
     * @param context The compilation context with type information and utilities.
     * @param mv The method visitor to emit bytecode to.
     */
    fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor)
}

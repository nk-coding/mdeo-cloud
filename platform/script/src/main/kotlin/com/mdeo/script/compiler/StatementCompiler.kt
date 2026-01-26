package com.mdeo.script.compiler

import com.mdeo.expression.ast.statements.TypedStatement
import org.objectweb.asm.MethodVisitor

/**
 * Interface for compiling statements to bytecode.
 * Implementations handle specific statement types and generate
 * the appropriate bytecode instructions.
 */
interface StatementCompiler {
    /**
     * Checks if this compiler can handle the given statement.
     * 
     * @param statement The statement to check.
     * @return true if this compiler can compile the statement.
     */
    fun canCompile(statement: TypedStatement): Boolean
    
    /**
     * Compiles the statement to bytecode.
     * 
     * @param statement The statement to compile.
     * @param context The compilation context with type information and utilities.
     * @param mv The method visitor to emit bytecode to.
     * @return Unit. Bytecode is emitted to the method visitor as a side effect.
     */
    fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor)
}

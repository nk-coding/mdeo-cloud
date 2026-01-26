package com.mdeo.script.compiler.statements

import com.mdeo.expression.ast.statements.TypedBreakStatement
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.CompilationException
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles break statements to bytecode.
 * 
 * A break statement exits the innermost enclosing loop.
 * It generates a GOTO instruction to the break label of the current loop.
 */
class BreakStatementCompiler : StatementCompiler {
    
    /**
     * Checks if this compiler can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a [TypedBreakStatement], false otherwise.
     */
    override fun canCompile(statement: TypedStatement): Boolean {
        return statement is TypedBreakStatement
    }
    
    /**
     * Compiles a break statement to bytecode.
     *
     * Retrieves the current loop's labels from the context and generates a GOTO
     * instruction to jump to the break label, exiting the innermost loop.
     *
     * @param statement The break statement to compile.
     * @param context The compilation context containing loop label information.
     * @param mv The method visitor used to generate bytecode instructions.
     * @throws CompilationException If the break statement is used outside of a loop.
     */
    override fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor) {
        val loopLabels = context.getCurrentLoopLabels()
            ?: throw CompilationException("Break statement used outside of a loop")
        
        mv.visitJumpInsn(Opcodes.GOTO, loopLabels.breakLabel)
    }
}

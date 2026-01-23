package com.mdeo.script.compiler.statements

import com.mdeo.script.ast.statements.TypedContinueStatement
import com.mdeo.script.ast.statements.TypedStatement
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.CompilationException
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles continue statements to bytecode.
 * 
 * A continue statement skips to the next iteration of the innermost enclosing loop.
 * It generates a GOTO instruction to the continue label (condition check) of the current loop.
 */
class ContinueStatementCompiler : StatementCompiler {
    
    /**
     * Determines if this compiler can handle the given statement.
     *
     * @param statement The typed statement to check.
     * @return True if the statement is a [TypedContinueStatement], false otherwise.
     */
    override fun canCompile(statement: TypedStatement): Boolean {
        return statement is TypedContinueStatement
    }
    
    /**
     * Compiles a continue statement to bytecode.
     *
     * Generates a GOTO instruction that jumps to the continue label of the current loop,
     * which typically points to the loop's condition check for the next iteration.
     *
     * @param statement The continue statement to compile.
     * @param context The compilation context containing loop labels.
     * @param mv The method visitor used to emit bytecode instructions.
     * @throws CompilationException If the continue statement is used outside of a loop.
     */
    override fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor) {
        val loopLabels = context.getCurrentLoopLabels()
            ?: throw CompilationException("Continue statement used outside of a loop")
        
        mv.visitJumpInsn(Opcodes.GOTO, loopLabels.continueLabel)
    }
}

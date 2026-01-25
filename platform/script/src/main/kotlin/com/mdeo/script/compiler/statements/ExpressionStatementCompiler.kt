package com.mdeo.script.compiler.statements

import com.mdeo.script.ast.TypedStatementKind
import com.mdeo.script.ast.statements.TypedExpressionStatement
import com.mdeo.script.ast.statements.TypedStatement
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.VoidType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles expression statements to bytecode.
 * 
 * An expression statement is an expression that is evaluated for its side effects,
 * not for its value. After evaluation, any value left on the stack must be popped
 * to maintain stack discipline.
 * 
 * Examples of expression statements:
 * - Function calls with void return: `println("hello")`
 * - Function calls with ignored return: `list.add(item)`
 * - Lambda invocations: `callback()`
 */
class ExpressionStatementCompiler : StatementCompiler {
    
    /**
     * Determines if this compiler can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is an expression statement, false otherwise.
     */
    override fun canCompile(statement: TypedStatement): Boolean {
        return statement.kind == TypedStatementKind.Expression
    }
    
    /**
     * Compiles an expression statement to bytecode.
     *
     * This method compiles the expression, determines its result type, and pops the
     * result from the stack since expression statements discard their value.
     *
     * @param statement The expression statement to compile.
     * @param context The compilation context providing access to expression compilation and type resolution.
     * @param mv The method visitor for emitting bytecode instructions.
     */
    override fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor) {
        val exprStmt = statement as TypedExpressionStatement
        
        val resultType = context.getType(exprStmt.expression.evalType)
        context.compileExpression(exprStmt.expression, mv, resultType)
        
        popResultIfNeeded(resultType, mv)
    }
    
    /**
     * Pops the expression result from the stack if it's not void.
     *
     * Stack slot handling:
     * - VoidType: No value on stack, nothing to pop.
     * - Primitive long or double (non-nullable): Uses POP2 as they occupy 2 stack slots.
     * - Boxed long/double (nullable): Uses POP as boxed types are references occupying 1 slot.
     * - All other types (int, float, boolean, string, objects, references): Uses POP for 1 slot.
     *
     * @param resultType The type of the expression result to potentially pop.
     * @param mv The method visitor for emitting bytecode instructions.
     */
    private fun popResultIfNeeded(resultType: ReturnType, mv: MethodVisitor) {
        when (resultType) {
            is VoidType -> {
            }
            is ClassTypeRef -> {
                when (resultType.type) {
                    "builtin.long", "builtin.double" -> {
                        if (!resultType.isNullable) {
                            mv.visitInsn(Opcodes.POP2)
                        } else {
                            mv.visitInsn(Opcodes.POP)
                        }
                    }
                    else -> {
                        mv.visitInsn(Opcodes.POP)
                    }
                }
            }
            else -> {
                mv.visitInsn(Opcodes.POP)
            }
        }
    }
}

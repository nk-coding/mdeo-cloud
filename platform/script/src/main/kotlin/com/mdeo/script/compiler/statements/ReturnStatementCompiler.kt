package com.mdeo.script.compiler.statements

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for return statements.
 * Generates bytecode to return a value from a method or return void.
 */
class ReturnStatementCompiler : StatementCompiler {
    
    /**
     * Checks if this compiler can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a return statement, false otherwise.
     */
    override fun canCompile(statement: TypedStatement): Boolean {
        return statement.kind == "return"
    }
    
    /**
     * Compiles a return statement to bytecode.
     *
     * This method handles both void returns (no value) and value returns.
     * For value returns, it compiles the return expression, applies any necessary
     * type coercion (boxing, unboxing, widening) using CoercionUtil, and emits
     * the appropriate return instruction.
     *
     * @param statement The return statement to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor for emitting bytecode.
     */
    override fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor) {
        val returnStmt = statement as TypedReturnStatement
        
        val returnValue = returnStmt.value
        if (returnValue == null) {
            mv.visitInsn(Opcodes.RETURN)
        } else {
            compileValueReturn(returnValue, context, mv)
        }
    }
    
    /**
     * Compiles a return statement that returns a value.
     *
     * @param returnValue The expression to return.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun compileValueReturn(returnValue: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val functionReturnType = context.getFunctionReturnType()
        val exprType = context.getType(returnValue.evalType)
        
        context.compileExpression(returnValue, mv, functionReturnType ?: exprType)
        
        emitTypedReturn(functionReturnType ?: exprType, mv)
    }
    
    /**
     * Emits the appropriate return instruction based on the function return type.
     * 
     * @param returnType The function return type.
     * @param mv The method visitor.
     */
    private fun emitTypedReturn(returnType: ReturnType, mv: MethodVisitor) {
        val opcode = getReturnOpcode(returnType)
        mv.visitInsn(opcode)
    }
    
    /**
     * Gets the appropriate return opcode for a given return type.
     * 
     * @param returnType The return type.
     * @return The appropriate return opcode.
     */
    private fun getReturnOpcode(returnType: ReturnType): Int {
        return when (returnType) {
            is VoidType -> Opcodes.RETURN
            is ClassTypeRef -> getClassTypeReturnOpcode(returnType)
            else -> Opcodes.ARETURN
        }
    }
    
    /**
     * Gets the return opcode for a class type reference.
     * 
     * @param classType The class type reference.
     * @return The appropriate return opcode.
     */
    private fun getClassTypeReturnOpcode(classType: ClassTypeRef): Int {
        if (classType.isNullable) {
            return Opcodes.ARETURN
        }
        
        return when (classType.type) {
            "builtin.int" -> Opcodes.IRETURN
            "builtin.long" -> Opcodes.LRETURN
            "builtin.float" -> Opcodes.FRETURN
            "builtin.double" -> Opcodes.DRETURN
            "builtin.boolean" -> Opcodes.IRETURN
            else -> Opcodes.ARETURN
        }
    }
}

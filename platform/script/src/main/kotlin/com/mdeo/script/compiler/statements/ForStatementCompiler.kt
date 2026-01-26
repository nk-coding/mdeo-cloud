package com.mdeo.script.compiler.statements

import com.mdeo.expression.ast.statements.TypedForStatement
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.LoopLabels
import com.mdeo.script.compiler.Scope
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles for statements to bytecode.
 * 
 * A for statement iterates over an iterable collection. The structure is:
 * 
 * ```
 * [compile iterable]
 * INVOKEINTERFACE java/lang/Iterable.iterator()Ljava/util/Iterator;
 * ASTORE iteratorSlot
 * 
 * loopStart:
 *   ALOAD iteratorSlot
 *   INVOKEINTERFACE java/util/Iterator.hasNext()Z
 *   IFEQ loopEnd
 * 
 *   ALOAD iteratorSlot
 *   INVOKEINTERFACE java/util/Iterator.next()Ljava/lang/Object;
 *   [unbox/cast if needed]
 *   [STORE to loop variable]
 * 
 *   [compile body statements]
 * 
 *   GOTO loopStart
 * 
 * loopEnd:
 * ```
 * 
 * This compiler registers loop labels with the compilation context
 * to support break and continue statements.
 */
class ForStatementCompiler : StatementCompiler {
    
    /**
     * Checks if this compiler can compile the given statement.
     * 
     * @param statement The statement to check.
     * @return True if the statement is a [TypedForStatement], false otherwise.
     */
    override fun canCompile(statement: TypedStatement): Boolean {
        return statement is TypedForStatement
    }
    
    /**
     * Compiles a for statement to bytecode.
     * 
     * This method sets up the iterator pattern by:
     * 1. Compiling the iterable expression and calling iterator() on it
     * 2. Creating a loop that calls hasNext() and next() on the iterator
     * 3. Unboxing/casting the element and storing it in the loop variable
     * 4. Compiling the body statements within the body scope
     * 5. Jumping back to the loop start, with loop end as the exit point
     * 
     * ScopeBuilder creates two scopes for for statements:
     * - forParamsScope: contains the loop variable and internal iterator variable
     * - forBodyScope: child of forParamsScope, contains body statements
     * 
     * The iterator variable is named `$iterator$<variableName>` and is added by ScopeBuilder.
     * 
     * @param statement The for statement to compile (must be a [TypedForStatement]).
     * @param context The compilation context providing scope and expression compilation.
     * @param mv The method visitor for emitting bytecode.
     */
    override fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor) {
        val forStmt = statement as TypedForStatement
        
        val loopStart = Label()
        val loopEnd = Label()
        
        val loopLabels = LoopLabels(continueLabel = loopStart, breakLabel = loopEnd)
        context.pushLoopLabels(loopLabels)
        
        val forBodyScope = context.getStatementScope(forStmt)
            ?: throw IllegalStateException(
                "For statement body scope not found in scope tree. This indicates a bug in ScopeBuilder - " +
                "all for statements should have their scopes registered during scope building phase."
            )

        val forParamsScope = forBodyScope.parent
            ?: throw IllegalStateException(
                "For statement params scope not found. This indicates a bug in ScopeBuilder - " +
                "for statements should have two scopes: params scope and body scope."
            )
        
        val loopVariable = forParamsScope.lookupVariable(forStmt.variableName, forParamsScope.level)
            ?: throw IllegalStateException("Loop variable not found in scope: ${forStmt.variableName}")
        
        val iteratorVariableName = "\$iterator\$${forStmt.variableName}"
        val iteratorVariable = forParamsScope.lookupVariable(iteratorVariableName, forParamsScope.level)
            ?: throw IllegalStateException("Iterator variable not found in scope: $iteratorVariableName")
        
        val variableType = context.getType(forStmt.variableType)
        
        val iteratorSlot = emitIteratorSetup(forStmt, context, mv, iteratorVariable.slotIndex)
        
        mv.visitLabel(loopStart)
        
        emitHasNextCheck(mv, iteratorSlot, loopEnd)
        
        emitNextAndStore(mv, iteratorSlot, variableType, loopVariable.slotIndex)
        
        compileLoopBody(forStmt, context, mv, forBodyScope)
        
        mv.visitJumpInsn(Opcodes.GOTO, loopStart)
        
        mv.visitLabel(loopEnd)
        
        context.popLoopLabels()
    }

    /**
     * Emits bytecode to compile the iterable expression, call iterator() on it,
     * and store the iterator in the designated slot.
     * 
     * @param forStmt The for statement containing the iterable expression.
     * @param context The compilation context for expression compilation.
     * @param mv The method visitor for emitting bytecode.
     * @param iteratorSlot The local variable slot to store the iterator.
     * @return The iterator slot index for subsequent use.
     */
    private fun emitIteratorSetup(
        forStmt: TypedForStatement,
        context: CompilationContext,
        mv: MethodVisitor,
        iteratorSlot: Int
    ): Int {
        val iterableType = context.getType(forStmt.iterable.evalType)
        context.compileExpression(forStmt.iterable, mv, iterableType)
        
        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/lang/Iterable",
            "iterator",
            "()Ljava/util/Iterator;",
            true
        )
        
        mv.visitVarInsn(Opcodes.ASTORE, iteratorSlot)
        return iteratorSlot
    }
    
    /**
     * Emits bytecode to load the iterator, call hasNext(), and jump to the loop end
     * if there are no more elements.
     * 
     * @param mv The method visitor for emitting bytecode.
     * @param iteratorSlot The local variable slot containing the iterator.
     * @param loopEnd The label to jump to when hasNext() returns false.
     */
    private fun emitHasNextCheck(mv: MethodVisitor, iteratorSlot: Int, loopEnd: Label) {
        mv.visitVarInsn(Opcodes.ALOAD, iteratorSlot)
        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/Iterator",
            "hasNext",
            "()Z",
            true
        )
        mv.visitJumpInsn(Opcodes.IFEQ, loopEnd)
    }
    
    /**
     * Emits bytecode to call next() on the iterator, unbox/cast the result,
     * and store it in the loop variable slot.
     * 
     * @param mv The method visitor for emitting bytecode.
     * @param iteratorSlot The local variable slot containing the iterator.
     * @param variableType The type of the loop variable (for unboxing/casting).
     * @param variableSlot The local variable slot for the loop variable.
     */
    private fun emitNextAndStore(
        mv: MethodVisitor,
        iteratorSlot: Int,
        variableType: ReturnType,
        variableSlot: Int
    ) {
        mv.visitVarInsn(Opcodes.ALOAD, iteratorSlot)
        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "java/util/Iterator",
            "next",
            "()Ljava/lang/Object;",
            true
        )
        
        ASMUtil.emitUnboxOrCast(variableType, mv)
        
        val storeOpcode = ASMUtil.getStoreOpcode(variableType)
        mv.visitVarInsn(storeOpcode, variableSlot)
    }
    
    /**
     * Enters the body scope, compiles all body statements, and exits the scope.
     * 
     * @param forStmt The for statement containing the body statements.
     * @param context The compilation context for statement compilation.
     * @param mv The method visitor for emitting bytecode.
     * @param forBodyScope The scope for the loop body.
     */
    private fun compileLoopBody(
        forStmt: TypedForStatement,
        context: CompilationContext,
        mv: MethodVisitor,
        forBodyScope: Scope
    ) {
        context.enterScope(forBodyScope)
        
        for (bodyStatement in forStmt.body) {
            context.compileStatement(bodyStatement, mv)
        }
        
        context.exitScope()
    }
    

    

}

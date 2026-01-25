package com.mdeo.script.compiler.statements

import com.mdeo.script.ast.expressions.TypedIdentifierExpression
import com.mdeo.script.ast.expressions.TypedMemberAccessExpression
import com.mdeo.script.ast.statements.TypedAssignmentStatement
import com.mdeo.script.ast.statements.TypedStatement
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.RefTypeUtil
import com.mdeo.script.compiler.StatementCompiler
import com.mdeo.script.compiler.VariableInfo
import com.mdeo.script.compiler.util.ASMUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles assignment statements to bytecode.
 * 
 * For variables NOT wrapped in a Ref:
 * - Compile the right-hand side expression
 * - Apply type conversion if needed
 * - Store to the local variable slot
 * 
 * For variables wrapped in a Ref:
 * - Load the Ref object
 * - Compile the right-hand side expression
 * - Apply type conversion if needed
 * - Store to the Ref's .value field
 * 
 * For member access expressions:
 * - Compile the target object expression
 * - Compile the right-hand side expression
 * - Apply type conversion if needed
 * - Store using PUTFIELD or setter method call
 */
class AssignmentCompiler : StatementCompiler {

    /**
     * Checks if this compiler can handle the given statement.
     *
     * @param statement the statement to check
     * @return true if the statement is an assignment statement, false otherwise
     */
    override fun canCompile(statement: TypedStatement): Boolean {
        return statement is TypedAssignmentStatement
    }

    /**
     * Compiles an assignment statement to bytecode.
     *
     * Dispatches to the appropriate assignment compiler based on the left-hand side
     * expression type (identifier or member access).
     *
     * @param statement the assignment statement to compile
     * @param context the compilation context containing scope and type information
     * @param mv the method visitor for emitting bytecode
     */
    override fun compile(statement: TypedStatement, context: CompilationContext, mv: MethodVisitor) {
        val assignment = statement as TypedAssignmentStatement

        val left = assignment.left
        when (left) {
            is TypedIdentifierExpression -> {
                compileIdentifierAssignment(left, assignment, context, mv)
            }

            is TypedMemberAccessExpression -> {
                compileMemberAccessAssignment(left, assignment, context, mv)
            }

            else -> {
                throw UnsupportedOperationException("Assignment to ${left.kind} not yet supported")
            }
        }
    }

    /**
     * Compiles an assignment to an identifier (local variable).
     *
     * Determines whether the variable is Ref-wrapped and emits the appropriate bytecode.
     * Variables that are written by lambdas need special handling with Ref types.
     *
     * @param left the identifier expression being assigned to
     * @param assignment the full assignment statement
     * @param context the compilation context containing scope and type information
     * @param mv the method visitor for emitting bytecode
     */
    private fun compileIdentifierAssignment(
        left: TypedIdentifierExpression,
        assignment: TypedAssignmentStatement,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val variable = context.currentScope?.lookupVariable(left.name, left.scope)
            ?: throw IllegalStateException("Variable not found: ${left.name} at scope level ${left.scope}")

        val targetType = variable.type

        if (variable.isWrittenByLambda && variable.slotIndex >= 0) {
            compileRefAssignment(variable, targetType, assignment, context, mv)
        } else {
            compileDirectAssignment(variable, targetType, assignment, context, mv)
        }
    }

    /**
     * Compiles a direct assignment to a local variable slot (not wrapped in Ref).
     *
     * Compiles the right-hand side expression, applies type coercion if needed,
     * then stores the result directly to the variable's local slot.
     *
     * @param variable the variable info containing the slot index
     * @param targetType the expected type of the variable
     * @param assignment the full assignment statement
     * @param context the compilation context containing type information
     * @param mv the method visitor for emitting bytecode
     */
    private fun compileDirectAssignment(
        variable: VariableInfo,
        targetType: ReturnType,
        assignment: TypedAssignmentStatement,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        // Compile expression with coercion to target type
        context.compileExpression(assignment.right, mv, targetType)

        val storeOpcode = ASMUtil.getStoreOpcode(targetType)
        mv.visitVarInsn(storeOpcode, variable.slotIndex)
    }

    /**
     * Compiles an assignment through a Ref wrapper.
     *
     * Used for variables that are written by lambdas and thus need to be wrapped
     * in a Ref object. Loads the Ref, compiles the right-hand side expression,
     * applies type coercion if needed, then stores to the Ref's value field.
     *
     * @param variable the variable info containing the slot index of the Ref
     * @param targetType the expected type of the variable (the wrapped type, not Ref)
     * @param assignment the full assignment statement
     * @param context the compilation context containing type information
     * @param mv the method visitor for emitting bytecode
     */
    private fun compileRefAssignment(
        variable: VariableInfo,
        targetType: ReturnType,
        assignment: TypedAssignmentStatement,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        mv.visitVarInsn(Opcodes.ALOAD, variable.slotIndex)

        // Compile expression with coercion to target type
        context.compileExpression(assignment.right, mv, targetType)

        val refClassName = RefTypeUtil.getRefClassName(targetType)
        val valueDescriptor = RefTypeUtil.getRefValueDescriptor(targetType)
        mv.visitFieldInsn(Opcodes.PUTFIELD, refClassName, "value", valueDescriptor)
    }

    /**
     * Compiles an assignment to a member access expression (property assignment).
     *
     * Compiles the target object expression, compiles the right-hand side expression,
     * applies type coercion if needed, then emits a property setter call.
     *
     * @param left the member access expression being assigned to
     * @param assignment the full assignment statement
     * @param context the compilation context containing type information
     * @param mv the method visitor for emitting bytecode
     */
    private fun compileMemberAccessAssignment(
        left: TypedMemberAccessExpression,
        assignment: TypedAssignmentStatement,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val targetObjectType = context.getType(left.expression.evalType)
        val propertyType = context.getType(left.evalType)

        context.compileExpression(left.expression, mv, targetObjectType)

        // Compile expression with coercion to property type
        context.compileExpression(assignment.right, mv, propertyType)

        emitPropertySet(context, left.member, targetObjectType, propertyType, mv)
    }

    /**
     * Emits bytecode to set a property on an object.
     *
     * Generates a setter method call (e.g., setPropertyName) using either
     * INVOKEINTERFACE for collection types or INVOKEVIRTUAL for regular classes.
     *
     * Lambda types are not expected to have settable properties, but we handle them
     * gracefully by treating them as Any.
     *
     * @param context the compilation context
     * @param memberName the name of the property being set
     * @param targetType the type of the object containing the property
     * @param propertyType the type of the property value
     * @param mv the method visitor for emitting bytecode
     */
    private fun emitPropertySet(
        context: CompilationContext,
        memberName: String,
        targetType: ReturnType,
        propertyType: ReturnType,
        mv: MethodVisitor
    ) {
        val lookupType = if (targetType is LambdaType) {
            "builtin.any"
        } else if (targetType is ClassTypeRef) {
            targetType.type
        } else {
            throw UnsupportedOperationException("Cannot set property on non-class/lambda type")
        }

        val propertyDef = context.typeRegistry.lookupProperty(lookupType, memberName)
            ?: throw UnsupportedOperationException("Property $memberName not found on type $lookupType")
        
        val valueDescriptor = ASMUtil.getTypeDescriptor(propertyType)
        propertyDef.emitSet(mv, valueDescriptor)
    }
}

package com.mdeo.script.compiler.expressions

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedListLiteralExpression
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiler for list literal expressions.
 * Generates bytecode to create a new ArrayList and populate it with element values.
 */
class ListLiteralCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a list literal, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == "listLiteral"
    }

    /**
     * Compiles the list literal expression to bytecode.
     * Creates a new ArrayList, populates it with the element values, and leaves it on the stack.
     *
     * @param expression The list literal expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor for bytecode generation.
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val listExpr = expression as TypedListLiteralExpression

        // Create new ArrayList with initial capacity
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList")
        mv.visitInsn(Opcodes.DUP)

        // Push initial capacity based on element count
        val elementCount = listExpr.elements.size
        when {
            elementCount >= -1 && elementCount <= 5 -> mv.visitInsn(Opcodes.ICONST_0 + elementCount)
            elementCount >= Byte.MIN_VALUE && elementCount <= Byte.MAX_VALUE -> mv.visitIntInsn(Opcodes.BIPUSH, elementCount)
            else -> mv.visitLdcInsn(elementCount)
        }

        // Call ArrayList constructor with initial capacity
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/util/ArrayList",
            "<init>",
            "(I)V",
            false
        )

        // Add each element to the list
        for (element in listExpr.elements) {
            mv.visitInsn(Opcodes.DUP) // Duplicate list reference for add call
            context.compileExpression(element, mv, getElementType(listExpr, context))
            mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "java/util/List",
                "add",
                "(Ljava/lang/Object;)Z",
                true
            )
            mv.visitInsn(Opcodes.POP) // Pop the boolean return value of add
        }
    }

    /**
     * Gets the element type from the list type.
     *
     * @param listExpr The list expression.
     * @param context The compilation context.
     * @return The element type or Any? if not determinable.
     */
    private fun getElementType(listExpr: TypedListLiteralExpression, context: CompilationContext): com.mdeo.expression.ast.types.ReturnType {
        val listType = context.getType(listExpr.evalType)
        if (listType is ClassTypeRef && listType.typeArgs?.isNotEmpty() == true) {
            return listType.typeArgs?.values?.first() ?: ClassTypeRef(type = "builtin.Any", isNullable = true)
        }
        return ClassTypeRef(type = "builtin.Any", isNullable = true)
    }
}

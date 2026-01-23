package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedMemberCallExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.TypeConversionUtil
import com.mdeo.script.compiler.ASMUtil
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles member call expressions to bytecode.
 * 
 * Member call expressions invoke methods on objects.
 * Supports null-safe chaining (?.) where if the expression is null,
 * the result is null instead of throwing a NullPointerException.
 * 
 * The compiler generates:
 * - INVOKEVIRTUAL for class method calls
 * - INVOKEINTERFACE for interface method calls
 * - INVOKESTATIC for helper classes (primitives/stdlib)
 * 
 * The `overload` field uniquely identifies the method signature
 * and is used to generate the correct method descriptor.
 */
class MemberCallCompiler : ExpressionCompiler {

    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a member call expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.MemberCall
    }

    /**
     * Compiles a member call expression to bytecode.
     *
     * @param expression The typed member call expression to compile.
     * @param context The compilation context containing type information and utilities.
     * @param mv The method visitor used to emit bytecode instructions.
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val memberCall = expression as TypedMemberCallExpression
        val targetType = context.getType(memberCall.expression.evalType)
        val resultType = context.getType(memberCall.evalType)
        
        if (memberCall.isNullChaining) {
            compileNullSafeMemberCall(memberCall, context, mv, targetType, resultType)
        } else {
            compileMemberCall(memberCall, context, mv, targetType, resultType)
        }
    }
    
    /**
     * Compiles a null-safe member call (?.).
     *
     * If the expression is null, the result is null instead of throwing a NullPointerException.
     * For null-safe chaining, the result must be boxed if the underlying method returns a primitive,
     * since the expression result type is always nullable.
     *
     * @param memberCall The null-safe member call expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The type of the target expression being called on.
     * @param resultType The expected result type of the expression.
     */
    private fun compileNullSafeMemberCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ReturnType,
        resultType: ReturnType
    ) {
        val nullLabel = Label()
        val endLabel = Label()

        context.compileExpression(memberCall.expression, mv)

        mv.visitInsn(Opcodes.DUP)
        mv.visitJumpInsn(Opcodes.IFNULL, nullLabel)

        emitMemberCall(memberCall, context, mv, targetType, resultType)

        boxPrimitiveReturnIfNeeded(memberCall.overload, mv)

        mv.visitJumpInsn(Opcodes.GOTO, endLabel)

        mv.visitLabel(nullLabel)
        mv.visitInsn(Opcodes.POP)
        mv.visitInsn(Opcodes.ACONST_NULL)

        mv.visitLabel(endLabel)
    }

    /**
     * Boxes the return value if the overload returns a primitive type.
     *
     * This is necessary for null-safe chaining where the expression result type is nullable
     * but the underlying method returns a non-nullable primitive.
     *
     * @param overload The method overload string containing return type information.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun boxPrimitiveReturnIfNeeded(overload: String, mv: MethodVisitor) {
        val overloadReturnType = parseOverloadReturnType(overload)
        if (overloadReturnType != null && CoercionUtil.isPrimitiveType(overloadReturnType)) {
            CoercionUtil.emitBoxing(overloadReturnType, mv)
        }
    }
    
    /**
     * Compiles a regular (non-null-safe) member call.
     *
     * Dispatches to the appropriate method based on the target type:
     * - Primitive types: calls static helper methods
     * - String type: calls string helper methods
     * - Other types: emits standard method invocation
     * 
     * Note: Lambda calls are now handled by ExpressionCallCompiler, not here.
     * TypedMemberCallExpression should only be used for actual method calls.
     *
     * @param memberCall The member call expression to compile.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The type of the target expression being called on.
     * @param resultType The expected result type of the expression.
     */
    private fun compileMemberCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ReturnType,
        resultType: ReturnType
    ) {
        when {
            targetType is ClassTypeRef && CoercionUtil.isPrimitiveType(targetType.type) -> {
                emitPrimitiveHelperCall(memberCall, context, mv, targetType, resultType)
            }
            targetType is ClassTypeRef && targetType.type == "builtin.string" -> {
                emitStringMethodCall(memberCall, context, mv, resultType)
            }
            else -> {
                context.compileExpression(memberCall.expression, mv)
                emitMemberCall(memberCall, context, mv, targetType, resultType)
            }
        }
    }
    
    /**
     * Emits a member call on the object already present on the stack.
     *
     * Dispatches to the appropriate emission method based on the target type:
     * - Primitive types: uses static helper methods
     * - String type: uses string helper methods
     * - Collection types: uses interface method invocation
     * - Other types: uses virtual or interface method invocation
     *
     * @param memberCall The member call expression to emit.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The type of the target expression (must be a ClassTypeRef).
     * @param resultType The expected result type of the expression.
     * @throws UnsupportedOperationException If the target type is not a class type.
     */
    private fun emitMemberCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ReturnType,
        resultType: ReturnType
    ) {
        if (targetType !is ClassTypeRef) {
            throw UnsupportedOperationException("Cannot call member on non-class type")
        }

        when {
            CoercionUtil.isPrimitiveType(targetType.type) -> {
                emitPrimitiveHelperCallWithReceiver(memberCall, context, mv, targetType, resultType)
            }
            targetType.type == "builtin.string" -> {
                emitStringMethodCallWithReceiver(memberCall, context, mv, resultType)
            }
            isCollectionType(targetType.type) -> {
                emitCollectionMethodCall(memberCall, context, mv, targetType, resultType)
            }
            else -> {
                emitObjectMethodCall(memberCall, context, mv, targetType, resultType)
            }
        }
    }
    
    /**
     * Emits a call to a primitive helper method.
     *
     * Since primitives don't have methods in the JVM, this emits a call to
     * a static helper class method (e.g., IntHelper, LongHelper) that takes
     * the primitive value as the first argument.
     *
     * @param memberCall The member call expression on a primitive type.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The primitive type being called on.
     * @param resultType The expected result type of the expression.
     */
    private fun emitPrimitiveHelperCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ClassTypeRef,
        resultType: ReturnType
    ) {
        context.compileExpression(memberCall.expression, mv)

        compileArgumentsWithCoercion(memberCall, context, mv)

        val helperClass = getHelperClass(targetType.type)
        val methodDescriptor = buildPrimitiveHelperDescriptor(targetType, memberCall.overload, memberCall.arguments, context)

        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            helperClass,
            memberCall.member,
            methodDescriptor,
            false
        )

        emitReturnTypeCoercion(resultType, memberCall.overload, mv)
    }

    /**
     * Compiles method arguments with type coercion based on the overload's parameter types.
     *
     * @param memberCall The member call expression containing the arguments.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun compileArgumentsWithCoercion(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        val parameterTypes = parseOverloadParameterTypes(memberCall.overload)
        for ((index, arg) in memberCall.arguments.withIndex()) {
            context.compileExpression(arg, mv)

            if (index < parameterTypes.size) {
                val argType = context.getType(arg.evalType)
                emitArgumentCoercion(argType, parameterTypes[index], arg, mv)
            }
        }
    }
    
    /**
     * Emits a primitive helper call when the receiver is already on the stack.
     *
     * Similar to emitPrimitiveHelperCall but assumes the primitive value
     * has already been pushed onto the stack.
     *
     * @param memberCall The member call expression on a primitive type.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The primitive type being called on.
     * @param resultType The expected result type of the expression.
     */
    private fun emitPrimitiveHelperCallWithReceiver(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ClassTypeRef,
        resultType: ReturnType
    ) {
        compileArgumentsWithCoercion(memberCall, context, mv)

        val helperClass = getHelperClass(targetType.type)
        val methodDescriptor = buildPrimitiveHelperDescriptor(targetType, memberCall.overload, memberCall.arguments, context)

        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            helperClass,
            memberCall.member,
            methodDescriptor,
            false
        )

        emitReturnTypeCoercion(resultType, memberCall.overload, mv)
    }
    
    /**
     * Emits a string method call.
     *
     * Compiles the string expression and delegates to the receiver variant.
     *
     * @param memberCall The member call expression on a string type.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param resultType The expected result type of the expression.
     */
    private fun emitStringMethodCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        resultType: ReturnType
    ) {
        context.compileExpression(memberCall.expression, mv)
        emitStringMethodCallWithReceiver(memberCall, context, mv, resultType)
    }
    
    /**
     * Emits a string method call when the receiver is already on the stack.
     *
     * String methods are implemented as static helper methods in StringHelper,
     * with the string as the first argument.
     *
     * @param memberCall The member call expression on a string type.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param resultType The expected result type of the expression.
     */
    private fun emitStringMethodCallWithReceiver(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        resultType: ReturnType
    ) {
        compileArgumentsWithCoercion(memberCall, context, mv)

        val methodDescriptor = buildStringHelperDescriptor(memberCall.overload, memberCall.arguments, context)

        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "com/mdeo/script/stdlib/primitives/StringHelper",
            memberCall.member,
            methodDescriptor,
            false
        )

        emitReturnTypeCoercion(resultType, memberCall.overload, mv)
    }
    
    /**
     * Emits a collection method call.
     *
     * Collection types use interface method invocation (INVOKEINTERFACE)
     * on the appropriate Script collection interface.
     *
     * @param memberCall The member call expression on a collection type.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The collection type being called on.
     * @param resultType The expected result type of the expression.
     */
    private fun emitCollectionMethodCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ClassTypeRef,
        resultType: ReturnType
    ) {
        compileArgumentsWithCoercion(memberCall, context, mv)

        val collectionInterface = getCollectionInterface(targetType.type)
        val methodDescriptor = parseOverloadDescriptor(memberCall.overload)

        mv.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            collectionInterface,
            memberCall.member,
            methodDescriptor,
            true
        )

        emitReturnTypeCoercion(resultType, memberCall.overload, mv)
    }
    
    /**
     * Emits a general object method call.
     *
     * Uses INVOKEINTERFACE for interface types and INVOKEVIRTUAL for class types.
     *
     * @param memberCall The member call expression on an object type.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     * @param targetType The object type being called on.
     * @param resultType The expected result type of the expression.
     */
    private fun emitObjectMethodCall(
        memberCall: TypedMemberCallExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        targetType: ClassTypeRef,
        resultType: ReturnType
    ) {
        compileArgumentsWithCoercion(memberCall, context, mv)

        val ownerClass = typeToInternalName(targetType.type)
        val methodDescriptor = parseOverloadDescriptor(memberCall.overload)
        val isInterface = isInterfaceType(targetType.type)

        mv.visitMethodInsn(
            if (isInterface) { Opcodes.INVOKEINTERFACE } else { Opcodes.INVOKEVIRTUAL },
            ownerClass,
            memberCall.member,
            methodDescriptor,
            isInterface
        )

        emitReturnTypeCoercion(resultType, memberCall.overload, mv)
    }
    
    /**
     * Builds a method descriptor for a primitive helper method.
     *
     * Uses the overload's return type, not the expression's result type,
     * to correctly handle null-safe chaining where the expression result
     * is nullable but the underlying method returns a non-nullable type.
     *
     * @param targetType The primitive target type (used as first parameter).
     * @param overload The method overload string containing return type info.
     * @param arguments The method arguments.
     * @param context The compilation context for type descriptor resolution.
     * @return The JVM method descriptor string.
     */
    private fun buildPrimitiveHelperDescriptor(
        targetType: ClassTypeRef,
        overload: String,
        arguments: List<TypedExpression>,
        context: CompilationContext
    ): String {
        val receiverDesc = getPrimitiveDescriptor(targetType.type)
        val argDescriptors = arguments.joinToString("") { arg ->
            ASMUtil.getTypeDescriptor(context.getType(arg.evalType))
        }
        val returnStart = overload.indexOf(':')
        val returnString = if (returnStart >= 0) overload.substring(returnStart + 1).trim() else "void"
        val returnDesc = typeToDescriptor(returnString)
        return "($receiverDesc$argDescriptors)$returnDesc"
    }
    
    /**
     * Builds a method descriptor for a string helper method.
     *
     * Uses the overload's return type, not the expression's result type,
     * to correctly handle null-safe chaining where the expression result
     * is nullable but the underlying method returns a non-nullable type.
     *
     * @param overload The method overload string containing return type info.
     * @param arguments The method arguments.
     * @param context The compilation context for type descriptor resolution.
     * @return The JVM method descriptor string.
     */
    private fun buildStringHelperDescriptor(
        overload: String,
        arguments: List<TypedExpression>,
        context: CompilationContext
    ): String {
        val argDescriptors = arguments.joinToString("") { arg ->
            ASMUtil.getTypeDescriptor(context.getType(arg.evalType))
        }
        val returnStart = overload.indexOf(':')
        val returnString = if (returnStart >= 0) overload.substring(returnStart + 1).trim() else "void"
        val returnDesc = typeToDescriptor(returnString)
        return "(Ljava/lang/String;$argDescriptors)$returnDesc"
    }
    
    /**
     * Parses the overload string to extract parameter types.
     *
     * @param overload The method overload string in format "methodName(param1, param2):returnType".
     * @return A list of parameter type strings, or empty list if no parameters.
     */
    private fun parseOverloadParameterTypes(overload: String): List<String> {
        val paramsStart = overload.indexOf('(')
        val paramsEnd = overload.indexOf(')')
        if (paramsStart < 0 || paramsEnd < 0 || paramsEnd <= paramsStart + 1) {
            return emptyList()
        }
        
        val paramsString = overload.substring(paramsStart + 1, paramsEnd)
        if (paramsString.isBlank()) {
            return emptyList()
        }
        
        return paramsString.split(',').map { it.trim() }
    }
    
    /**
     * Parses the overload string to extract the return type.
     *
     * Returns the base return type without the nullable marker (?).
     *
     * @param overload The method overload string in format "methodName(params):returnType".
     * @return The base return type string, or null if no return type specified.
     */
    private fun parseOverloadReturnType(overload: String): String? {
        val returnStart = overload.indexOf(':')
        if (returnStart < 0) return null
        
        val returnString = overload.substring(returnStart + 1).trim()
        return if (returnString.endsWith("?")) {
            returnString.dropLast(1)
        } else {
            returnString
        }
    }
    
    /**
     * Parses the overload string to generate a JVM method descriptor.
     *
     * @param overload The method overload string in format "methodName(params):returnType".
     * @return The JVM method descriptor string.
     * @throws IllegalArgumentException If the overload format is invalid.
     */
    private fun parseOverloadDescriptor(overload: String): String {
        val paramsStart = overload.indexOf('(')
        val paramsEnd = overload.indexOf(')')
        val returnStart = overload.indexOf(':')
        
        if (paramsStart < 0 || paramsEnd < 0 || returnStart < 0) {
            throw IllegalArgumentException("Invalid overload format: $overload")
        }
        
        val paramsString = overload.substring(paramsStart + 1, paramsEnd)
        val returnString = overload.substring(returnStart + 1).trim()
        
        val paramDescriptors = if (paramsString.isBlank()) {
            ""
        } else {
            paramsString.split(',').joinToString("") { typeToDescriptor(it.trim()) }
        }
        
        val returnDescriptor = typeToDescriptor(returnString)
        
        return "($paramDescriptors)$returnDescriptor"
    }
    
    /**
     * Converts a type string to a JVM type descriptor.
     *
     * @param typeString The type string (e.g., "builtin.int", "builtin.string?").
     * @return The JVM type descriptor (e.g., "I", "Ljava/lang/String;").
     */
    private fun typeToDescriptor(typeString: String): String {
        val isNullable = typeString.endsWith("?")
        val baseName = if (isNullable) typeString.dropLast(1) else typeString
        
        return when (baseName) {
            "builtin.int" -> if (isNullable) "Ljava/lang/Integer;" else "I"
            "builtin.long" -> if (isNullable) "Ljava/lang/Long;" else "J"
            "builtin.float" -> if (isNullable) "Ljava/lang/Float;" else "F"
            "builtin.double" -> if (isNullable) "Ljava/lang/Double;" else "D"
            "builtin.boolean" -> if (isNullable) "Ljava/lang/Boolean;" else "Z"
            "builtin.string" -> "Ljava/lang/String;"
            "builtin.any" -> "Ljava/lang/Object;"
            "void" -> "V"
            else -> "Ljava/lang/Object;"
        }
    }
    
    /**
     * Gets the helper class for a primitive type.
     *
     * @param typeName The primitive type name (e.g., "builtin.int").
     * @return The internal class name of the helper class.
     * @throws IllegalArgumentException If the type is not a known primitive type.
     */
    private fun getHelperClass(typeName: String): String {
        return when (typeName) {
            "builtin.int" -> "com/mdeo/script/stdlib/primitives/IntHelper"
            "builtin.long" -> "com/mdeo/script/stdlib/primitives/LongHelper"
            "builtin.float" -> "com/mdeo/script/stdlib/primitives/FloatHelper"
            "builtin.double" -> "com/mdeo/script/stdlib/primitives/DoubleHelper"
            "builtin.boolean" -> "com/mdeo/script/stdlib/primitives/BooleanHelper"
            else -> throw IllegalArgumentException("Unknown primitive type: $typeName")
        }
    }
    
    /**
     * Gets the collection interface name for a stdlib collection type.
     *
     * @param typeName The collection type name (e.g., "stdlib.List").
     * @return The internal class name of the collection interface.
     */
    private fun getCollectionInterface(typeName: String): String {
        val collectionName = typeName.substringAfter("stdlib.")
        return "com/mdeo/script/stdlib/collections/Script$collectionName"
    }
    
    /**
     * Checks if a type is a collection type.
     *
     * @param typeName The type name to check.
     * @return True if the type is a stdlib collection (List, Set, Bag, OrderedSet, Map, Sequence).
     */
    private fun isCollectionType(typeName: String): Boolean {
        return typeName.startsWith("stdlib.") && typeName.substringAfter("stdlib.") in listOf(
            "List", "Set", "Bag", "OrderedSet", "Map", "Sequence"
        )
    }
    
    /**
     * Checks if a type is an interface type.
     *
     * @param typeName The type name to check.
     * @return True if the type is an interface (currently only collection types).
     */
    private fun isInterfaceType(typeName: String): Boolean {
        return isCollectionType(typeName)
    }
    
    /**
     * Gets the primitive descriptor for a primitive type.
     *
     * @param typeName The primitive type name (e.g., "builtin.int").
     * @return The JVM primitive type descriptor (e.g., "I" for int).
     */
    private fun getPrimitiveDescriptor(typeName: String): String {
        return when (typeName) {
            "builtin.int" -> "I"
            "builtin.long" -> "J"
            "builtin.float" -> "F"
            "builtin.double" -> "D"
            "builtin.boolean" -> "Z"
            else -> "Ljava/lang/Object;"
        }
    }

    
    /**
     * Converts a type name to a JVM internal class name.
     *
     * @param typeName The type name to convert (e.g., "stdlib.List" or "com.example.MyClass").
     * @return The JVM internal class name (e.g., "com/mdeo/script/stdlib/collections/ScriptList").
     */
    private fun typeToInternalName(typeName: String): String {
        return when {
            typeName.startsWith("stdlib.") -> {
                val name = typeName.substringAfter("stdlib.")
                "com/mdeo/script/stdlib/collections/Script$name"
            }
            else -> typeName.replace(".", "/")
        }
    }
    
    /**
     * Emits argument coercion bytecode if needed.
     *
     * For nullable parameters, if the argument is a primitive of a different type,
     * the primitive is first widened and then boxed to the target wrapper type.
     *
     * @param argType The actual type of the argument expression.
     * @param paramType The expected parameter type from the overload.
     * @param arg The argument expression (used for additional coercion info).
     * @param mv The method visitor for emitting bytecode.
     */
    private fun emitArgumentCoercion(
        argType: ReturnType,
        paramType: String,
        arg: TypedExpression,
        mv: MethodVisitor
    ) {
        if (argType !is ClassTypeRef) return
        
        val isParamNullable = paramType.endsWith("?")
        val paramBaseName = if (isParamNullable) paramType.dropLast(1) else paramType
        
        if (paramBaseName == "builtin.any" || isParamNullable) {
            if (!argType.isNullable && CoercionUtil.isPrimitiveType(argType.type)) {
                if (CoercionUtil.isPrimitiveType(paramBaseName) && argType.type != paramBaseName) {
                    TypeConversionUtil.emitConversion(argType.type, paramBaseName, mv)
                    CoercionUtil.emitBoxing(paramBaseName, mv)
                } else {
                    CoercionUtil.emitBoxing(argType.type, mv)
                }
            }
        } else {
            val targetType = ClassTypeRef(paramBaseName, isParamNullable)
            CoercionUtil.emitCoercion(argType, targetType, arg, mv)
        }
    }
    
    /**
     * Emits return type coercion if needed.
     *
     * Unboxes the return value if the expected type is a non-nullable primitive
     * but the method returns a nullable or Any type.
     *
     * @param expectedType The expected result type of the expression.
     * @param overload The method overload string containing return type info.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun emitReturnTypeCoercion(
        expectedType: ReturnType,
        overload: String,
        mv: MethodVisitor
    ) {
        if (expectedType !is ClassTypeRef) {
            return
        }
        
        val returnStart = overload.indexOf(':')
        if (returnStart < 0) return
        
        val returnString = overload.substring(returnStart + 1).trim()
        val isReturnNullable = returnString.endsWith("?")
        val returnBaseName = if (isReturnNullable) returnString.dropLast(1) else returnString
        
        if ((returnBaseName == "builtin.any" || isReturnNullable) && 
            !expectedType.isNullable && CoercionUtil.isPrimitiveType(expectedType.type)) {
            CoercionUtil.emitUnboxing(expectedType.type, mv)
        }
    }
}

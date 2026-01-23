package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedFunctionCallExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.compiler.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.TypeConversionUtil
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles function call expressions to bytecode.
 * 
 * Function calls invoke top-level functions defined in the current file
 * or imported from other files. The compiler generates INVOKESTATIC
 * instructions to the correct generated class/method.
 * 
 * The `overload` field in TypedFunctionCallExpression uniquely identifies
 * the method signature and is used to generate the correct method descriptor.
 */
class FunctionCallCompiler : ExpressionCompiler {
    
    /**
     * Checks if this compiler can handle the given expression.
     *
     * @param expression The typed expression to check.
     * @return True if the expression is a function call, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.FunctionCall
    }
    
    /**
     * Compiles a function call expression to JVM bytecode.
     *
     * This method compiles each argument expression, applies necessary type coercions,
     * emits an INVOKESTATIC instruction to call the target function, and handles
     * return type coercion if needed.
     *
     * @param expression The function call expression to compile (must be TypedFunctionCallExpression).
     * @param context The compilation context containing AST and type information.
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val functionCall = expression as TypedFunctionCallExpression
        
        val parameterTypes = parseOverloadParameterTypes(functionCall.overload)
        
        for ((index, arg) in functionCall.arguments.withIndex()) {
            context.compileExpression(arg, mv)
            
            if (index < parameterTypes.size) {
                val argType = context.getType(arg.evalType)
                val paramType = parameterTypes[index]
                emitArgumentCoercion(argType, paramType, arg, mv)
            }
        }
        
        val ownerClass = resolveOwnerClass(functionCall.name, context)
        val methodDescriptor = parseOverloadDescriptor(functionCall.overload)
        
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            ownerClass,
            functionCall.name,
            methodDescriptor,
            false
        )
        
        val returnType = context.getType(functionCall.evalType)
        emitReturnTypeCoercion(returnType, functionCall.overload, mv)
    }
    
    /**
     * Resolves the JVM internal class name that owns a function.
     *
     * Resolution order: first checks if the function is defined in the current file,
     * then checks imports, and falls back to the current class name.
     *
     * @param functionName The name of the function to resolve.
     * @param context The compilation context containing AST and import information.
     * @return The JVM internal class name (e.g., "com/mdeo/script/generated/Script_xxx").
     */
    private fun resolveOwnerClass(functionName: String, context: CompilationContext): String {
        val localFunction = context.ast.functions.find { it.name == functionName }
        if (localFunction != null) {
            return context.currentClassName
        }
        
        val importedFunction = context.ast.imports.find { it.name == functionName }
        if (importedFunction != null) {
            return uriToClassName(importedFunction.uri)
        }
        
        return context.currentClassName
    }
    
    /**
     * Converts a file URI to a JVM internal class name.
     *
     * Non-alphanumeric characters are encoded as underscore followed by their hex code
     * to create a valid Java class name.
     *
     * @param fileUri The file URI to convert.
     * @return The JVM internal class name in the format "com/mdeo/script/generated/Script_xxx".
     */
    private fun uriToClassName(fileUri: String): String {
        val sanitized = buildString {
            for (char in fileUri) {
                when {
                    char.isLetterOrDigit() -> append(char)
                    else -> append("_${char.code.toString(16).uppercase().padStart(2, '0')}")
                }
            }
        }
        return "com/mdeo/script/generated/Script_$sanitized"
    }
    
    /**
     * Parses the overload string to extract parameter types.
     * 
     * The overload format is: functionName(paramType1,paramType2,...):returnType
     * Where paramType is like "builtin.int", "builtin.string", etc.
     * 
     * @param overload The overload string in the format "name(types):returnType".
     * @return List of parameter type names as strings (e.g., ["builtin.int", "builtin.string"]).
     *         Returns an empty list if no parameters are present.
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
     * Parses the overload string to generate a JVM method descriptor.
     * 
     * @param overload The overload string in the format "name(types):returnType".
     * @return The JVM method descriptor (e.g., "(II)I" for two int params returning int).
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
     * Handles both primitive types and their nullable variants. Nullable primitives
     * are represented as their wrapper types (e.g., "builtin.int?" becomes "Ljava/lang/Integer;").
     *
     * @param typeString The type name (e.g., "builtin.int", "builtin.string?").
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
     * Emits argument coercion bytecode if needed.
     *
     * Handles boxing primitives for generic parameters (Any?) and type widening.
     * For nullable parameters, if the argument is a primitive of a different type,
     * the primitive is first widened to the target type and then boxed to the
     * target wrapper type. For same-type or Any? parameters, the source type
     * is simply boxed.
     *
     * @param argType The actual type of the argument expression.
     * @param paramType The expected parameter type from the function signature.
     * @param arg The argument expression (used for coercion context).
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    private fun emitArgumentCoercion(
        argType: ReturnType,
        paramType: String,
        arg: TypedExpression,
        mv: MethodVisitor
    ) {
        if (argType !is ClassTypeRef) {
            return
        }
        
        val isParamNullable = paramType.endsWith("?")
        val paramBaseName = if (isParamNullable) paramType.dropLast(1) else paramType
        
        if (paramBaseName == "builtin.any" || isParamNullable) {
            if (!argType.isNullable && CoercionUtil.isPrimitiveType(argType.type)) {
                emitPrimitiveToBoxedCoercion(argType.type, paramBaseName, mv)
            }
        } else {
            val targetType = ClassTypeRef(paramBaseName, isParamNullable)
            CoercionUtil.emitCoercion(argType, targetType, arg, mv)
        }
    }
    
    /**
     * Emits bytecode to widen a primitive type and box it to the target wrapper type.
     *
     * If the source and target are different primitive types, the source is first
     * widened (e.g., int -> long) and then boxed. If they are the same type or
     * the target is Any, the source is simply boxed.
     *
     * @param sourceType The source primitive type name (e.g., "builtin.int").
     * @param targetType The target type name (e.g., "builtin.long" or "builtin.any").
     * @param mv The ASM MethodVisitor for emitting bytecode.
     */
    private fun emitPrimitiveToBoxedCoercion(sourceType: String, targetType: String, mv: MethodVisitor) {
        if (CoercionUtil.isPrimitiveType(targetType) && sourceType != targetType) {
            TypeConversionUtil.emitConversion(sourceType, targetType, mv)
            CoercionUtil.emitBoxing(targetType, mv)
        } else {
            CoercionUtil.emitBoxing(sourceType, mv)
        }
    }
    
    /**
     * Emits return type coercion if needed.
     *
     * Handles unboxing when the return type from the function signature is a
     * generic (Any?) but the expected type at the call site is a primitive.
     *
     * @param expectedType The expected return type at the call site.
     * @param overload The function overload string containing the return type.
     * @param mv The ASM MethodVisitor for emitting bytecode.
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
        if (returnStart < 0) {
            return
        }
        
        val returnString = overload.substring(returnStart + 1).trim()
        val isReturnNullable = returnString.endsWith("?")
        val returnBaseName = if (isReturnNullable) returnString.dropLast(1) else returnString
        
        if ((returnBaseName == "builtin.any" || isReturnNullable) && 
            !expectedType.isNullable && CoercionUtil.isPrimitiveType(expectedType.type)) {
            CoercionUtil.emitUnboxing(expectedType.type, mv)
        }
    }
}

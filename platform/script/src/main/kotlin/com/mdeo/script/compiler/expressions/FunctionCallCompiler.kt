package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.TypedExpressionKind
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedFunctionCallExpression
import com.mdeo.script.compiler.CompilationContext
import org.objectweb.asm.MethodVisitor

/**
 * Compiles function call expressions to bytecode using the unified registry system.
 *
 * All functions (global stdlib and file-scope) are resolved through the hierarchical
 * [FunctionRegistry] system, which provides automatic fallback from file-scope to
 * imported functions to global stdlib functions.
 *
 * **Function Types:**
 * - Global functions: `println`, `listOf`, etc. with potential overloads
 * - File-scope functions: defined in current file, single overload (key "")
 * - Imported functions: from other files, resolved via import registries
 *
 * **Overload Keys:**
 * - File-scope functions: empty string `""` (single signature)
 * - Global functions: specific key like `"builtin.int"` or `""` for default
 *
 * **Compilation Strategy:**
 * All functions MUST be in the registry before compilation. If a function is missing,
 * this indicates a bug in the registration phase and will result in a compilation error.
 */
class FunctionCallCompiler : AbstractCallCompiler() {

    /**
     * Checks if this compiler can handle the given expression.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression.kind == TypedExpressionKind.FunctionCall
    }

    /**
     * Compiles a function call expression to bytecode.
     *
     * Resolves the function through the unified registry and emits proper
     * INVOKESTATIC bytecode with parameter coercion and return type handling.
     *
     * @throws IllegalStateException if function is not found in registry
     */
    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val functionCall = expression as TypedFunctionCallExpression
        
        val funcDef = context.functionRegistry.lookupFunction(functionCall.name)
            ?: error("Function '${functionCall.name}' not found in registry")
        
        val signature = funcDef.getOverload(functionCall.overload)
            ?: error("Overload '${functionCall.overload}' not found for function '${functionCall.name}'")
        
        if (signature.isVarArgs) {
            compileVarArgsArray(functionCall.arguments, context, mv)
        } else {
            compileArgumentsWithCoercion(
                functionCall.arguments,
                signature.parameterTypes,
                context,
                mv
            )
        }
        
        signature.emitInvocation(mv)
        
        val expectedReturnType = context.getType(functionCall.evalType)
        emitReturnTypeCoercion(expectedReturnType, signature.returnType, mv)
    }
}

package com.mdeo.script.compiler.expressions

import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.expressions.TypedLambdaExpression
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.LambdaType
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.VoidType
import com.mdeo.script.compiler.util.ASMUtil
import com.mdeo.script.compiler.util.CoercionUtil
import com.mdeo.script.compiler.CompilationContext
import com.mdeo.script.compiler.ExpressionCompiler
import com.mdeo.script.compiler.LambdaInterfaceRegistry
import com.mdeo.script.compiler.LocalVariableIndexAssigner
import com.mdeo.script.compiler.RefTypeUtil
import com.mdeo.script.compiler.Scope
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Compiles lambda expressions to bytecode using invokedynamic.
 * 
 * Lambda expressions are compiled using the invokedynamic instruction with
 * LambdaMetafactory as the bootstrap method. Custom functional interfaces
 * (Lambda$Int$Int, etc.) are generated to avoid boxing overhead.
 * The method in interfaces is always named "call".
 * 
 * This compiler overrides [compile] instead of [compileInternal] to handle
 * expected type coercion directly. When the expected type is a different lambda
 * type, this compiler adapts the generated lambda to match the expected signature,
 * avoiding unnecessary wrapper lambdas.
 */
class LambdaCompiler : ExpressionCompiler() {

    /**
     * Checks if this compiler can compile the given expression.
     *
     * @param expression The expression to check.
     * @return True if the expression is a lambda expression, false otherwise.
     */
    override fun canCompile(expression: TypedExpression): Boolean {
        return expression is TypedLambdaExpression
    }

    /**
     * Compiles a lambda expression to bytecode, adapting to the expected type.
     *
     * This method overrides the base compile to handle lambda type coercion directly.
     * When expectedType is a LambdaType different from the lambda's natural type,
     * this method generates the lambda with the expected signature, handling:
     * - Extra parameters (reserved but not registered)
     * - Parameter type coercion (expected → actual)
     * - Return type coercion (actual → expected)
     *
     * @param expression The lambda expression to compile.
     * @param context The compilation context containing type information and scope.
     * @param mv The method visitor for emitting bytecode.
     * @param expectedType The type the lambda should conform to.
     */
    override fun compile(
        expression: TypedExpression,
        context: CompilationContext,
        mv: MethodVisitor,
        expectedType: ReturnType
    ) {
        val lambda = expression as TypedLambdaExpression
        val actualLambdaType = context.getType(lambda.evalType) as LambdaType

        val effectiveLambdaType = expectedType as? LambdaType ?: actualLambdaType

        val registry = context.getLambdaInterfaceRegistry()
        val normalizedKey = registry.createKey(effectiveLambdaType)

        val capturedVariables = analyzeCapturedVariables(lambda, context)
        val methodName = context.generateLambdaMethodName()

        generateSyntheticMethod(
            methodName,
            lambda,
            actualLambdaType,
            effectiveLambdaType,
            capturedVariables,
            context
        )

        emitInvokeDynamic(methodName, effectiveLambdaType, normalizedKey, capturedVariables, context, mv)
    }

    override fun compileInternal(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        throw IllegalStateException("LambdaCompiler uses compile() directly, not compileInternal()")
    }


    /**
     * Analyzes the lambda body to determine which outer variables are captured.
     *
     * Uses the scope tree exclusively to find captured variables. This approach
     * is more complete as it handles all statement types including for loops.
     * The scope tree is built by ScopeBuilder during earlier compilation phases.
     *
     * @param lambda The lambda expression to analyze.
     * @param context The compilation context containing scope information.
     * @return A list of captured variables found in the lambda body.
     * @throws IllegalStateException If the scope is not found (indicates a bug in ScopeBuilder).
     */
    private fun analyzeCapturedVariables(
        lambda: TypedLambdaExpression,
        context: CompilationContext
    ): List<CapturedVariable> {
        val lambdaBodyScope = context.getStatementScope(lambda)
            ?: throw IllegalStateException(
                "Lambda scope not found in scope tree. This indicates a bug in ScopeBuilder - " +
                        "all lambda expressions should have their scopes registered during scope building phase."
            )

        return collectCapturedVariablesFromScope(lambdaBodyScope, context)
    }

    /**
     * Collects captured variables using the scope tree.
     *
     * Uses the Scope.collectCapturedVariables method which walks the scope tree
     * and finds all variables that are read or written but declared outside
     * the lambda boundary.
     *
     * @param lambdaBodyScope The lambda's body scope from the scope tree.
     * @param context The compilation context for variable lookups.
     * @return A list of captured variables.
     */
    private fun collectCapturedVariablesFromScope(
        lambdaBodyScope: Scope,
        context: CompilationContext
    ): List<CapturedVariable> {
        val lambdaParamsLevel = lambdaBodyScope.parent?.level ?: lambdaBodyScope.level
        val capturedPairs = lambdaBodyScope.collectCapturedVariables(lambdaParamsLevel)

        return capturedPairs.mapNotNull { (name, declarationLevel) ->
            val variable = context.currentScope?.lookupVariable(name, declarationLevel)

            if (variable != null) {
                CapturedVariable(
                    name = name,
                    type = variable.type,
                    isRef = variable.isWrittenByLambda,
                    outerSlotIndex = variable.slotIndex,
                    scopeLevel = declarationLevel
                )
            } else {
                null
            }
        }
    }

    /**
     * Generates a private static synthetic method in the current class for the lambda body.
     *
     * Creates a method with ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC flags. The method
     * descriptor includes captured variables followed by lambda parameters. The method body
     * compiles the lambda statements using existing statement compilers.
     *
     * @param methodName The name of the synthetic method.
     * @param lambda The lambda expression to compile.
     * @param actualLambdaType The lambda's natural type from its expression.
     * @param effectiveLambdaType The type to generate (may differ from actual).
     * @param capturedVariables The list of captured variables that become method parameters.
     * @param context The compilation context from the enclosing scope.
     */
    private fun generateSyntheticMethod(
        methodName: String,
        lambda: TypedLambdaExpression,
        actualLambdaType: LambdaType,
        effectiveLambdaType: LambdaType,
        capturedVariables: List<CapturedVariable>,
        context: CompilationContext
    ) {
        val cw = context.classWriter
            ?: throw IllegalStateException("ClassWriter required for lambda compilation")

        val methodDescriptor = buildSyntheticMethodDescriptor(effectiveLambdaType, capturedVariables, context)

        val mv = cw.visitMethod(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_SYNTHETIC,
            methodName,
            methodDescriptor,
            null,
            null
        )

        mv.visitCode()

        val lambdaBodyScope = context.getStatementScope(lambda)
            ?: throw IllegalStateException(
                "Lambda body scope not found in scope tree. This indicates a bug in ScopeBuilder."
            )

        val lambdaParamsScope = lambdaBodyScope.parent
            ?: throw IllegalStateException(
                "Lambda params scope not found. This indicates a bug in ScopeBuilder."
            )

        for (captured in capturedVariables) {
            val varInfo = lambdaParamsScope.declareVariable(captured.name, captured.type)
            varInfo.isWrittenByLambda = captured.isRef
        }

        for ((index, param) in effectiveLambdaType.parameters.withIndex()) {
            val paramName = if (index < lambda.parameters.size) {
                lambda.parameters[index]
            } else {
                "\$unused_param_$index"
            }
            val paramType = if (index < actualLambdaType.parameters.size) {
                actualLambdaType.parameters[index].type
            } else {
                param.type
            }
            lambdaParamsScope.declareVariable(paramName, paramType)
        }

        assignLambdaSlots(lambdaParamsScope, context)

        emitParameterCoercion(lambda, actualLambdaType, effectiveLambdaType, lambdaParamsScope, mv)

        val returnTypeIndex = findReturnTypeIndex(effectiveLambdaType.returnType, context)

        val lambdaCompilationContext = context.withLambdaContext(
            functionReturnTypeIndex = returnTypeIndex,
            functionParamsScope = lambdaParamsScope
        )

        lambdaCompilationContext.enterScope(lambdaBodyScope)

        for (statement in lambda.body.body) {
            lambdaCompilationContext.compileStatement(statement, mv)
        }

        lambdaCompilationContext.exitScope()

        if (effectiveLambdaType.returnType is VoidType) {
            mv.visitInsn(Opcodes.RETURN)
        }

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    /**
     * Emits coercion for parameters from effective types to actual types.
     * 
     * Loads each ACTUAL parameter from its slot, coerces it from the effective type
     * to the actual type, and stores it back. Extra parameters are not coerced.
     * 
     * @param lambda The lambda expression (for parameter names)
     * @param actualLambdaType The lambda's natural type
     * @param effectiveLambdaType The type we're generating for
     * @param lambdaParamsScope The scope with parameter declarations
     * @param mv The method visitor for emitting bytecode
     */
    private fun emitParameterCoercion(
        lambda: TypedLambdaExpression,
        actualLambdaType: LambdaType,
        effectiveLambdaType: LambdaType,
        lambdaParamsScope: Scope,
        mv: MethodVisitor
    ) {
        for (index in lambda.parameters.indices) {
            if (index >= effectiveLambdaType.parameters.size) {
                break
            }

            val actualType = actualLambdaType.parameters[index].type
            val effectiveType = effectiveLambdaType.parameters[index].type
            val paramName = lambda.parameters[index]

            if (actualType != effectiveType) {
                val varInfo = lambdaParamsScope.lookupVariable(paramName, lambdaParamsScope.level)
                    ?: continue

                mv.visitVarInsn(ASMUtil.getLoadOpcode(effectiveType), varInfo.slotIndex)
                CoercionUtil.emitCoercion(effectiveType, actualType, mv)
                mv.visitVarInsn(ASMUtil.getStoreOpcode(actualType), varInfo.slotIndex)
            }
        }
    }

    /**
     * Assigns local variable slots for a lambda method.
     * 
     * Uses LocalVariableIndexAssigner to assign slots to all variables
     * (captured, parameters, and body locals) in the lambda scope tree.
     * Lambda methods are static, so slot assignment starts at 0.
     * 
     * @param lambdaParamsScope The lambda parameters scope containing all variables.
     * @param context The compilation context.
     */
    private fun assignLambdaSlots(
        lambdaParamsScope: Scope,
        context: CompilationContext
    ) {
        val indexAssigner = LocalVariableIndexAssigner(context)
        indexAssigner.assignIndices(lambdaParamsScope, isStatic = true)
    }

    /**
     * Builds the method descriptor for the synthetic method.
     *
     * The descriptor includes captured variables as the first parameters, followed by
     * the lambda's declared parameters, and the return type.
     *
     * @param lambdaType The lambda type containing parameter and return types.
     * @param capturedVariables The list of captured variables.
     * @param context The compilation context for converting types to descriptors.
     * @return The JVM method descriptor string.
     */
    private fun buildSyntheticMethodDescriptor(
        lambdaType: LambdaType,
        capturedVariables: List<CapturedVariable>,
        context: CompilationContext
    ): String {
        val capturedParams = capturedVariables.joinToString("") { captured ->
            if (captured.isRef) {
                "L${RefTypeUtil.getRefClassName(captured.type)};"
            } else {
                context.getTypeDescriptor(captured.type)
            }
        }
        val lambdaParams = lambdaType.parameters.joinToString("") { param ->
            context.getTypeDescriptor(param.type)
        }
        val returnDesc = context.getTypeDescriptor(lambdaType.returnType)
        return "($capturedParams$lambdaParams)$returnDesc"
    }


    /**
     * Emits an invokedynamic instruction to create the lambda instance.
     *
     * Loads captured variables onto the stack, then emits an invokedynamic instruction
     * that uses LambdaMetafactory.metafactory as the bootstrap method. The bootstrap
     * method dynamically creates a lambda instance implementing the functional interface.
     *
     * @param methodName The name of the synthetic method implementing the lambda.
     * @param lambdaType The type information for the lambda (actual type used for synthetic method).
     * @param normalizedKey The normalized lambda type key (used for interface signature).
     * @param capturedVariables The list of captured variables to pass to the lambda.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun emitInvokeDynamic(
        methodName: String,
        lambdaType: LambdaType,
        normalizedKey: LambdaType,
        capturedVariables: List<CapturedVariable>,
        context: CompilationContext,
        mv: MethodVisitor
    ) {
        for (captured in capturedVariables) {
            val loadOpcode = if (captured.isRef) {
                Opcodes.ALOAD
            } else {
                ASMUtil.getLoadOpcode(captured.type)
            }
            mv.visitVarInsn(loadOpcode, captured.outerSlotIndex)
        }

        val functionalInterface = getFunctionalInterface(lambdaType, context)
        val interfaceMethodInfo = getInterfaceMethodInfo(functionalInterface, normalizedKey, context)

        val invokeDynamicDescriptor = buildInvokeDynamicDescriptor(lambdaType, capturedVariables, context)

        val samMethodType = buildSAMMethodType(normalizedKey, context)

        val implMethodDescriptor = buildSyntheticMethodDescriptor(lambdaType, capturedVariables, context)
        val implMethodHandle = org.objectweb.asm.Handle(
            Opcodes.H_INVOKESTATIC,
            context.currentClassName,
            methodName,
            implMethodDescriptor,
            false
        )

        val instantiatedMethodType = buildInstantiatedMethodType(lambdaType, context)

        mv.visitInvokeDynamicInsn(
            interfaceMethodInfo.name,
            invokeDynamicDescriptor,
            org.objectweb.asm.Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
            ),
            samMethodType,
            implMethodHandle,
            instantiatedMethodType
        )
    }

    /**
     * Builds the invokedynamic descriptor (captured types -> functional interface).
     *
     * @param lambdaType The lambda type.
     * @param capturedVariables The list of captured variables.
     * @param context The compilation context.
     * @return The invokedynamic descriptor string.
     */
    private fun buildInvokeDynamicDescriptor(
        lambdaType: LambdaType,
        capturedVariables: List<CapturedVariable>,
        context: CompilationContext
    ): String {
        val capturedParams = capturedVariables.joinToString("") { captured ->
            if (captured.isRef) {
                "L${RefTypeUtil.getRefClassName(captured.type)};"
            } else {
                context.getTypeDescriptor(captured.type)
            }
        }
        val functionalInterface = getFunctionalInterface(lambdaType, context)
        return "($capturedParams)L$functionalInterface;"
    }

    /**
     * Builds the SAM method type for the bootstrap method.
     * Uses the actual types from the normalized key.
     *
     * @param normalizedKey The normalized lambda type key.
     * @param context The compilation context.
     * @return The Type object representing the SAM method type.
     */
    private fun buildSAMMethodType(normalizedKey: LambdaType, context: CompilationContext): org.objectweb.asm.Type {
        val descriptor = buildInterfaceMethodDescriptor(normalizedKey, context)
        return org.objectweb.asm.Type.getMethodType(descriptor)
    }

    /**
     * Builds the instantiated method type for the bootstrap method.
     *
     * @param lambdaType The actual lambda type (not normalized).
     * @param context The compilation context.
     * @return The Type object representing the instantiated method type.
     */
    private fun buildInstantiatedMethodType(
        lambdaType: LambdaType,
        context: CompilationContext
    ): org.objectweb.asm.Type {
        val descriptor = buildInterfaceMethodDescriptor(lambdaType, context)
        return org.objectweb.asm.Type.getMethodType(descriptor)
    }

    /**
     * Determines the appropriate functional interface for a lambda type.
     *
     * First checks the lambda interface registry for a matching predefined interface
     * (Func0-3, Action0-3, Predicate1). If no predefined interface matches, uses
     * the registry to generate a new interface name with simple counting (Lambda$0, Lambda$1, etc.).
     *
     * @param lambdaType The lambda type to map to a functional interface.
     * @param context The compilation context containing the interface registry.
     * @return The fully qualified internal name of the functional interface.
     */
    private fun getFunctionalInterface(lambdaType: LambdaType, context: CompilationContext): String {
        val registry = context.getLambdaInterfaceRegistry()
        val lookupResult = registry.getInterfaceForLambdaType(lambdaType)

        if (lookupResult.isNewlyGenerated) {
            val bytecode = generateFunctionalInterface(lookupResult.interfaceName, lambdaType, context)
            context.registerInterface(lookupResult.interfaceName, bytecode)
        }

        return lookupResult.interfaceName
    }

    /**
     * Generates a custom functional interface bytecode.
     * 
     * @param interfaceName The name of the interface.
     * @param lambdaType The lambda type.
     * @param context The compilation context.
     * @return The generated bytecode.
     */
    private fun generateFunctionalInterface(
        interfaceName: String,
        lambdaType: LambdaType,
        context: CompilationContext
    ): ByteArray {
        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        cw.visit(
            Opcodes.V11,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE,
            interfaceName,
            null,
            "java/lang/Object",
            null
        )

        val methodDescriptor = buildInterfaceMethodDescriptor(lambdaType, context)

        cw.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
            "call",
            methodDescriptor,
            null,
            null
        )?.visitEnd()

        cw.visitEnd()

        return cw.toByteArray()
    }

    /**
     * Builds the method descriptor for the interface's call method.
     * Uses primitive types directly without boxing.
     * 
     * @param lambdaType The lambda type.
     * @param context The compilation context.
     * @return The method descriptor (e.g., "(ID)I" for (int, double) -> int).
     */
    private fun buildInterfaceMethodDescriptor(
        lambdaType: LambdaType,
        context: CompilationContext
    ): String {
        val params = lambdaType.parameters.joinToString("") { param ->
            context.getTypeDescriptor(param.type)
        }
        val returnDesc = context.getTypeDescriptor(lambdaType.returnType)
        return "($params)$returnDesc"
    }


    /**
     * Finds the type index for a return type.
     *
     * @param returnType The return type to find the index for.
     * @param context The compilation context containing the types array.
     * @return The index of the return type in the types array, or -1 if not found.
     */
    private fun findReturnTypeIndex(returnType: ReturnType, context: CompilationContext): Int {
        for ((index, t) in context.ast.types.withIndex()) {
            if (t == returnType) {
                return index
            }
        }
        return -1
    }

    /**
     * Gets information about the interface method to implement.
     * 
     * For custom interfaces, the method is always named "call" and uses
     * primitive types directly (no boxing).
     *
     * @param functionalInterface The internal name of the functional interface.
     * @param lambdaType The lambda type.
     * @param context The compilation context.
     * @return Information about the method name, descriptor, and parameter types.
     */
    private fun getInterfaceMethodInfo(
        functionalInterface: String,
        lambdaType: LambdaType,
        context: CompilationContext
    ): InterfaceMethodInfo {
        val descriptor = buildInterfaceMethodDescriptor(lambdaType, context)
        val paramTypes = lambdaType.parameters.map { param ->
            context.getTypeDescriptor(param.type)
        }
        return InterfaceMethodInfo("call", descriptor, paramTypes)
    }


}

/**
 * Represents information about a captured variable in a lambda.
 */
private data class CapturedVariable(
    /**
     * The name of the variable.
     */
    val name: String,

    /**
     * The type of the variable.
     */
    val type: ReturnType,

    /**
     * Whether this variable is wrapped in a Ref.
     */
    val isRef: Boolean,

    /**
     * The slot index in the enclosing function where this variable is stored.
     */
    val outerSlotIndex: Int,

    /**
     * The scope level where the variable was declared.
     */
    val scopeLevel: Int
)

/**
 * Information about a functional interface method.
 */
private data class InterfaceMethodInfo(
    val name: String,
    val descriptor: String,
    val parameterTypes: List<String>
)
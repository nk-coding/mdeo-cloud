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
import com.mdeo.script.compiler.LocalVariableIndexAssigner
import com.mdeo.script.compiler.RefTypeUtil
import com.mdeo.script.compiler.Scope
import com.mdeo.script.compiler.StatementCompiler
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Represents information about a captured variable in a lambda.
 */
data class CapturedVariable(
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
 * Compiles lambda expressions to bytecode using invokedynamic.
 * 
 * Lambda expressions are compiled using the invokedynamic instruction with
 * LambdaMetafactory as the bootstrap method. Custom functional interfaces
 * (Lambda$Int$Int, etc.) are generated to avoid boxing overhead.
 * The method in interfaces is always named "call".
 */
class LambdaCompiler : ExpressionCompiler {

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
     * Compiles a lambda expression to bytecode using invokedynamic.
     *
     * This method generates a private static synthetic method in the current class,
     * analyzes captured variables, registers the method name, and emits an invokedynamic
     * instruction to create the lambda instance at runtime.
     *
     * @param expression The lambda expression to compile.
     * @param context The compilation context containing type information and scope.
     * @param mv The method visitor for emitting bytecode.
     */
    override fun compile(expression: TypedExpression, context: CompilationContext, mv: MethodVisitor) {
        val lambda = expression as TypedLambdaExpression
        val lambdaType = context.getType(lambda.evalType) as LambdaType

        val capturedVariables = analyzeCapturedVariables(lambda, context)

        val methodName = context.generateLambdaMethodName()

        generateSyntheticMethod(
            methodName,
            lambda,
            lambdaType,
            capturedVariables,
            context
        )

        emitInvokeDynamic(methodName, lambdaType, capturedVariables, context, mv)
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
        /* 
         * The lambda params scope is the parent of the body scope.
         * Variables declared at levels less than the params scope level are captured.
         */
        val lambdaParamsLevel = lambdaBodyScope.parent?.level ?: lambdaBodyScope.level
        val capturedPairs = lambdaBodyScope.collectCapturedVariables(lambdaParamsLevel)

        return capturedPairs.mapNotNull { (name, declarationLevel) ->
            /* 
             * Look up the variable in the current scope.
             * The scope tree knows where variables are declared and whether they're Ref-wrapped.
             */
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
     * This now uses the regular scope tree that was created by ScopeBuilder. It:
     * 1. Gets the lambda params scope and body scope from the scope tree
     * 2. Adds captured variables to the params scope
     * 3. Assigns slots to all variables (captured + params + locals)
     * 4. Uses the regular scope tree for all variable lookups
     *
     * @param methodName The name of the synthetic method.
     * @param lambda The lambda expression to compile.
     * @param lambdaType The type information for the lambda.
     * @param capturedVariables The list of captured variables that become method parameters.
     * @param context The compilation context from the enclosing scope.
     */
    private fun generateSyntheticMethod(
        methodName: String,
        lambda: TypedLambdaExpression,
        lambdaType: LambdaType,
        capturedVariables: List<CapturedVariable>,
        context: CompilationContext
    ) {
        val cw = context.classWriter
            ?: throw IllegalStateException("ClassWriter required for lambda compilation")

        val methodDescriptor = buildSyntheticMethodDescriptor(lambdaType, capturedVariables, context)

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

        for ((index, paramName) in lambda.parameters.withIndex()) {
            val paramType = lambdaType.parameters[index].type
            lambdaParamsScope.declareVariable(paramName, paramType)
        }


        assignLambdaSlots(lambdaParamsScope, context)

        val returnTypeIndex = findReturnTypeIndex(lambdaType.returnType, context)

        /* 
         * Create a new compilation context for the lambda method.
         * Use the lambda params scope as the function params scope.
         */
        val lambdaCompilationContext = context.withLambdaContext(
            functionReturnTypeIndex = returnTypeIndex,
            functionParamsScope = lambdaParamsScope
        )

        lambdaCompilationContext.enterScope(lambdaBodyScope)

        for (statement in lambda.body.body) {
            lambdaCompilationContext.compileStatement(statement, mv)
        }

        lambdaCompilationContext.exitScope()

        emitDefaultReturn(lambdaType.returnType, mv)

        mv.visitMaxs(0, 0)
        mv.visitEnd()
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
     * @param lambdaType The type information for the lambda.
     * @param capturedVariables The list of captured variables to pass to the lambda.
     * @param context The compilation context.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun emitInvokeDynamic(
        methodName: String,
        lambdaType: LambdaType,
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
        val interfaceMethodInfo = getInterfaceMethodInfo(functionalInterface, lambdaType, context)

        val invokeDynamicDescriptor = buildInvokeDynamicDescriptor(lambdaType, capturedVariables, context)

        val samMethodType = buildSAMMethodType(lambdaType, context)

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
     *
     * For custom interfaces, this is the same as the interface method descriptor
     * using primitive types directly (no generics, no boxing).
     *
     * @param lambdaType The lambda type.
     * @param context The compilation context.
     * @return The Type object representing the SAM method type.
     */
    private fun buildSAMMethodType(lambdaType: LambdaType, context: CompilationContext): org.objectweb.asm.Type {
        val descriptor = buildInterfaceMethodDescriptor(lambdaType, context)
        return org.objectweb.asm.Type.getMethodType(descriptor)
    }

    /**
     * Builds the instantiated method type for the bootstrap method.
     *
     * For custom interfaces without generics, this is the same as the SAM method type.
     * Uses primitive types directly without boxing.
     *
     * @param lambdaType The lambda type.
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
     * Generates custom functional interfaces to avoid boxing overhead.
     * Interface names follow the pattern: Lambda$ReturnType$ParamType1$ParamType2...
     * 
     * Examples:
     * - () => void: Lambda$Void$0
     * - () => int: Lambda$Int$0
     * - (int) => int: Lambda$Int$Int
     * - (int, double) => void: Lambda$Void$Int$Double
     *
     * @param lambdaType The lambda type to map to a functional interface.
     * @param context The compilation context containing the generated interfaces.
     * @return The fully qualified internal name of the functional interface.
     */
    private fun getFunctionalInterface(lambdaType: LambdaType, context: CompilationContext): String {
        val interfaceName = buildInterfaceName(lambdaType)

        if (!context.hasInterface(interfaceName)) {
            val bytecode = generateFunctionalInterface(interfaceName, lambdaType, context)
            context.registerInterface(interfaceName, bytecode)
        }

        return interfaceName
    }

    /**
     * Builds the interface name for a lambda type.
     * 
     * Uses the centralized CoercionUtil.getFunctionalInterfaceName to ensure
     * consistent naming across all lambda-related compilers.
     * 
     * @param lambdaType The lambda type.
     * @return The interface name (e.g., "Lambda$Int$Double").
     */
    private fun buildInterfaceName(lambdaType: LambdaType): String {
        val parameterTypes = lambdaType.parameters.map { it.type }
        return CoercionUtil.getFunctionalInterfaceName(lambdaType.returnType, parameterTypes)
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
     * Emits a default return instruction if needed.
     *
     * Generates the appropriate return instruction and default value
     * based on the return type (void, primitive, or reference).
     *
     * @param returnType The return type of the lambda.
     * @param mv The method visitor for emitting bytecode.
     */
    private fun emitDefaultReturn(returnType: ReturnType, mv: MethodVisitor) {
        when {
            returnType is VoidType -> mv.visitInsn(Opcodes.RETURN)
            returnType is ClassTypeRef -> {
                if (returnType.isNullable) {
                    mv.visitInsn(Opcodes.ACONST_NULL)
                    mv.visitInsn(Opcodes.ARETURN)
                } else {
                    when (returnType.type) {
                        "builtin.int", "builtin.boolean" -> {
                            mv.visitInsn(Opcodes.ICONST_0)
                            mv.visitInsn(Opcodes.IRETURN)
                        }

                        "builtin.long" -> {
                            mv.visitInsn(Opcodes.LCONST_0)
                            mv.visitInsn(Opcodes.LRETURN)
                        }

                        "builtin.float" -> {
                            mv.visitInsn(Opcodes.FCONST_0)
                            mv.visitInsn(Opcodes.FRETURN)
                        }

                        "builtin.double" -> {
                            mv.visitInsn(Opcodes.DCONST_0)
                            mv.visitInsn(Opcodes.DRETURN)
                        }

                        else -> {
                            mv.visitInsn(Opcodes.ACONST_NULL)
                            mv.visitInsn(Opcodes.ARETURN)
                        }
                    }
                }
            }

            else -> {
                mv.visitInsn(Opcodes.ACONST_NULL)
                mv.visitInsn(Opcodes.ARETURN)
            }
        }
    }


    /**
     * Information about a functional interface method.
     */
    private data class InterfaceMethodInfo(
        val name: String,
        val descriptor: String,
        val parameterTypes: List<String>
    )

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

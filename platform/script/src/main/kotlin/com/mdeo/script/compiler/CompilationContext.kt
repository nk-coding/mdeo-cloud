package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.VoidType
import com.mdeo.script.compiler.registry.function.FunctionRegistry
import com.mdeo.script.compiler.registry.function.GlobalFunctionRegistry
import com.mdeo.script.compiler.registry.property.GlobalPropertyRegistry
import com.mdeo.script.compiler.registry.type.TypeRegistry
import com.mdeo.script.compiler.util.ASMUtil
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

/**
 * Represents the labels for a loop, used for break and continue statements.
 *
 * @param continueLabel The label to jump to for continue (typically the condition check).
 * @param breakLabel The label to jump to for break (typically after the loop).
 */
data class LoopLabels(
    val continueLabel: Label,
    val breakLabel: Label
)

/**
 * Context for compilation, providing access to type information,
 * registered compilers, and compilation utilities.
 *
 * This context is passed to all expression and statement compilers
 * to provide shared access to compilation infrastructure.
 *
 * @param ast The TypedAST being compiled, providing access to the types array.
 * @param currentClassName The internal JVM class name being generated.
 * @param expressionCompilers Registered expression compilers for handling different expression types.
 * @param statementCompilers Registered statement compilers for handling different statement types.
 * @param functionReturnTypeIndex The index of the current function's return type in the types array.
 *                                Used for determining if boxing is needed for return statements.
 * @param functionParamsScope The function parameters scope (root scope for local variables).
 *                            For lambda bodies, this should be a LambdaBodyScope instance.
 * @param statementScopes Maps AST elements (statements, expressions) to their pre-built scopes.
 *                        Used to look up the correct scope when compiling nested structures.
 * @param classWriter The ClassWriter for the current class being compiled. Used by lambda compilation
 *                    to add synthetic methods for lambda bodies.
 * @param generatedInterfaces Shared mutable map for collecting generated functional interfaces.
 *                            All compilation contexts share the same map to avoid duplicate generation.
 * @param lambdaInterfaceRegistry Registry for looking up predefined functional interfaces (Func0-3, Action0-3, Predicate1)
 *                                or generating new interface names with simple counting when needed.
 * @param globalFunctionRegistry Registry for global scope functions.
 *                               Defaults to the global stdlib registry but can be customized per compilation.
 * @param globalPropertyRegistry Registry for global scope properties.
 *                               Defaults to the global stdlib registry but can be customized per compilation.
 * @param functionRegistry The hierarchical function registry for file-scope and imported function lookups.
 *                         Defaults to the global function registry but should be a FileFunctionRegistry
 *                         for proper cross-file import resolution.
 * @param typeRegistry The type registry for looking up type definitions (methods and properties on types).
 *                     Defaults to the global stdlib registry.
 */
class CompilationContext(
    val ast: TypedAst,
    val currentClassName: String,
    private val expressionCompilers: List<ExpressionCompiler>,
    private val statementCompilers: List<StatementCompiler>,
    val functionReturnTypeIndex: Int = -1,
    val functionParamsScope: Scope? = null,
    private val statementScopes: Map<Any, Scope> = emptyMap(),
    val classWriter: ClassWriter? = null,
    private val generatedInterfaces: MutableMap<String, ByteArray> = mutableMapOf(),
    private val lambdaCounter: LambdaCounter = LambdaCounter(),
    private val lambdaInterfaceRegistry: LambdaInterfaceRegistry = LambdaInterfaceRegistry(),
    val globalFunctionRegistry: GlobalFunctionRegistry = GlobalFunctionRegistry.GLOBAL,
    val globalPropertyRegistry: GlobalPropertyRegistry = GlobalPropertyRegistry.GLOBAL,
    val fileScopePropertyRegistry: GlobalPropertyRegistry = GlobalPropertyRegistry(),
    val functionRegistry: FunctionRegistry = globalFunctionRegistry,
    val typeRegistry: TypeRegistry = TypeRegistry.GLOBAL
) {
    /**
     * The current scope during compilation.
     * Updated as we enter/exit nested scopes (while, if, etc.).
     */
    var currentScope: Scope? = functionParamsScope
        private set
    
    /**
     * Stack of scopes for nested scope management.
     */
    private val scopeStack = mutableListOf<Scope>()
    
    /**
     * Stack of loop labels for break/continue statements.
     * Each entry represents a nested loop.
     */
    private val loopLabelStack = mutableListOf<LoopLabels>()
    
    init {
        functionParamsScope?.let { scopeStack.add(it) }
    }
    
    /**
     * Pushes loop labels onto the stack when entering a loop.
     * 
     * @param labels The labels for the loop.
     */
    fun pushLoopLabels(labels: LoopLabels) {
        loopLabelStack.add(labels)
    }
    
    /**
     * Pops loop labels from the stack when exiting a loop.
     *
     * Safely removes the most recent loop labels. If there are no labels
     * on the stack (not in a loop), this method does nothing.
     */
    fun popLoopLabels() {
        if (loopLabelStack.isNotEmpty()) {
            loopLabelStack.removeAt(loopLabelStack.lastIndex)
        }
    }
    
    /**
     * Gets the current loop labels for break/continue.
     * 
     * @return The current loop labels, or null if not in a loop.
     */
    fun getCurrentLoopLabels(): LoopLabels? {
        return loopLabelStack.lastOrNull()
    }
    
    /**
     * Generates a unique synthetic method name for a lambda.
     * Uses Java-style naming: lambda$script$N.
     * 
     * @return A unique method name like "lambda$script$0".
     */
    fun generateLambdaMethodName(): String {
        return "lambda${'$'}script${'$'}${lambdaCounter.getAndIncrement()}"
    }
    
    /**
     * Enters a new scope during compilation.
     * 
     * @param scope The scope to enter.
     */
    fun enterScope(scope: Scope) {
        scopeStack.add(scope)
        currentScope = scope
    }
    
    /**
     * Gets the pre-built scope for an AST element (e.g., while, for, if statement).
     * 
     * @param element The AST element.
     * @return The pre-built scope, or null if not found.
     */
    fun getStatementScope(element: Any): Scope? {
        return statementScopes[element]
    }
    
    /**
     * Exits the current scope, returning to the parent scope.
     *
     * Safely removes the current scope from the stack and updates the
     * currentScope reference. If the scope stack is empty (no scope to exit),
     * this method does nothing.
     */
    fun exitScope() {
        if (scopeStack.isNotEmpty()) {
            scopeStack.removeAt(scopeStack.lastIndex)
            currentScope = scopeStack.lastOrNull()
        }
    }
    /**
     * Gets the current function's return type.
     * 
     * @return The return type, or null if not in a function context.
     */
    fun getFunctionReturnType(): ReturnType? {
        return if (functionReturnTypeIndex >= 0) ast.types[functionReturnTypeIndex] else null
    }
    
    /**
     * Gets the return type for a given type index.
     * 
     * @param typeIndex The index into the types array.
     * @return The ReturnType at that index.
     */
    fun getType(typeIndex: Int): ReturnType = ast.types[typeIndex]
    
    /**
     * Checks if a type is void.
     * 
     * @param typeIndex The index into the types array.
     * @return true if the type is void.
     */
    fun isVoid(typeIndex: Int): Boolean = ast.types[typeIndex] is VoidType
    
    /**
     * Gets the JVM type descriptor for a return type.
     * 
     * Delegates to ASMUtil for consistency across the compiler.
     * 
     * @param returnType The return type to get the descriptor for.
     * @return The JVM type descriptor string.
     */
    fun getTypeDescriptor(returnType: ReturnType): String {
        return ASMUtil.getTypeDescriptor(returnType)
    }
    
    /**
     * Registers a generated functional interface.
     * 
     * @param interfaceName The internal name of the interface (e.g., "Lambda$Int$Int").
     * @param bytecode The generated bytecode for the interface.
     */
    fun registerInterface(interfaceName: String, bytecode: ByteArray) {
        generatedInterfaces[interfaceName] = bytecode
    }
    
    /**
     * Checks if an interface has already been generated.
     * 
     * @param interfaceName The internal name of the interface.
     * @return True if the interface has been generated, false otherwise.
     */
    fun hasInterface(interfaceName: String): Boolean {
        return generatedInterfaces.containsKey(interfaceName)
    }
    
    /**
     * Gets all generated interfaces.
     * 
     * @return A map of interface names to their bytecode.
     */
    fun getGeneratedInterfaces(): Map<String, ByteArray> {
        return generatedInterfaces.toMap()
    }
    
    /**
     * Gets the lambda interface registry for looking up predefined or generating new interfaces.
     *
     * @return The lambda interface registry.
     */
    fun getLambdaInterfaceRegistry(): LambdaInterfaceRegistry {
        return lambdaInterfaceRegistry
    }
    
    /**
     * Creates a new compilation context for a lambda body with modified return type and params scope.
     * 
     * This method creates a copy of the current context, replacing only the fields that need to change
     * for lambda compilation while sharing all other resources (compilers, registries, interfaces, etc.).
     * 
     * @param functionReturnTypeIndex The return type index for the lambda method.
     * @param functionParamsScope The scope containing lambda parameters and captured variables.
     * @return A new CompilationContext configured for lambda compilation.
     */
    fun withLambdaContext(
        functionReturnTypeIndex: Int,
        functionParamsScope: Scope
    ): CompilationContext {
        return CompilationContext(
            ast = ast,
            currentClassName = currentClassName,
            expressionCompilers = expressionCompilers,
            statementCompilers = statementCompilers,
            functionReturnTypeIndex = functionReturnTypeIndex,
            functionParamsScope = functionParamsScope,
            statementScopes = statementScopes,
            classWriter = classWriter,
            generatedInterfaces = generatedInterfaces,
            lambdaCounter = lambdaCounter,
            lambdaInterfaceRegistry = lambdaInterfaceRegistry,
            globalFunctionRegistry = globalFunctionRegistry,
            globalPropertyRegistry = globalPropertyRegistry,
            fileScopePropertyRegistry = fileScopePropertyRegistry,
            functionRegistry = functionRegistry,
            typeRegistry = typeRegistry
        )
    }
    
    /**
     * Compiles an expression using the appropriate expression compiler.
     * 
     * @param expression The expression to compile.
     * @param mv The method visitor to emit bytecode to.
     * @param expectedType The type the expression result should be coerced to.
     * @throws CompilationException if no compiler can handle the expression.
     */
    fun compileExpression(expression: TypedExpression, mv: MethodVisitor, expectedType: ReturnType) {
        val compiler = expressionCompilers.find { it.canCompile(expression) }
            ?: throw CompilationException("No compiler found for expression: ${expression.kind}")
        compiler.compile(expression, this, mv, expectedType)
    }
    
    /**
     * Compiles a statement using the appropriate statement compiler.
     * 
     * @param statement The statement to compile.
     * @param mv The method visitor to emit bytecode to.
     * @throws CompilationException if no compiler can handle the statement.
     */
    fun compileStatement(statement: TypedStatement, mv: MethodVisitor) {
        val compiler = statementCompilers.find { it.canCompile(statement) }
            ?: throw CompilationException("No compiler found for statement: ${statement.kind}")
        compiler.compile(statement, this, mv)
    }

    /**
     * Tracks the next free JVM local-variable slot available for temporary use.
     * Initialised lazily from the scope tree on first call to [allocateTempSlot].
     */
    private var nextTempSlot: Int = -1

    /**
     * Allocates one or more consecutive JVM local-variable slots for temporary storage.
     *
     * Slots are allocated beyond all scope-declared variables. The counter only ever
     * increases, so nested [ExtensionCallCompiler][com.mdeo.script.compiler.expressions.ExtensionCallCompiler]
     * invocations receive non-overlapping slots while outer temporaries remain live.
     *
     * @param slotsNeeded Number of consecutive slots to allocate (2 for `long`/`double`,
     *                    1 for everything else).
     * @return The index of the first allocated slot.
     */
    fun allocateTempSlot(slotsNeeded: Int = 1): Int {
        if (nextTempSlot < 0) {
            nextTempSlot = computeNextFreeSlot()
        }
        val slot = nextTempSlot
        nextTempSlot += slotsNeeded
        return slot
    }

    /**
     * Computes the first JVM local-variable slot that is not yet claimed by any
     * scope-declared variable in the current method.
     *
     * Walks the entire statically-nested scope tree starting from
     * [functionParamsScope]. Lambda sub-scopes ([isStaticallyNested] = false) are
     * excluded because they are compiled as separate JVM methods with their own local
     * variable tables.
     */
    private fun computeNextFreeSlot(): Int {
        var max = 0
        fun walk(scope: Scope) {
            for (info in scope.declaredVariables.values) {
                if (info.slotIndex >= 0) {
                    val slotEnd = info.slotIndex + ASMUtil.getSlotsForType(info.type, info.isWrittenByLambda)
                    if (slotEnd > max) max = slotEnd
                }
            }
            for (child in scope.children) {
                if (child.isStaticallyNested) walk(child)
            }
        }
        functionParamsScope?.let { walk(it) }
        return maxOf(max, 1)
    }
}

/**
 * Exception thrown when compilation fails.
 */
class CompilationException(message: String) : RuntimeException(message)

/**
 * Mutable counter for generating unique lambda method names.
 * This is shared across compilation contexts to ensure uniqueness across nested lambdas.
 */
class LambdaCounter {
    private var counter: Int = 0
    
    /**
     * Gets the current counter value and increments it.
     * 
     * @return The counter value before incrementing.
     */
    fun getAndIncrement(): Int = counter++
}

package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedAst
import com.mdeo.script.ast.expressions.TypedExpression
import com.mdeo.script.ast.statements.TypedStatement
import com.mdeo.script.ast.types.ClassTypeRef
import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.VoidType
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
 * @param lambdaContext Context for lambda compilation, tracks generated lambda method names.
 *                      May be null if lambda compilation is not enabled for this context.
 * @param classWriter The ClassWriter for the current class being compiled. Used by lambda compilation
 *                    to add synthetic methods for lambda bodies. May be null for lambda body contexts
 *                    which inherit the parent's class writer via lambdaContext.
 * @param generatedInterfaces Shared mutable map for collecting generated functional interfaces.
 *                            All compilation contexts share the same map to avoid duplicate generation.
 */
class CompilationContext(
    val ast: TypedAst,
    val currentClassName: String,
    private val expressionCompilers: List<ExpressionCompiler>,
    private val statementCompilers: List<StatementCompiler>,
    val functionReturnTypeIndex: Int = -1,
    val functionParamsScope: Scope? = null,
    private val statementScopes: Map<Any, Scope> = emptyMap(),
    val lambdaContext: LambdaCompilationContext? = null,
    val classWriter: ClassWriter? = null,
    private val generatedInterfaces: MutableMap<String, ByteArray> = mutableMapOf()
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
     * Gets all pre-built statement scopes.
     * 
     * This is used when creating child compilation contexts (e.g., for lambda bodies)
     * that need to access the same scope tree as the parent context.
     * 
     * @return The map of AST elements to their pre-built scopes.
     */
    fun getStatementScopes(): Map<Any, Scope> {
        return statementScopes
    }
    
    /**
     * Gets the shared generated interfaces map for child contexts.
     * 
     * This is used when creating child compilation contexts (e.g., for lambda bodies)
     * that need to share the same generated interfaces map as the parent context.
     * 
     * @return The mutable map of generated interfaces.
     */
    fun getSharedInterfaces(): MutableMap<String, ByteArray> {
        return generatedInterfaces
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
     * Compiles an expression using the appropriate expression compiler.
     * 
     * @param expression The expression to compile.
     * @param mv The method visitor to emit bytecode to.
     * @throws CompilationException if no compiler can handle the expression.
     */
    fun compileExpression(expression: TypedExpression, mv: MethodVisitor) {
        val compiler = expressionCompilers.find { it.canCompile(expression) }
            ?: throw CompilationException("No compiler found for expression: ${expression.kind}")
        compiler.compile(expression, this, mv)
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
}

/**
 * Exception thrown when compilation fails.
 */
class CompilationException(message: String) : RuntimeException(message)

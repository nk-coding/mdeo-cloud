package com.mdeo.script.compiler

import com.mdeo.script.ast.TypedFunction
import com.mdeo.expression.ast.expressions.TypedBinaryExpression
import com.mdeo.expression.ast.expressions.TypedCallExpression
import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.expression.ast.expressions.TypedIdentifierExpression
import com.mdeo.script.ast.expressions.TypedLambdaExpression
import com.mdeo.expression.ast.expressions.TypedMemberAccessExpression
import com.mdeo.expression.ast.expressions.TypedMemberCallExpression
import com.mdeo.expression.ast.expressions.TypedTernaryExpression
import com.mdeo.expression.ast.expressions.TypedUnaryExpression
import com.mdeo.expression.ast.statements.TypedAssignmentStatement
import com.mdeo.expression.ast.statements.TypedExpressionStatement
import com.mdeo.expression.ast.statements.TypedForStatement
import com.mdeo.expression.ast.statements.TypedIfStatement
import com.mdeo.expression.ast.statements.TypedReturnStatement
import com.mdeo.expression.ast.statements.TypedStatement
import com.mdeo.expression.ast.statements.TypedVariableDeclarationStatement
import com.mdeo.expression.ast.statements.TypedWhileStatement
import com.mdeo.expression.ast.types.ClassTypeRef
import com.mdeo.expression.ast.types.LambdaType
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ValueType
import com.mdeo.script.compiler.util.ASMUtil
import java.util.LinkedHashMap

/**
 * Represents a variable declared in a scope.
 * Contains all information needed for code generation.
 *
 * @param name The name of the variable.
 * @param type The type of the variable (ReturnType).
 * @param slotIndex The JVM local variable slot index.
 * @param isWrittenByLambda Whether this variable is written by a non-static inner scope (lambda).
 *                          If true, the variable needs to be wrapped in a Ref type.
 */
data class VariableInfo(
    val name: String,
    val type: ReturnType,
    var slotIndex: Int = -1,
    var isWrittenByLambda: Boolean = false
)

/**
 * Represents a scope in the program.
 *
 * Scope levels:
 * - Level 0: Global scope
 * - Level 1: File scope
 * - Level 2: Function parameters scope
 * - Level 3: Function body scope
 * - Level 4+: Nested scopes (while, if, for body, etc.)
 *
 * Functions and lambdas create TWO scopes: one for parameters (level N)
 * and one for the body (level N+1).
 *
 * @param level The scope level (depth). 0 = global, 1 = file, 2 = function params, etc.
 * @param parent The parent scope, or null for the root scope.
 * @param isStaticallyNested Whether this scope is statically nested.
 *                           Lambdas are NOT statically nested - they create a new JVM scope.
 *                           All other scopes (while, if, for body) ARE statically nested.
 */
class Scope(
    val level: Int,
    val parent: Scope? = null,
    val isStaticallyNested: Boolean = true
) {
    /**
     * Child scopes created within this scope.
     */
    val children: MutableList<Scope> = mutableListOf()

    /**
     * Variables declared in this scope.
     * Maps variable name to variable info.
     */
    val declaredVariables: MutableMap<String, VariableInfo> = LinkedHashMap()

    /**
     * Variables written (assigned) in this scope.
     * Stores pairs of (name, scope level where declared).
     */
    val writtenVariables: MutableSet<Pair<String, Int>> = mutableSetOf()

    /**
     * Variables read (accessed) in this scope.
     * Stores pairs of (name, scope level where declared).
     * This is only for variables accessed directly in this scope, not in child scopes.
     */
    val readVariables: MutableSet<Pair<String, Int>> = mutableSetOf()

    init {
        parent?.children?.add(this)
    }

    /**
     * Creates a child scope.
     *
     * @param isStaticallyNested Whether the new scope is statically nested.
     * @return The new child scope.
     */
    fun createChild(isStaticallyNested: Boolean = true): Scope {
        return Scope(
            level = level + 1,
            parent = this,
            isStaticallyNested = isStaticallyNested
        )
    }

    /**
     * Declares a variable in this scope.
     * 
     * @param name The variable name.
     * @param type The type of the variable.
     * @return The created VariableInfo.
     */
    fun declareVariable(name: String, type: ReturnType): VariableInfo {
        val info = VariableInfo(name, type)
        declaredVariables[name] = info
        return info
    }

    /**
     * Records that a variable is written in this scope.
     * 
     * @param name The variable name.
     * @param declarationLevel The scope level where the variable was declared.
     */
    fun recordWrite(name: String, declarationLevel: Int) {
        writtenVariables.add(name to declarationLevel)
    }

    /**
     * Records that a variable is read (accessed) in this scope.
     * 
     * @param name The variable name.
     * @param declarationLevel The scope level where the variable was declared.
     */
    fun recordRead(name: String, declarationLevel: Int) {
        readVariables.add(name to declarationLevel)
    }

    /**
     * Looks up a variable by name and the specific scope level where it was declared.
     * 
     * @param name The variable name.
     * @param scopeLevel The scope level where the variable was declared.
     * @return The VariableInfo if found, null otherwise.
     */
    fun lookupVariable(name: String, scopeLevel: Int): VariableInfo? {
        if (level == scopeLevel) {
            return declaredVariables[name]
        }
        return parent?.lookupVariable(name, scopeLevel)
    }

    /**
     * Looks up a variable by name, returning the nearest declaration found traversing
     * upward from this scope up to and including [declarationLevel].
     *
     * Unlike [lookupVariable], which requires an exact scope-level match, this method
     * returns the first occurrence of [name] found going toward the root. This is
     * necessary when looking up captured variables inside a lambda body: a captured
     * variable is re-declared in the lambda's own parameters scope (at the lambda's
     * level) rather than at its original [declarationLevel], so an exact-level lookup
     * would skip the re-declaration and return the stale slot index from the original
     * outer-function scope.
     *
     * @param name The variable name to search for.
     * @param declarationLevel The scope level at which the variable was originally
     *                         declared; used as an upper bound for the upward walk.
     * @return The nearest [VariableInfo] found at or between the current scope and
     *         [declarationLevel], or null if not found.
     */
    fun lookupVariableNearestUpTo(name: String, declarationLevel: Int): VariableInfo? {
        val v = declaredVariables[name]
        if (v != null) {
            return v
        }
        if (level <= declarationLevel) {
            return null
        }
        return parent?.lookupVariableNearestUpTo(name, declarationLevel)
    }

    /**
     * Looks up a variable by name, respecting lambda-boundary re-declarations.
     *
     * Walks upward from this scope toward [declarationLevel].  For each scope:
     * - If this scope is a lambda-params boundary ([isStaticallyNested] == false) **and**
     *   [name] is declared here, return it immediately.  Lambda compilation re-declares
     *   captured variables in the lambda-params scope with the correct JVM slot index for
     *   that synthetic method; this path is the authoritative slot for use inside the lambda.
     * - If [name] is declared at the exact [declarationLevel], return it (the original
     *   declaration in any enclosing scope).
     * - Statically-nested intermediate scopes (while/if/for bodies) that happen to shadow
     *   [name] at a level != [declarationLevel] are **skipped**, so the caller gets the
     *   variable they actually asked for.
     *
     * This differs from [lookupVariableNearestUpTo] (which always short-circuits on the
     * first matching declaration regardless of nesting kind) in that it ignores shadowing
     * declarations inside statically-nested child scopes, preserving correct behaviour for
     * code like:
     * ```
     * var x = 100          // scope 3
     * while (...) {
     *     var x = 10       // scope 4  — shadows, but identifier("x", scope=3) still wants slot at 3
     * }
     * ```
     *
     * @param name The variable name to search for.
     * @param declarationLevel The scope level at which the variable was originally declared.
     * @return The [VariableInfo] for the innermost lambda-boundary re-declaration, or the
     *         original declaration at [declarationLevel], or null if not found.
     */
    fun lookupVariableRespectingLambdaBoundary(name: String, declarationLevel: Int): VariableInfo? {
        val v = declaredVariables[name]
        if (v != null && !isStaticallyNested) {
            return v
        }
        if (v != null && level == declarationLevel) {
            return v
        }
        if (level <= declarationLevel) {
            return null
        }
        return parent?.lookupVariableRespectingLambdaBoundary(name, declarationLevel)
    }

    /**
     * Finds the scope at a specific level by traversing up.
     * 
     * @param targetLevel The target scope level.
     * @return The scope at that level, or null if not found.
     */
    fun getScopeAtLevel(targetLevel: Int): Scope? {
        return when {
            level == targetLevel -> this
            level > targetLevel -> parent?.getScopeAtLevel(targetLevel)
            else -> null
        }
    }

    /**
     * Checks if this scope or any ancestor is a lambda scope.
     * Used to determine if a variable write crosses a lambda boundary.
     * 
     * @param upToLevel Stop checking at this level (exclusive).
     * @return true if there's a lambda scope between this scope and the target level.
     */
    fun hasLambdaScopeBetween(upToLevel: Int): Boolean {
        if (level <= upToLevel) {
            return false
        }
        if (!isStaticallyNested) {
            return true
        }
        return parent?.hasLambdaScopeBetween(upToLevel) ?: false
    }

    /**
     * Finds the nearest non-statically-nested scope (lambda boundary) in the ancestor chain.
     * 
     * @return The nearest lambda scope, or null if no lambda boundary exists.
     */
    fun findNearestLambdaScope(): Scope? {
        if (!isStaticallyNested) {
            return this
        }
        return parent?.findNearestLambdaScope()
    }

    /**
     * Collects all variables that are captured by this scope from outer scopes.
     * 
     * A variable is captured if:
     * 1. It is read or written in this scope or any of its child scopes
     * 2. It was declared in a scope level outside the lambda boundary
     * 
     * This method walks the scope tree and collects all variable accesses
     * that refer to variables declared outside the lambda's parameter scope.
     * 
     * @param lambdaParamsLevel The level of the lambda's parameters scope.
     *                          Variables declared at or above this level are captured.
     * @return A set of pairs (variable name, declaration scope level) for all captured variables.
     */
    fun collectCapturedVariables(lambdaParamsLevel: Int): Set<Pair<String, Int>> {
        val captured = mutableSetOf<Pair<String, Int>>()
        collectCapturedVariablesRecursive(lambdaParamsLevel, captured)
        return captured
    }

    /**
     * Recursively collects captured variables from this scope and its children.
     * 
     * @param lambdaParamsLevel The level of the lambda's parameters scope.
     * @param captured The mutable set to populate with captured variables.
     */
    private fun collectCapturedVariablesRecursive(
        lambdaParamsLevel: Int,
        captured: MutableSet<Pair<String, Int>>
    ) {
        /* 
         * Add all reads and writes from this scope where the variable 
         * was declared outside the lambda boundary.
         */
        for ((name, declarationLevel) in readVariables) {
            if (declarationLevel < lambdaParamsLevel) {
                captured.add(name to declarationLevel)
            }
        }
        for ((name, declarationLevel) in writtenVariables) {
            if (declarationLevel < lambdaParamsLevel) {
                captured.add(name to declarationLevel)
            }
        }

        /* Recursively collect from child scopes. */
        for (child in children) {
            child.collectCapturedVariablesRecursive(lambdaParamsLevel, captured)
        }
    }
}

/**
 * Builds a scope tree for a function.
 * Analyzes variable declarations and writes to determine which variables
 * need to be wrapped in Ref types.
 *
 * @param context The compilation context with type information.
 */
class ScopeBuilder(
    private val context: CompilationContext
) {
    /**
     * Maps statements to their scopes.
     * Used during compilation to find the correct scope for a statement.
     */
    val statementScopes: MutableMap<Any, Scope> = mutableMapOf()

    /**
     * Builds the complete scope tree for a function.
     * This includes the parameters scope and the body scope.
     * 
     * @param function The function to analyze.
     * @param paramsScope The pre-created scope for function parameters.
     * @return The body scope (child of params scope).
     */
    fun buildFunctionScope(function: TypedFunction, paramsScope: Scope): Scope {
        val bodyScope = paramsScope.createChild()
        statementScopes[function] = bodyScope

        for (statement in function.body.body) {
            collectFromStatement(statement, bodyScope)
        }

        analyzeWrittenVariables(paramsScope)

        return bodyScope
    }

    /**
     * Collects variable declarations and writes from a statement.
     * Recursively processes nested statements and expressions.
     *
     * @param statement The statement to analyze.
     * @param scope The current scope context.
     */
    private fun collectFromStatement(statement: TypedStatement, scope: Scope) {
        when (statement) {
            is TypedVariableDeclarationStatement -> {
                val varType = context.getType(statement.type)
                scope.declareVariable(statement.name, varType)
                statement.initialValue?.let { collectFromExpression(it, scope) }
            }

            is TypedAssignmentStatement -> {
                collectWriteTarget(statement.left, scope)
                collectFromExpression(statement.right, scope)
            }

            is TypedWhileStatement -> {
                collectFromExpression(statement.condition, scope)
                val whileScope = scope.createChild()
                statementScopes[statement] = whileScope
                for (bodyStmt in statement.body) {
                    collectFromStatement(bodyStmt, whileScope)
                }
            }

            is TypedIfStatement -> {
                collectFromExpression(statement.condition, scope)
                val thenScope = scope.createChild()
                statementScopes[statement] = thenScope
                for (thenStmt in statement.thenBlock) {
                    collectFromStatement(thenStmt, thenScope)
                }
                for (elseIf in statement.elseIfs) {
                    collectFromExpression(elseIf.condition, scope)
                    val elseIfScope = scope.createChild()
                    statementScopes[elseIf] = elseIfScope
                    for (elseIfStmt in elseIf.thenBlock) {
                        collectFromStatement(elseIfStmt, elseIfScope)
                    }
                }
                statement.elseBlock?.let { elseBlock ->
                    val elseScope = scope.createChild()
                    statementScopes[elseBlock] = elseScope
                    for (elseStmt in elseBlock) {
                        collectFromStatement(elseStmt, elseScope)
                    }
                }
            }

            is TypedForStatement -> {
                val forParamsScope = scope.createChild()
                val varType = context.getType(statement.variableType)
                forParamsScope.declareVariable(statement.variableName, varType)
                val iteratorType = ClassTypeRef("java.util", "Iterator", false)
                forParamsScope.declareVariable("\$iterator\$${statement.variableName}", iteratorType)
                collectFromExpression(statement.iterable, scope)
                val forBodyScope = forParamsScope.createChild()
                statementScopes[statement] = forBodyScope
                for (bodyStmt in statement.body) {
                    collectFromStatement(bodyStmt, forBodyScope)
                }
            }

            else -> {
                collectFromStatementExpressions(statement, scope)
            }
        }
    }

    /**
     * Collects expressions from other statement types.
     * Handles return statements and expression statements.
     *
     * @param statement The statement to analyze.
     * @param scope The current scope context.
     */
    private fun collectFromStatementExpressions(statement: TypedStatement, scope: Scope) {
        when (statement) {
            is TypedReturnStatement -> {
                statement.value?.let { collectFromExpression(it, scope) }
            }

            is TypedExpressionStatement -> {
                collectFromExpression(statement.expression, scope)
            }

            else -> {}
        }
    }

    /**
     * Records a write to a variable from an assignment target.
     * Only processes identifier expressions as valid write targets.
     *
     * @param expression The expression representing the assignment target.
     * @param scope The current scope context.
     */
    private fun collectWriteTarget(expression: TypedExpression, scope: Scope) {
        if (expression is TypedIdentifierExpression) {
            scope.recordWrite(expression.name, expression.scope)
        }
    }

    /**
     * Collects variable references from an expression.
     * Recursively processes nested expressions and creates lambda scopes.
     *
     * @param expression The expression to analyze.
     * @param scope The current scope context.
     */
    private fun collectFromExpression(expression: TypedExpression, scope: Scope) {
        when (expression) {
            is TypedIdentifierExpression -> {
                scope.recordRead(expression.name, expression.scope)
            }

            is TypedLambdaExpression -> {
                val lambdaParamsScope = scope.createChild(isStaticallyNested = false)
                
                val lambdaType = context.getType(expression.evalType) as? LambdaType

                val lambdaBodyScope = lambdaParamsScope.createChild()
                statementScopes[expression] = lambdaBodyScope
                for (bodyStmt in expression.body.body) {
                    collectFromStatement(bodyStmt, lambdaBodyScope)
                }
            }

            is TypedBinaryExpression -> {
                collectFromExpression(expression.left, scope)
                collectFromExpression(expression.right, scope)
            }

            is TypedUnaryExpression -> {
                collectFromExpression(expression.expression, scope)
            }

            is TypedMemberCallExpression -> {
                collectFromExpression(expression.expression, scope)
                for (arg in expression.arguments) {
                    collectFromExpression(arg.value, scope)
                }
            }

            is TypedCallExpression -> {
                for (arg in expression.arguments) {
                    collectFromExpression(arg.value, scope)
                }
            }

            is TypedMemberAccessExpression -> {
                collectFromExpression(expression.expression, scope)
            }

            is TypedTernaryExpression -> {
                collectFromExpression(expression.condition, scope)
                collectFromExpression(expression.trueExpression, scope)
                collectFromExpression(expression.falseExpression, scope)
            }

            else -> {}
        }
    }

    /**
     * Analyzes the scope tree to mark variables written by lambdas.
     * Entry point for the recursive analysis.
     *
     * @param rootScope The root scope to start analysis from.
     */
    private fun analyzeWrittenVariables(rootScope: Scope) {
        analyzeWrittenVariablesInScope(rootScope)
    }

    /**
     * Recursively analyzes a scope and its children for lambda writes.
     * Marks variables as written by lambda if the write crosses a lambda boundary.
     *
     * @param scope The scope to analyze.
     */
    private fun analyzeWrittenVariablesInScope(scope: Scope) {
        for ((varName, declarationLevel) in scope.writtenVariables) {
            if (scope.hasLambdaScopeBetween(declarationLevel)) {
                val variable = scope.lookupVariable(varName, declarationLevel)
                variable?.isWrittenByLambda = true
            }
        }

        for (child in scope.children) {
            analyzeWrittenVariablesInScope(child)
        }
    }

    /**
     * Finds the type index for a given ValueType in the context's types array.
     *
     * @param type The value type to find.
     * @param context The compilation context.
     * @return The type index, or -1 if not found.
     */
    private fun findTypeIndex(type: ValueType, context: CompilationContext): Int {
        for ((index, t) in context.ast.types.withIndex()) {
            if (t == type) {
                return index
            }
        }
        return -1
    }
}

/**
 * Assigns JVM local variable indices to variables in a function.
 * Handles index reuse when scopes end and accounts for long/double taking 2 slots.
 */
class LocalVariableIndexAssigner(
    /**
     * The compilation context with type information.
     */
    private val context: CompilationContext
) {
    /**
     * The current next available slot index.
     */
    private var nextSlot: Int = 0

    /**
     * The maximum slot index ever reached (high-water mark).
     * This is needed because sibling scopes reuse slots, but we need to track
     * the maximum slots used across all branches for proper JVM method frame sizing.
     */
    private var maxSlotUsed: Int = 0

    /**
     * Stack of slot indices to restore when leaving scopes.
     */
    private val slotStack: MutableList<Int> = mutableListOf()

    /**
     * Assigns indices to all variables in the function scope tree.
     * For static methods, slot 0 is available (no 'this').
     * 
     * @param paramsScope The function parameters scope.
     * @param isStatic Whether this is a static method (slot 0 available).
     */
    fun assignIndices(paramsScope: Scope, isStatic: Boolean = true) {
        nextSlot = if (isStatic) 0 else 1
        maxSlotUsed = nextSlot
        assignIndicesInScope(paramsScope)
    }

    /**
     * Assigns indices in a scope and recursively in its children.
     *
     * After allocating this scope's variables, updates the high-water mark.
     * Remembers the slot position after this scope's variables so that sibling
     * child scopes can start from the same slot position (slot reuse).
     * When processing sibling scopes, resets to the slot after this scope's
     * variables, allowing sibling scopes to reuse the same slots.
     *
     * @param scope The scope to assign indices for.
     */
    private fun assignIndicesInScope(scope: Scope) {
        val scopeStartSlot = nextSlot
        slotStack.add(scopeStartSlot)

        for ((_, variable) in scope.declaredVariables) {
            variable.slotIndex = nextSlot
            nextSlot += ASMUtil.getSlotsForType(variable.type, variable.isWrittenByLambda)
        }

        if (nextSlot > maxSlotUsed) {
            maxSlotUsed = nextSlot
        }

        val slotAfterScopeVariables = nextSlot

        for (child in scope.children) {
            if (child.isStaticallyNested) {
                assignIndicesInScope(child)
                nextSlot = slotAfterScopeVariables
            }
        }

        slotStack.removeAt(slotStack.lastIndex)
    }



    /**
     * Gets the maximum number of local variable slots used.
     * This tracks the high-water mark across all scopes, including
     * sibling scopes that reuse slots.
     */
    fun getMaxLocals(): Int = maxSlotUsed
}

/**
 * Provides utility functions for working with Ref types.
 */
object RefTypeUtil {

    /**
     * Gets the internal class name for the Ref type for a given type.
     * 
     * @param type The type to wrap.
     * @return The internal class name of the Ref type.
     */
    fun getRefClassName(type: ReturnType): String {
        if (type is ClassTypeRef && type.`package` == "builtin") {
            return when (type.type) {
                "int" -> "com/mdeo/script/runtime/refs/IntRef"
                "long" -> "com/mdeo/script/runtime/refs/LongRef"
                "float" -> "com/mdeo/script/runtime/refs/FloatRef"
                "double" -> "com/mdeo/script/runtime/refs/DoubleRef"
                else -> "com/mdeo/script/runtime/refs/ObjectRef"
            }
        }
        return "com/mdeo/script/runtime/refs/ObjectRef"
    }

    /**
     * Gets the value field descriptor for a Ref type.
     * 
     * @param type The wrapped type.
     * @return The field descriptor.
     */
    fun getRefValueDescriptor(type: ReturnType): String {
        if (type is ClassTypeRef && type.`package` == "builtin") {
            return when (type.type) {
                "int" -> "I"
                "long" -> "J"
                "float" -> "F"
                "double" -> "D"
                else -> "Ljava/lang/Object;"
            }
        }
        return "Ljava/lang/Object;"
    }
}

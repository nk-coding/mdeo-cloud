package com.mdeo.modeltransformation.compiler

import com.mdeo.expression.ast.expressions.TypedExpression
import com.mdeo.modeltransformation.compiler.expressions.AssertNonNullCompiler
import com.mdeo.modeltransformation.compiler.expressions.BinaryOperatorCompiler
import com.mdeo.modeltransformation.compiler.expressions.BooleanLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.DoubleLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.FloatLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.IdentifierCompiler
import com.mdeo.modeltransformation.compiler.expressions.IntLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.LambdaCompiler
import com.mdeo.modeltransformation.compiler.expressions.ListLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.LongLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.MemberAccessCompiler
import com.mdeo.modeltransformation.compiler.expressions.MemberCallCompiler
import com.mdeo.modeltransformation.compiler.expressions.NullLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.StringLiteralCompiler
import com.mdeo.modeltransformation.compiler.expressions.TernaryExpressionCompiler
import com.mdeo.modeltransformation.compiler.expressions.TypeCastCompiler
import com.mdeo.modeltransformation.compiler.expressions.TypeCheckCompiler
import com.mdeo.modeltransformation.compiler.expressions.UnaryOperatorCompiler
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal

/**
 * Registry for traversal-based expression compilers.
 *
 * This registry manages [ExpressionCompiler] instances and provides
 * centralized compilation dispatch. All expressions compile to [TraversalCompilationResult],
 * ensuring uniform handling across the compilation infrastructure.
 *
 * ## Compiler Registration
 * Compilers are checked in registration order when finding a compiler for an
 * expression. Register more specific compilers before more general ones to
 * ensure proper dispatch.
 *
 * ## Initial Traversal Propagation
 * The [compile] method accepts an optional [initialTraversal] parameter.
 * This is passed to the compiler for expressions that need to be prefixed
 * (e.g., in match contexts where `__.as()` is required).
 *
 * Example usage:
 * ```kotlin
 * val registry = ExpressionCompilerRegistry()
 * registry.register(IntLiteralCompiler())
 * registry.register(StringLiteralCompiler())
 * registry.register(BinaryExpressionTraversalCompiler(registry))
 *
 * // Normal compilation
 * val result = registry.compile(expression, context)
 *
 * // Compilation with initial traversal (for match contexts)
 * val matchResult = registry.compile(expression, context, __.as("a"))
 * ```
 *
 * @see ExpressionCompiler
 * @see TraversalCompilationResult
 * @see TraversalCompilationContext
 */
class ExpressionCompilerRegistry {

    private val compilers: MutableList<ExpressionCompiler> = mutableListOf()

    /**
     * Registers an expression compiler with this registry.
     *
     * Compilers are checked in registration order when finding a compiler
     * for an expression. Register more specific compilers before more
     * general ones to ensure proper dispatch.
     *
     * @param compiler The compiler to register
     * @return This registry, for method chaining
     */
    fun register(compiler: ExpressionCompiler): ExpressionCompilerRegistry {
        compilers.add(compiler)
        return this
    }

    /**
     * Registers multiple expression compilers with this registry.
     *
     * Convenience method for registering multiple compilers at once.
     * Compilers are registered in the order they appear in the vararg.
     *
     * @param compilersToRegister The compilers to register
     * @return This registry, for method chaining
     */
    fun registerAll(vararg compilersToRegister: ExpressionCompiler): ExpressionCompilerRegistry {
        compilers.addAll(compilersToRegister)
        return this
    }

    /**
     * Finds a compiler that can handle the given expression.
     *
     * Searches through registered compilers in order and returns the first
     * one that returns true from [ExpressionCompiler.canCompile].
     *
     * @param expression The expression to find a compiler for
     * @return The compiler that can handle the expression, or null if none found
     */
    fun findCompiler(expression: TypedExpression): ExpressionCompiler? {
        return compilers.find { it.canCompile(expression) }
    }

    /**
     * Compiles an expression using the appropriate registered compiler.
     *
     * This method finds a compiler for the expression and delegates
     * compilation to it. If no suitable compiler is found, a
     * [CompilationException] is thrown.
     *
     * ## Initial Traversal
     * The [initialTraversal] parameter is passed to the compiler for
     * expressions that need to build upon an existing traversal.
     * This is essential for match contexts where traversals must
     * start with `__.as()`.
     *
     * @param expression The expression to compile
     * @param context The compilation context
     * @param initialTraversal Optional initial traversal to build upon
     * @return The compilation result containing a GraphTraversal
     * @throws CompilationException If no compiler is registered for the expression type
     */
    fun compile(
        expression: TypedExpression,
        context: TraversalCompilationContext,
        initialTraversal: GraphTraversal<*, *>? = null
    ): TraversalCompilationResult<*, *> {
        val compiler = findCompiler(expression)
            ?: throw CompilationException(
                "No compiler registered for expression type: ${expression.kind}",
                expression
            )
        return compiler.compile(expression, context, initialTraversal)
    }

    /**
     * Checks if a compiler is registered that can handle the given expression.
     *
     * @param expression The expression to check
     * @return `true` if a compiler can handle the expression, `false` otherwise
     */
    fun canCompile(expression: TypedExpression): Boolean {
        return findCompiler(expression) != null
    }

    /**
     * Returns the number of registered compilers.
     *
     * @return The number of registered compilers
     */
    fun size(): Int = compilers.size

    /**
     * Clears all registered compilers.
     *
     * This is primarily useful for testing or reconfiguring the registry.
     */
    fun clear() {
        compilers.clear()
    }

    companion object {
        /**
         * Creates a registry with all default traversal compilers registered.
         *
         * This includes all literal, operator, access, and utility expression compilers:
         * - **Literals**: Int, Long, Double, Float, String, Boolean, Null, and List
         * - **Operators**: Binary operators (+, -, *, /, %, <, >, <=, >=, ==, !=, &&, ||),
         *   Unary operators (-, +, !), Ternary (? :)
         * - **Access**: Identifier references, member access, member calls
         * - **Type operations**: Type cast (as), type check (is), assert non-null (!!)
         * - **Lambda**: Lambda expressions (placeholder implementation)
         *
         * Compilers are registered in a specific order to ensure proper dispatch.
         * More specific compilers are registered first.
         *
         * @return A new [ExpressionCompilerRegistry] with all default compilers registered
         */
        fun createDefaultRegistry(): ExpressionCompilerRegistry {
            val registry = ExpressionCompilerRegistry()
            
            registry.registerAll(
                IntLiteralCompiler(),
                LongLiteralCompiler(),
                DoubleLiteralCompiler(),
                FloatLiteralCompiler(),
                StringLiteralCompiler(),
                BooleanLiteralCompiler(),
                NullLiteralCompiler()
            )
            
            registry.register(ListLiteralCompiler(registry))
            
            registry.registerAll(
                BinaryOperatorCompiler(registry),
                UnaryOperatorCompiler(registry),
                TernaryExpressionCompiler(registry),
                TypeCastCompiler(registry),
                TypeCheckCompiler(registry),
                AssertNonNullCompiler(registry)
            )
            
            registry.registerAll(
                IdentifierCompiler(),
                MemberAccessCompiler(registry),
                MemberCallCompiler(registry)
            )
            
            registry.register(LambdaCompiler())
            
            return registry
        }
    }
}

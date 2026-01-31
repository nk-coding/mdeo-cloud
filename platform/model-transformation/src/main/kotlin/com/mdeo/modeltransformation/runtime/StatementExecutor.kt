package com.mdeo.modeltransformation.runtime

import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement

/**
 * Interface for executing transformation statements.
 *
 * Statement executors are responsible for executing specific types of transformation
 * statements. The registry-based approach allows for modular and extensible statement
 * handling, where each statement type can have its own dedicated executor.
 *
 * Implementations should handle a specific statement type or family of types.
 * The [canExecute] method determines whether this executor can handle a given
 * statement, and [execute] performs the actual execution.
 *
 * @see StatementExecutorRegistry
 * @see TransformationExecutionContext
 * @see TransformationExecutionResult
 */
interface StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * Implementations should check the statement type and any other relevant
     * criteria to determine if this executor is the appropriate one to use.
     * This method should be fast and avoid complex computations.
     *
     * @param statement The statement to check.
     * @return True if this executor can execute the statement, false otherwise.
     */
    fun canExecute(statement: TypedTransformationStatement): Boolean
    
    /**
     * Executes the given statement.
     *
     * This method executes a typed transformation statement within the given
     * execution context. The execution may modify the graph and update the
     * context with new variable bindings or instance mappings.
     *
     * @param statement The statement to execute. The caller should ensure
     *                  [canExecute] returns true before calling this method.
     * @param context The current execution context.
     * @param engine The transformation engine, used for executing nested statements.
     * @return The result of executing the statement.
     */
    fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult
}

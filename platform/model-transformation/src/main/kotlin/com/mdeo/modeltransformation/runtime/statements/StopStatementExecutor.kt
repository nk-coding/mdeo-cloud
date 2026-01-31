package com.mdeo.modeltransformation.runtime.statements

import com.mdeo.modeltransformation.ast.statements.TypedStopStatement
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.runtime.StatementExecutor
import com.mdeo.modeltransformation.runtime.TransformationEngine
import com.mdeo.modeltransformation.runtime.TransformationExecutionContext
import com.mdeo.modeltransformation.runtime.TransformationExecutionResult

/**
 * Executor for TypedStopStatement.
 *
 * This executor handles stop and kill statements, which terminate transformation
 * execution. Both keywords result in a Stopped result, but the keyword is
 * preserved for potential semantic differences in how the caller handles the
 * termination.
 *
 * - "stop": Normal termination, may allow cleanup or finalization
 * - "kill": Immediate termination, typically aborts without cleanup
 *
 * The semantic difference between stop and kill may be interpreted differently
 * by the transformation orchestrator or runtime environment.
 */
class StopStatementExecutor : StatementExecutor {
    
    /**
     * Determines whether this executor can handle the given statement.
     *
     * @param statement The statement to check.
     * @return True if the statement is a TypedStopStatement.
     */
    override fun canExecute(statement: TypedTransformationStatement): Boolean {
        return statement is TypedStopStatement
    }
    
    /**
     * Executes a stop or kill statement.
     *
     * Returns a Stopped result with the keyword preserved. The current execution
     * context is included in the result for potential inspection.
     *
     * @param statement The stop statement to execute.
     * @param context The current execution context.
     * @param engine The transformation engine (not used for stop statements).
     * @return A Stopped result indicating intentional termination.
     */
    override fun execute(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext,
        engine: TransformationEngine
    ): TransformationExecutionResult {
        val stopStatement = statement as TypedStopStatement
        
        return TransformationExecutionResult.Stopped(
            keyword = stopStatement.keyword,
            context = context
        )
    }
}

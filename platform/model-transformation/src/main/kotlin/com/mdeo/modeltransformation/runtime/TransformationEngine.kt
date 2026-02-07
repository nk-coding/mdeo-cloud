package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.TypedClass
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.compiler.CompilationContext
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.compiler.registry.gremlinType
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource

/**
 * Main entry point for executing model transformations.
 *
 * The TransformationEngine executes a TypedAst against a graph using the provided
 * GraphTraversalSource. It maintains execution state through a TransformationExecutionContext
 * and delegates statement execution to registered StatementExecutors.
 *
 * The engine processes statements sequentially, updating the context as bindings
 * are established through pattern matching. If a statement fails (e.g., a match
 * doesn't apply when required), the transformation terminates with a failure result.
 *
 * @param traversalSource The Gremlin GraphTraversalSource for graph operations.
 * @param expressionCompilerRegistry Registry for compiling expressions to GraphTraversal.
 * @param statementExecutorRegistry Registry for executing transformation statements.
 */
class TransformationEngine(
    val traversalSource: GraphTraversalSource,
    val ast: TypedAst,
    val expressionCompilerRegistry: ExpressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
    val statementExecutorRegistry: StatementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
) {

    val instanceNameRegistry: InstanceNameRegistry = InstanceNameRegistry()

    /**
     * The types list from the currently executing TypedAst.
     * This is populated from the AST provided in the constructor.
     */
    val types: List<ReturnType> = ast.types

    /**
     * Dynamic type registry that includes both standard library types and metamodel types.
     *
     * This registry is built during initialization using the TypedAst. It uses the GLOBAL
     * registry as a parent, so all stdlib types remain available.
     *
     * Metamodel types (e.g., "metamodel./metamodel.mm.House") are registered with their
     * fully qualified names and include graph properties and edge associations.
     */
    val typeRegistry: GremlinTypeRegistry

    init {
        typeRegistry = createTypeRegistry(ast.classes)
    }

    /**
     * Executes a complete model transformation.
     *
     * This is the main entry point for running a transformation. It processes
     * all statements in the TypedAst sequentially, maintaining state throughout
     * the execution.
     *
     * @return The result of the transformation execution.
     */
    fun execute(): TransformationExecutionResult {
        var context = TransformationExecutionContext.empty()
        var accumulatedResult = TransformationExecutionResult.Success(context)

        for (statement in ast.statements) {
            val result = executeStatement(statement, context)

            when (result) {
                is TransformationExecutionResult.Success -> {
                    context = result.context
                    accumulatedResult = accumulatedResult.merge(result)
                }
                is TransformationExecutionResult.Failure -> return result
                is TransformationExecutionResult.Stopped -> return result
            }
        }

        return accumulatedResult
    }

    /**
     * Executes a single transformation statement.
     *
     * This method delegates to the appropriate StatementExecutor based on the
     * statement type. If no executor is registered for the statement type,
     * a failure result is returned.
     *
     * @param statement The statement to execute.
     * @param context The current execution context.
     * @return The result of executing the statement.
     */
    fun executeStatement(
        statement: TypedTransformationStatement,
        context: TransformationExecutionContext
    ): TransformationExecutionResult {
        return statementExecutorRegistry.execute(statement, context, this)
    }

    /**
     * Executes a block of statements sequentially.
     *
     * Statements are executed in order. If any statement fails or stops,
     * execution terminates and that result is returned. Otherwise, results
     * are accumulated and returned as a merged Success.
     *
     * @param statements The statements to execute.
     * @param context The initial execution context.
     * @return The combined result of executing all statements.
     */
    fun executeBlock(
        statements: List<TypedTransformationStatement>,
        context: TransformationExecutionContext
    ): TransformationExecutionResult {
        var currentContext = context
        var accumulatedResult = TransformationExecutionResult.Success(currentContext)

        for (statement in statements) {
            val result = executeStatement(statement, currentContext)

            when (result) {
                is TransformationExecutionResult.Success -> {
                    currentContext = result.context
                    accumulatedResult = accumulatedResult.merge(result)
                }
                is TransformationExecutionResult.Failure -> return result
                is TransformationExecutionResult.Stopped -> return result
            }
        }

        return accumulatedResult
    }

    /**
     * Returns the type at the given index in the types array.
     *
     * @param typeIndex The index of the type to retrieve.
     * @return The ReturnType at the specified index.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    fun getType(typeIndex: Int): ReturnType {
        return types[typeIndex]
    }

    /**
     * Returns the type at the given index, or null if out of bounds.
     *
     * @param typeIndex The index of the type to retrieve.
     * @return The ReturnType at the specified index, or null if invalid.
     */
    fun getTypeOrNull(typeIndex: Int): ReturnType? {
        return types.getOrNull(typeIndex)
    }

    /**
     * Creates a dynamic type registry including metamodel classes.
     *
     * @param classes The metamodel class definitions from the TypedAst.
     */
    private fun createTypeRegistry(classes: List<TypedClass>): GremlinTypeRegistry {
        val dynamicRegistry = GremlinTypeRegistry(parent = CompilationContext.GLOBAL_TYPE_REGISTRY)

        for (typedClass in classes) {
            val fqn = typedClass.`package` + "." + typedClass.name

            val builder = gremlinType(fqn)

            for (superClass in typedClass.superClasses) {
                builder.extends(superClass)
            }

            for (property in typedClass.properties) {
                builder.graphProperty(property.name)
            }

            for (relation in typedClass.relations) {
                val edgeLabel = EdgeLabelUtils.computeEdgeLabel(
                    relation.property,
                    relation.oppositeProperty,
                    relation.isOutgoing
                )
                builder.association(
                    propertyName = relation.property,
                    edgeLabel = edgeLabel,
                    isOutgoing = relation.isOutgoing
                )
            }

            dynamicRegistry.register(builder.build())
        }

        return dynamicRegistry
    }

    companion object {
        /**
         * Creates a TransformationEngine with default registries.
         *
         * @param traversalSource The Gremlin GraphTraversalSource.
         * @param ast The TypedAst to execute.
         * @return A new TransformationEngine with default configuration.
         */
        fun create(traversalSource: GraphTraversalSource, ast: TypedAst): TransformationEngine {
            return TransformationEngine(
                traversalSource = traversalSource,
                ast = ast,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
            )
        }
    }
}

package com.mdeo.modeltransformation.runtime

import com.mdeo.expression.ast.types.AssociationData
import com.mdeo.expression.ast.types.ClassData
import com.mdeo.expression.ast.types.MetamodelData
import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.modeltransformation.ast.EdgeLabelUtils
import com.mdeo.modeltransformation.ast.TypedAst
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import com.mdeo.modeltransformation.compiler.ExpressionCompilerRegistry
import com.mdeo.modeltransformation.compiler.registry.TypeRegistry
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
 * @param ast The TypedAst containing transformation statements.
 * @param metamodelData The metamodel data containing class and association definitions.
 * @param expressionCompilerRegistry Registry for compiling expressions to GraphTraversal.
 * @param statementExecutorRegistry Registry for executing transformation statements.
 */
class TransformationEngine(
    val traversalSource: GraphTraversalSource,
    val ast: TypedAst,
    val metamodelData: MetamodelData = MetamodelData.empty(),
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
     * This registry is built during initialization using the MetamodelData. It uses the GLOBAL
     * registry as a parent, so all stdlib types remain available.
     *
     * Metamodel types are registered with package prefixes:
     * - "class/path/to/metamodel.ClassName" for metamodel classes
     * - "enum/path/to/metamodel.EnumName" for enum value types
     * - "enum-container/path/to/metamodel.EnumName" for enum container types
     */
    val typeRegistry: TypeRegistry

    /**
     * The class package prefix derived from the metamodel path.
     * Example: if metamodelPath is "/project/models/house", classPackage is "class/project/models/house"
     */
    val classPackage: String = "class${ast.metamodelPath}"

    /**
     * The enum package prefix derived from the metamodel path.
     * Example: if metamodelPath is "/project/models/house", enumPackage is "enum/project/models/house"
     */
    val enumPackage: String = "enum${ast.metamodelPath}"

    /**
     * The enum container package prefix derived from the metamodel path.
     * Example: if metamodelPath is "/project/models/house", enumContainerPackage is "enum-container/project/models/house"
     */
    val enumContainerPackage: String = "enum-container${ast.metamodelPath}"

    init {
        typeRegistry = createTypeRegistry(metamodelData)
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
        var context = TransformationExecutionContext.emptyWithEnums(metamodelData.enums.map { it.name })
        var accumulatedResult = TransformationExecutionResult.Success()

        for (statement in ast.statements) {
            val result = executeStatement(statement, context)

            when (result) {
                is TransformationExecutionResult.Success -> {
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
        val currentContext = context.enterScope()
        var accumulatedResult = TransformationExecutionResult.Success()

        for (statement in statements) {
            val result = executeStatement(statement, currentContext)

            when (result) {
                is TransformationExecutionResult.Success -> {
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
     * Creates a dynamic type registry including metamodel classes and enums.
     * 
     * Type references use the following formats:
     * - "class/path/to/metamodel.ClassName" for metamodel classes
     * - "enum/path/to/metamodel.EnumName" for enum value types (stored on properties)
     * - "enum-container/path/to/metamodel.EnumName" for enum container types (the singleton for accessing enum values)
     *
     * @param metamodelData The metamodel data containing class, enum, and association definitions.
     */
    private fun createTypeRegistry(metamodelData: MetamodelData): TypeRegistry {
        val dynamicRegistry = TypeRegistry(parent = TypeRegistry.GLOBAL)

        registerEnumTypes(dynamicRegistry, metamodelData)

        val classAssociations = buildClassAssociationsMap(metamodelData.associations)

        for (classData in metamodelData.classes) {
            val builder = gremlinType(classPackage, classData.name)

            for (superClass in classData.extends) {
                builder.extends(classPackage, superClass)
            }

            for (property in classData.properties) {
                builder.graphProperty(property.name)
            }

            val associations = classAssociations[classData.name] ?: emptyList()
            for ((assoc, isSource) in associations) {
                val thisEnd = if (isSource) assoc.source else assoc.target
                val otherEnd = if (isSource) assoc.target else assoc.source
                
                val propertyName = thisEnd.name ?: continue
                val oppositePropertyName = otherEnd.name
                
                val edgeLabel = EdgeLabelUtils.computeEdgeLabel(propertyName, oppositePropertyName)
                builder.association(
                    propertyName = propertyName,
                    edgeLabel = edgeLabel,
                    isOutgoing = isSource
                )
            }

            dynamicRegistry.register(builder.build())
        }

        return dynamicRegistry
    }

    /**
     * Registers enum types in the type registry.
     * 
     * For each enum in the metamodel, two types are registered:
     * 
     * 1. "enum-container/path/to/metamodel.EnumName" - The container type (singleton) used to access enum values.
     *    Each entry is a property that returns a constant string in the format `EnumName`.`entryName`.
     *    Accessing an entry like `EnumName.entryName` produces this string literal.
     * 
     * 2. "enum/path/to/metamodel.EnumName" - The actual enum value type, used when a property has an enum type.
     *    This represents the stored enum value on an object property.
     * 
     * @param registry The registry to add enum types to.
     * @param metamodelData The metamodel data containing enum definitions.
     */
    private fun registerEnumTypes(registry: TypeRegistry, metamodelData: MetamodelData) {
        for (enumData in metamodelData.enums) {
            val enumName = enumData.name

            val containerBuilder = gremlinType(enumContainerPackage, enumName)
            for (entryName in enumData.entries) {
                val enumValueString = "`$enumName`.`$entryName`"
                containerBuilder.enumEntry(entryName, enumValueString)
            }
            registry.register(containerBuilder.build())

            val valueTypeBuilder = gremlinType(enumPackage, enumName)
            registry.register(valueTypeBuilder.build())
        }
    }

    /**
     * Builds a map from class name to list of (association, isSource) pairs.
     * isSource is true if the class is the source end of the association.
     */
    private fun buildClassAssociationsMap(associations: List<AssociationData>): Map<String, List<Pair<AssociationData, Boolean>>> {
        val result = mutableMapOf<String, MutableList<Pair<AssociationData, Boolean>>>()
        
        for (assoc in associations) {
            result.getOrPut(assoc.source.className) { mutableListOf() }
                .add(assoc to true)
            
            result.getOrPut(assoc.target.className) { mutableListOf() }
                .add(assoc to false)
        }
        
        return result
    }

    companion object {
        /**
         * Creates a TransformationEngine with default registries.
         *
         * @param traversalSource The Gremlin GraphTraversalSource.
         * @param ast The TypedAst to execute.
         * @param metamodelData The metamodel data containing class and association definitions.
         * @return A new TransformationEngine with default configuration.
         */
        fun create(
            traversalSource: GraphTraversalSource, 
            ast: TypedAst,
            metamodelData: MetamodelData = MetamodelData.empty()
        ): TransformationEngine {
            return TransformationEngine(
                traversalSource = traversalSource,
                ast = ast,
                metamodelData = metamodelData,
                expressionCompilerRegistry = ExpressionCompilerRegistry.createDefaultRegistry(),
                statementExecutorRegistry = StatementExecutorRegistry.createDefaultRegistry()
            )
        }
    }
}

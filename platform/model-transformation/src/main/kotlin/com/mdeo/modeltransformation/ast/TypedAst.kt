package com.mdeo.modeltransformation.ast

import com.mdeo.expression.ast.types.ReturnType
import com.mdeo.expression.ast.types.ReturnTypeSerializer
import com.mdeo.modeltransformation.ast.statements.TypedTransformationStatement
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Root of the TypedAST containing all model transformation program information.
 *
 * This is the top-level data structure that represents a complete model transformation
 * program after type checking. It contains type information, metamodel reference,
 * and all transformation statements.
 *
 * Note: Metamodel class definitions are now fetched separately via MetamodelData
 * and passed to the TransformationEngine at execution time.
 *
 * @param types Array of all types used in the program. Generics are replaced by Any? due to
 *              type erasure. Types are indexed by typeIndex in expressions and statements.
 * @param metamodelPath Absolute path of the imported metamodel file that this transformation operates on.
 * @param statements All top-level transformation statements in the program.
 */
@Serializable
data class TypedAst(
    val types: List<@Serializable(with = ReturnTypeSerializer::class) ReturnType>,
    val metamodelPath: String,
    val statements: List<@Contextual TypedTransformationStatement>
)

package com.mdeo.script.ast

import com.mdeo.script.ast.types.ReturnType
import com.mdeo.script.ast.types.ReturnTypeSerializer
import kotlinx.serialization.Serializable

/**
 * Root of the TypedAST containing all program information.
 */
@Serializable
data class TypedAst(
    /**
     * Array of all types used in the program.
     * Generics are replaced by Any? due to type erasure.
     * Indexed by typeIndex in expressions.
     */
    val types: List<@Serializable(with = ReturnTypeSerializer::class) ReturnType>,
    /**
     * All imports in the program.
     */
    val imports: List<TypedImport>,
    /**
     * All top-level functions in the program.
     */
    val functions: List<TypedFunction>
)

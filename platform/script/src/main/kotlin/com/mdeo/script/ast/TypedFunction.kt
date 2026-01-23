package com.mdeo.script.ast

import kotlinx.serialization.Serializable

/**
 * Function declaration.
 *
 * @param name Name of the function.
 * @param parameters Parameters of the function.
 * @param returnType Index into the types array for the return type.
 * @param body Body of the function.
 */
@Serializable
data class TypedFunction(
    val name: String,
    val parameters: List<TypedParameter>,
    val returnType: Int,
    val body: TypedCallableBody
)

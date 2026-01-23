package com.mdeo.script.ast

import kotlinx.serialization.Serializable

/**
 * Function or lambda parameter.
 *
 * @param name Name of the parameter.
 * @param type Index into the types array for the parameter type.
 */
@Serializable
data class TypedParameter(
    val name: String,
    val type: Int
)

package com.mdeo.expression.ast.types

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base interface for all return types.
 * A return type can be either a value type or void.
 */
interface ReturnType

/**
 * A void type reference.
 * Used for functions/methods that don't return a value.
 *
 * @param kind Marker to distinguish void type from other types.
 */
@Serializable
@SerialName("void")
data class VoidType(
    @EncodeDefault
    val kind: String = "void"
) : ReturnType

/**
 * Base interface for value types.
 * A value type can be a class type, generic type reference, or lambda type.
 */
interface ValueType : ReturnType

/**
 * A reference to a concrete type, optionally with type arguments.
 *
 * @param package The package of the type (e.g., "builtin", "class/path/to/file", "enum/path").
 *                Defaults to empty string for backward compatibility with old serialization format.
 * @param type The simple type name being referenced (e.g., "string", "ClassName").
 *             When package is empty, this may contain the fully qualified name for backward compatibility.
 * @param isNullable Whether this type reference can be null.
 * @param typeArgs Optional type arguments for generic types.
 */
@Serializable
@SerialName("class")
data class ClassTypeRef(
    val `package`: String = "",
    val type: String,
    val isNullable: Boolean,
    val typeArgs: Map<String, @Serializable(with = ValueTypeSerializer::class) ValueType>? = null
) : ValueType

/**
 * A reference to a generic type parameter.
 * Used when referring to a generic type variable like 'T' or 'U'.
 *
 * @param generic The name of the generic type parameter.
 * @param isNullable Whether this type reference can be null.
 */
@Serializable
@SerialName("generic")
data class GenericTypeRef(
    val generic: String,
    val isNullable: Boolean? = null
) : ValueType

/**
 * A method or function parameter type.
 *
 * @param name The name of the parameter.
 * @param type The type of the parameter.
 */
@Serializable
data class Parameter(
    val name: String,
    val type: @Serializable(with = ValueTypeSerializer::class) ValueType
)

/**
 * A lambda type (anonymous function type).
 *
 * @param returnType The return type of the lambda.
 * @param parameters The parameters of the lambda.
 * @param isNullable Whether this lambda type can be null.
 */
@Serializable
@SerialName("lambda")
data class LambdaType(
    val returnType: @Serializable(with = ReturnTypeSerializer::class) ReturnType,
    val parameters: List<Parameter>,
    val isNullable: Boolean
) : ValueType

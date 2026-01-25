package com.mdeo.script.ast.types

/**
 * DSL helper functions for constructing ValueType and ReturnType instances.
 *
 * These functions provide a fluent API for creating type definitions
 * in the stdlib registry without manual JSON-like object construction.
 */

/**
 * Creates a void return type.
 *
 * @return A VoidType instance.
 */
fun voidType(): VoidType = VoidType()

/**
 * Creates a non-nullable class type reference.
 *
 * @param typeName The type name (e.g., "builtin.int", "builtin.string").
 * @return A ClassTypeRef with isNullable = false.
 */
fun classType(typeName: String): ClassTypeRef = ClassTypeRef(typeName, false)

/**
 * Creates a nullable class type reference.
 *
 * @param typeName The type name (e.g., "builtin.int", "builtin.string").
 * @return A ClassTypeRef with isNullable = true.
 */
fun nullableClassType(typeName: String): ClassTypeRef = ClassTypeRef(typeName, true)

/**
 * Creates a class type reference with generic type arguments.
 *
 * @param typeName The base type name (e.g., "builtin.List", "builtin.Map").
 * @param isNullable Whether the type is nullable.
 * @param typeArgs The type arguments as a map (e.g., mapOf("T" to classType("builtin.string"))).
 * @return A ClassTypeRef with type arguments.
 */
fun genericClassType(
    typeName: String,
    isNullable: Boolean = false,
    typeArgs: Map<String, ValueType>
): ClassTypeRef = ClassTypeRef(typeName, isNullable, typeArgs)

/**
 * Creates a lambda type.
 *
 * @param returnType The return type of the lambda.
 * @param parameters The parameters of the lambda.
 * @param isNullable Whether the lambda type is nullable.
 * @return A LambdaType instance.
 */
fun lambdaType(
    returnType: ReturnType,
    parameters: List<Parameter> = emptyList(),
    isNullable: Boolean = false
): LambdaType = LambdaType(returnType, parameters, isNullable)

/**
 * Creates a lambda type with a simple parameter list.
 *
 * @param returnType The return type of the lambda.
 * @param isNullable Whether the lambda type is nullable.
 * @param params Pairs of (name, type) for the parameters.
 * @return A LambdaType instance.
 */
fun lambdaType(
    returnType: ReturnType,
    isNullable: Boolean = false,
    vararg params: Pair<String, ValueType>
): LambdaType = LambdaType(
    returnType,
    params.map { (name, type) -> Parameter(name, type) },
    isNullable
)

/**
 * Object containing common type constants for use in stdlib definitions.
 *
 * This avoids repeatedly constructing common types like int, string, boolean, etc.
 */
object BuiltinTypes {
    // Primitive types (non-nullable)
    val INT = classType("builtin.int")
    val LONG = classType("builtin.long")
    val FLOAT = classType("builtin.float")
    val DOUBLE = classType("builtin.double")
    val BOOLEAN = classType("builtin.boolean")
    val STRING = classType("builtin.string")
    val ANY = classType("builtin.any")
    
    // Nullable primitive types
    val NULLABLE_INT = nullableClassType("builtin.int")
    val NULLABLE_LONG = nullableClassType("builtin.long")
    val NULLABLE_FLOAT = nullableClassType("builtin.float")
    val NULLABLE_DOUBLE = nullableClassType("builtin.double")
    val NULLABLE_BOOLEAN = nullableClassType("builtin.boolean")
    val NULLABLE_STRING = nullableClassType("builtin.string")
    val NULLABLE_ANY = nullableClassType("builtin.any")
    
    // Collection types (non-nullable)
    val LIST = genericClassType("builtin.List", typeArgs = mapOf("T" to NULLABLE_ANY))
    val SET = genericClassType("builtin.Set", typeArgs = mapOf("T" to NULLABLE_ANY))
    val BAG = genericClassType("builtin.Bag", typeArgs = mapOf("T" to NULLABLE_ANY))
    val ORDERED_SET = genericClassType("builtin.OrderedSet", typeArgs = mapOf("T" to NULLABLE_ANY))
    val COLLECTION = classType("builtin.Collection")
    val READONLY_COLLECTION = classType("builtin.ReadonlyCollection")
    val READONLY_ORDERED_COLLECTION = classType("builtin.ReadonlyOrderedCollection")
    val ORDERED_COLLECTION = classType("builtin.OrderedCollection")
    val MAP = genericClassType("builtin.Map", typeArgs = mapOf("K" to NULLABLE_ANY, "V" to NULLABLE_ANY))
    val READONLY_MAP = genericClassType("builtin.ReadonlyMap", typeArgs = mapOf("K" to NULLABLE_ANY, "V" to NULLABLE_ANY))
    
    // Void type
    val VOID = voidType()
    
    /**
     * Creates a predicate lambda type: (T) -> boolean
     *
     * @param paramType The type of the predicate parameter.
     * @return A LambdaType for a predicate function.
     */
    fun predicate(paramType: ValueType = NULLABLE_ANY): LambdaType =
        lambdaType(BOOLEAN, false, "it" to paramType)
    
    /**
     * Creates a consumer lambda type: (T) -> void
     *
     * @param paramType The type of the consumer parameter.
     * @return A LambdaType for a consumer function.
     */
    fun consumer(paramType: ValueType = NULLABLE_ANY): LambdaType =
        lambdaType(VOID, false, "it" to paramType)
    
    /**
     * Creates a function lambda type: (T) -> R
     *
     * @param paramType The type of the function parameter.
     * @param returnType The return type of the function.
     * @return A LambdaType for a function.
     */
    fun function(paramType: ValueType = NULLABLE_ANY, returnType: ReturnType = NULLABLE_ANY): LambdaType =
        lambdaType(returnType, false, "it" to paramType)
}

package com.mdeo.script.compiler

import com.mdeo.expression.ast.types.*

/**
 * Registry for lambda functional interfaces.
 *
 * This registry maps lambda types to functional interface names.
 * It uses normalized lambda types as keys (replacing generic type parameters with nullable Any).
 * All interfaces are treated uniformly - no distinction between predefined and generated.
 *
 * Boxing/unboxing is determined by the actual types:
 * - Non-nullable primitives (int, long, float, double, boolean) → no boxing
 * - Nullable types (int?, Any?) or Any → needs boxing
 */
class LambdaInterfaceRegistry {

    /**
     * Maps normalized lambda types to interface names (internal JVM names).
     * The map uses LambdaType as keys with generic parameters replaced by nullable Any.
     */
    private val interfaceMap: MutableMap<LambdaType, String> = mutableMapOf()

    /**
     * Counter for generating unique interface names.
     */
    private var interfaceCounter: Int = 0

    init {
        initializePredefinedInterfaces()
    }

    /**
     * Initializes predefined interfaces from the runtime package.
     *
     * These interfaces are defined in com.mdeo.script.runtime:
     * - Func0<R>, Func1<T,R>, Func2<T1,T2,R>, Func3<T1,T2,T3,R> for functions with return values
     * - Action0, Action1<T>, Action2<T1,T2>, Action3<T1,T2,T3> for void-returning functions
     * - Predicate1<T> for boolean-returning single-argument functions
     */
    private fun initializePredefinedInterfaces() {
        val basePackage = "com/mdeo/script/runtime"
        val nullableAny = ClassTypeRef("builtin.any", isNullable = true)
        val voidType = VoidType()
        
        registerPredefined(
            createLambdaType(voidType, emptyList()),
            "$basePackage/interfaces/Action0"
        )
        registerPredefined(
            createLambdaType(voidType, listOf(nullableAny)),
            "$basePackage/interfaces/Action1"
        )
        registerPredefined(
            createLambdaType(voidType, listOf(nullableAny, nullableAny)),
            "$basePackage/interfaces/Action2"
        )
        registerPredefined(
            createLambdaType(voidType, listOf(nullableAny, nullableAny, nullableAny)),
            "$basePackage/interfaces/Action3"
        )

        val nonNullBoolean = ClassTypeRef("builtin.boolean", isNullable = false)
        registerPredefined(
            createLambdaType(nonNullBoolean, listOf(nullableAny)),
            "$basePackage/interfaces/Predicate1"
        )

        registerPredefined(
            createLambdaType(nullableAny, emptyList()),
            "$basePackage/interfaces/Func0"
        )
        registerPredefined(
            createLambdaType(nullableAny, listOf(nullableAny)),
            "$basePackage/interfaces/Func1"
        )
        registerPredefined(
            createLambdaType(nullableAny, listOf(nullableAny, nullableAny)),
            "$basePackage/interfaces/Func2"
        )
        registerPredefined(
            createLambdaType(nullableAny, listOf(nullableAny, nullableAny, nullableAny)),
            "$basePackage/interfaces/Func3"
        )
    }

    /**
     * Helper to create a LambdaType with the given return type and parameter types.
     */
    private fun createLambdaType(returnType: ReturnType, paramTypes: List<ValueType>): LambdaType {
        return LambdaType(
            returnType = returnType,
            parameters = paramTypes.mapIndexed { i, type -> Parameter("p$i", type) },
            isNullable = false
        )
    }

    /**
     * Registers a predefined interface.
     */
    private fun registerPredefined(key: LambdaType, interfaceName: String) {
        interfaceMap[key] = interfaceName
    }

    /**
     * Creates a normalized key from a LambdaType.
     * 
     * Replaces all generic type parameters with nullable Any to create a normalized
     * key for interface lookup. This allows generic lambdas to map to the same
     * functional interface. Parameter names are also normalized to "p0", "p1", etc.
     * to ensure two lambda types with same signatures but different parameter names
     * map to the same functional interface.
     *
     * @param lambdaType The lambda type to normalize.
     * @return A normalized lambda type suitable for use as a map key.
     */
    fun createKey(lambdaType: LambdaType): LambdaType {
        val nullableAny = ClassTypeRef("builtin.any", isNullable = true)
        
        return LambdaType(
            returnType = normalizeType(lambdaType.returnType, nullableAny),
            parameters = lambdaType.parameters.mapIndexed { index, param ->
                Parameter("p$index", normalizeType(param.type, nullableAny) as ValueType)
            },
            isNullable = lambdaType.isNullable
        )
    }

    /**
     * Normalizes a type by replacing generic type refs with nullable Any.
     */
    private fun normalizeType(type: ReturnType, nullableAny: ClassTypeRef): ReturnType {
        return when (type) {
            is GenericTypeRef -> nullableAny
            is VoidType -> type
            is ClassTypeRef -> type
            is LambdaType -> createKey(type)
            else -> type
        }
    }

    /**
     * Result of looking up a lambda interface.
     *
     * @param interfaceName The internal JVM name of the functional interface.
     * @param isNewlyGenerated True if this interface was just registered (needs bytecode generation).
     */
    data class InterfaceLookupResult(
        val interfaceName: String,
        val isNewlyGenerated: Boolean
    )

    /**
     * Gets the functional interface name for a lambda type.
     *
     * Uses normalized lambda types as keys. If no interface exists for the normalized
     * type, generates a new interface name with simple counting.
     *
     * @param lambdaType The lambda type to find an interface for.
     * @return The lookup result containing the interface name and metadata.
     */
    fun getInterfaceForLambdaType(lambdaType: LambdaType): InterfaceLookupResult {
        val key = createKey(lambdaType)
        
        interfaceMap[key]?.let { interfaceName ->
            return InterfaceLookupResult(
                interfaceName = interfaceName,
                isNewlyGenerated = false
            )
        }

        val newInterfaceName = "Lambda$${interfaceCounter++}"
        interfaceMap[key] = newInterfaceName

        return InterfaceLookupResult(
            interfaceName = newInterfaceName,
            isNewlyGenerated = true
        )
    }

    companion object {
        /**
         * The package path for predefined runtime interfaces.
         */
        const val RUNTIME_PACKAGE = "com/mdeo/script/runtime"
        
        /**
         * Checks if a type needs boxing.
         * Non-nullable primitives do not need boxing.
         * Nullable types and Any need boxing.
         */
        fun needsBoxing(type: ReturnType): Boolean {
            if (type is VoidType) return false
            if (type !is ClassTypeRef) return true
            if (type.isNullable) return true
            
            return when (type.type) {
                "builtin.int", "builtin.long", "builtin.float", 
                "builtin.double", "builtin.boolean" -> false
                else -> true
            }
        }
    }
}

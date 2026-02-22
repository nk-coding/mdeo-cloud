package com.mdeo.modeltransformation.stdlib

import com.mdeo.modeltransformation.compiler.registry.GremlinTypeDefinition
import com.mdeo.modeltransformation.compiler.registry.GremlinTypeRegistry
import com.mdeo.modeltransformation.stdlib.types.createAnyType
import com.mdeo.modeltransformation.stdlib.types.createBagType
import com.mdeo.modeltransformation.stdlib.types.createBooleanType
import com.mdeo.modeltransformation.stdlib.types.createCollectionType
import com.mdeo.modeltransformation.stdlib.types.createDoubleType
import com.mdeo.modeltransformation.stdlib.types.createFloatType
import com.mdeo.modeltransformation.stdlib.types.createIntType
import com.mdeo.modeltransformation.stdlib.types.createListType
import com.mdeo.modeltransformation.stdlib.types.createLongType
import com.mdeo.modeltransformation.stdlib.types.createOrderedSetType
import com.mdeo.modeltransformation.stdlib.types.createOrderedCollectionType
import com.mdeo.modeltransformation.stdlib.types.createSetType
import com.mdeo.modeltransformation.stdlib.types.createStringType

/**
 * Registrar for standard library type definitions.
 *
 * Registers all built-in types and their properties/methods for Gremlin compilation.
 * This includes primitive types (int, long, string, etc.) and collection types.
 *
 * All methods are implemented as pure Gremlin traversals for portability.
 * Lambda-accepting methods (filter, map, exists, etc.) are compiled to pure
 * traversals by compiling the lambda body with parameter binding.
 *
 * @see GremlinTypeRegistry
 */
object StdlibRegistrar {

    /**
     * Registers all standard library types with the given registry.
     *
     * @param registry The registry to register types with.
     */
    fun registerAll(registry: GremlinTypeRegistry) {
        registerPrimitiveTypes(registry)
        registerCollectionTypes(registry)
    }

    /**
     * Creates a registry with all standard library types pre-registered.
     *
     * @return A new [GremlinTypeRegistry] with all stdlib types.
     */
    fun createRegistry(): GremlinTypeRegistry {
        val registry = GremlinTypeRegistry()
        registerAll(registry)
        return registry
    }

    /**
     * Creates all standard library type definitions.
     *
     * @return A list of all stdlib type definitions.
     */
    fun createAllTypes(): List<GremlinTypeDefinition> {
        return listOf(
            createAnyType(),
            createBooleanType(),
            createIntType(),
            createLongType(),
            createFloatType(),
            createDoubleType(),
            createStringType(),
            createCollectionType(),
            createOrderedCollectionType(),
            createListType(),
            createSetType(),
            createOrderedSetType(),
            createBagType()
        )
    }

    /**
     * Registers all primitive types with the given registry.
     *
     * Primitive types include: any, boolean, int, long, float, double, and string.
     *
     * @param registry The registry to register primitive types with
     */
    private fun registerPrimitiveTypes(registry: GremlinTypeRegistry) {
        registry.register(createAnyType())
        registry.register(createBooleanType())
        registry.register(createIntType())
        registry.register(createLongType())
        registry.register(createFloatType())
        registry.register(createDoubleType())
        registry.register(createStringType())
    }

    /**
     * Registers all collection types with the given registry.
     *
     * Collection types include: Collection, OrderedCollection, List, Set, OrderedSet, and Bag.
     *
     * @param registry The registry to register collection types with
     */
    private fun registerCollectionTypes(registry: GremlinTypeRegistry) {
        registry.register(createCollectionType())
        registry.register(createOrderedCollectionType())
        registry.register(createListType())
        registry.register(createSetType())
        registry.register(createOrderedSetType())
        registry.register(createBagType())
    }
}

package com.mdeo.script.stdlib.registrar.global

import com.mdeo.script.ast.types.BuiltinTypes
import com.mdeo.script.compiler.registry.function.FunctionDefinition
import com.mdeo.script.compiler.registry.function.GlobalFunctionRegistry
import com.mdeo.script.compiler.registry.function.globalFunction

/**
 * Registers all stdlib global function definitions in the registry.
 *
 * The definitions match the TypeScript stdlib definitions in
 * app/packages/language-script/src/features/type-system/
 */
object GlobalFunctionRegistrar {

    private const val GLOBAL_HELPER = "com/mdeo/script/stdlib/impl/globals/GlobalFunctions"

    /**
     * Registers all stdlib global functions in the given registry.
     *
     * @param registry The registry to add global functions to.
     */
    fun registerAll(registry: GlobalFunctionRegistry) {
        registry.registerFunction(createPrintln())
        registry.registerFunction(createListOf())
        registry.registerFunction(createSetOf())
        registry.registerFunction(createBagOf())
        registry.registerFunction(createOrderedSetOf())
        registry.registerFunction(createEmptyList())
        registry.registerFunction(createEmptySet())
        registry.registerFunction(createEmptyBag())
        registry.registerFunction(createEmptyOrderedSet())
    }

    /**
     * Creates the println function definition.
     *
     * println(string) - prints a string followed by a newline.
     */
    private fun createPrintln(): FunctionDefinition {
        return globalFunction("println") {
            staticOverload("") {
                descriptor = "(Ljava/lang/String;)V"
                owner = GLOBAL_HELPER
                jvmMethod = "println"
                parameterTypes = listOf(BuiltinTypes.STRING)
                returnType = BuiltinTypes.VOID
            }
        }
    }

    /**
     * Creates the listOf function definition.
     *
     * listOf(...args) - creates a mutable list from varargs.
     */
    private fun createListOf(): FunctionDefinition {
        return globalFunction("listOf") {
            varArgsOverload("") {
                descriptor = "([Ljava/lang/Object;)Lcom/mdeo/script/stdlib/impl/collections/ScriptList;"
                owner = GLOBAL_HELPER
                jvmMethod = "listOf"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.LIST
            }
        }
    }

    /**
     * Creates the setOf function definition.
     *
     * setOf(...args) - creates a mutable set from varargs.
     */
    private fun createSetOf(): FunctionDefinition {
        return globalFunction("setOf") {
            varArgsOverload("") {
                descriptor = "([Ljava/lang/Object;)Lcom/mdeo/script/stdlib/impl/collections/ScriptSet;"
                owner = GLOBAL_HELPER
                jvmMethod = "setOf"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.SET
            }
        }
    }

    /**
     * Creates the bagOf function definition.
     *
     * bagOf(...args) - creates a mutable bag from varargs.
     */
    private fun createBagOf(): FunctionDefinition {
        return globalFunction("bagOf") {
            varArgsOverload("") {
                descriptor = "([Ljava/lang/Object;)Lcom/mdeo/script/stdlib/impl/collections/Bag;"
                owner = GLOBAL_HELPER
                jvmMethod = "bagOf"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.BAG
            }
        }
    }

    /**
     * Creates the orderedSetOf function definition.
     *
     * orderedSetOf(...args) - creates a mutable ordered set from varargs.
     */
    private fun createOrderedSetOf(): FunctionDefinition {
        return globalFunction("orderedSetOf") {
            varArgsOverload("") {
                descriptor = "([Ljava/lang/Object;)Lcom/mdeo/script/stdlib/impl/collections/OrderedSet;"
                owner = GLOBAL_HELPER
                jvmMethod = "orderedSetOf"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.ORDERED_SET
            }
        }
    }

    /**
     * Creates the emptyList function definition.
     *
     * emptyList<T>() - creates an empty mutable list.
     */
    private fun createEmptyList(): FunctionDefinition {
        return globalFunction("emptyList") {
            staticOverload("") {
                descriptor = "()Lcom/mdeo/script/stdlib/impl/collections/ScriptList;"
                owner = GLOBAL_HELPER
                jvmMethod = "emptyList"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.LIST
            }
        }
    }

    /**
     * Creates the emptySet function definition.
     *
     * emptySet<T>() - creates an empty mutable set.
     */
    private fun createEmptySet(): FunctionDefinition {
        return globalFunction("emptySet") {
            staticOverload("") {
                descriptor = "()Lcom/mdeo/script/stdlib/impl/collections/ScriptSet;"
                owner = GLOBAL_HELPER
                jvmMethod = "emptySet"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.SET
            }
        }
    }

    /**
     * Creates the emptyBag function definition.
     *
     * emptyBag<T>() - creates an empty mutable bag.
     */
    private fun createEmptyBag(): FunctionDefinition {
        return globalFunction("emptyBag") {
            staticOverload("") {
                descriptor = "()Lcom/mdeo/script/stdlib/impl/collections/Bag;"
                owner = GLOBAL_HELPER
                jvmMethod = "emptyBag"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.BAG
            }
        }
    }

    /**
     * Creates the emptyOrderedSet function definition.
     *
     * emptyOrderedSet<T>() - creates an empty mutable ordered set.
     */
    private fun createEmptyOrderedSet(): FunctionDefinition {
        return globalFunction("emptyOrderedSet") {
            staticOverload("") {
                descriptor = "()Lcom/mdeo/script/stdlib/impl/collections/OrderedSet;"
                owner = GLOBAL_HELPER
                jvmMethod = "emptyOrderedSet"
                parameterTypes = emptyList()
                returnType = BuiltinTypes.ORDERED_SET
            }
        }
    }
}

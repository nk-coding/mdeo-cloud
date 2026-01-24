package com.mdeo.script.stdlib.registrar.types

import com.mdeo.script.compiler.registry.type.TypeDefinition
import com.mdeo.script.compiler.registry.type.typeDefinition

private const val READONLY_COLLECTION = "com/mdeo/script/stdlib/impl/collections/ReadonlyCollection"

/**
 * Creates the ReadonlyCollection type definition.
 *
 * ReadonlyCollection is the base interface for all readonly collections.
 * It provides read-only operations like size(), isEmpty(), includes(), etc.
 */
fun createReadonlyCollectionType(): TypeDefinition {
    return typeDefinition("builtin.ReadonlyCollection") {
        extends("builtin.any")

        instanceMethod("size") {
            overload(
                "",
                "()I",
                READONLY_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("isEmpty") {
            overload(
                "",
                "()Z",
                READONLY_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("notEmpty") {
            overload(
                "",
                "()Z",
                READONLY_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("includes") {
            overload(
                "",
                "(Ljava/lang/Object;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?")
            )
        }

        instanceMethod("excludes") {
            overload(
                "",
                "(Ljava/lang/Object;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?")
            )
        }

        instanceMethod("count") {
            overload(
                "",
                "(Ljava/lang/Object;)I",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?")
            )
        }

        instanceMethod("sum") {
            overload(
                "",
                "()D",
                READONLY_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("concat") {
            overload(
                "nosep",
                "()Ljava/lang/String;",
                READONLY_COLLECTION,
                isInterface = true
            )
            overload(
                "sep",
                "(Ljava/lang/String;)Ljava/lang/String;",
                READONLY_COLLECTION,
                isInterface = true
            )
        }

        instanceMethod("including") {
            overload(
                "",
                "(Ljava/lang/Object;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?"),
                returnType = "builtin.ReadonlyCollection"
            )
        }

        instanceMethod("excluding") {
            overload(
                "",
                "(Ljava/lang/Object;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.any?"),
                returnType = "builtin.ReadonlyCollection"
            )
        }

        instanceMethod("asList") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/ScriptList;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.List"
            )
        }

        instanceMethod("asSet") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/ScriptSet;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.Set"
            )
        }

        instanceMethod("asBag") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/Bag;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.Bag"
            )
        }

        instanceMethod("asOrderedSet") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/OrderedSet;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.OrderedSet"
            )
        }

        instanceMethod("includesAll") {
            overload(
                "",
                "(Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.ReadonlyCollection"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("excludesAll") {
            overload(
                "",
                "(Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.ReadonlyCollection"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("includingAll") {
            overload(
                "",
                "(Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.ReadonlyCollection"),
                returnType = "builtin.ReadonlyCollection"
            )
        }

        instanceMethod("excludingAll") {
            overload(
                "",
                "(Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.ReadonlyCollection"),
                returnType = "builtin.ReadonlyCollection"
            )
        }

        instanceMethod("flatten") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.ReadonlyCollection"
            )
        }

        instanceMethod("random") {
            overload(
                "",
                "()Ljava/lang/Object;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.any?"
            )
        }

        instanceMethod("clone") {
            overload(
                "",
                "()Lcom/mdeo/script/stdlib/impl/collections/Collection;",
                READONLY_COLLECTION,
                isInterface = true,
                returnType = "builtin.Collection"
            )
        }

        instanceMethod("atLeastNMatch") {
            overload(
                "",
                "(Ljava/util/function/Predicate;I)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda", "builtin.int"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("atMostNMatch") {
            overload(
                "",
                "(Ljava/util/function/Predicate;I)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda", "builtin.int"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("aggregate") {
            overload(
                "",
                "(Ljava/util/function/Function;)Lcom/mdeo/script/stdlib/impl/collections/ScriptMap;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.Map"
            )
        }

        instanceMethod("map") {
            overload(
                "",
                "(Ljava/util/function/Function;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.ReadonlyCollection"
            )
        }

        instanceMethod("exists") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("forEach") {
            overload(
                "",
                "(Ljava/util/function/Consumer;)V",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda")
            )
        }

        instanceMethod("associate") {
            overload(
                "",
                "(Ljava/util/function/Function;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyMap;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.ReadonlyMap"
            )
        }

        instanceMethod("nMatch") {
            overload(
                "",
                "(Ljava/util/function/Predicate;I)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda", "builtin.int"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("none") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("one") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("reject") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Lcom/mdeo/script/stdlib/impl/collections/Collection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.Collection"
            )
        }

        instanceMethod("rejectOne") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Lcom/mdeo/script/stdlib/impl/collections/Collection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.Collection"
            )
        }

        instanceMethod("filter") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Lcom/mdeo/script/stdlib/impl/collections/Collection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.Collection"
            )
        }

        instanceMethod("all") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Z",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.boolean"
            )
        }

        instanceMethod("find") {
            overload(
                "",
                "(Ljava/util/function/Predicate;)Ljava/lang/Object;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.any?"
            )
        }

        instanceMethod("sortedBy") {
            overload(
                "",
                "(Ljava/util/function/Function;)Lcom/mdeo/script/stdlib/impl/collections/ReadonlyOrderedCollection;",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.ReadonlyOrderedCollection"
            )
        }

        instanceMethod("count") {
            overload(
                "lambda",
                "(Ljava/util/function/Predicate;)I",
                READONLY_COLLECTION,
                isInterface = true,
                parameterTypes = listOf("builtin.lambda"),
                returnType = "builtin.int"
            )
        }
    }
}

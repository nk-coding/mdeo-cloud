import type { Method } from "@mdeo/language-expression";
import { globalFunction, typeRef, voidType, genericTypeRef } from "@mdeo/language-expression";

/**
 * Defines the global stdlib functions available at scope level 0.
 *
 * These functions are accessible throughout the program without
 * requiring imports or object context.
 */

/**
 * println(string) - prints a string followed by a newline.
 */
export const printlnFunction: Method = globalFunction("println")
    .signature((sig) => sig.param("message", typeRef("builtin", "string").build()).returns(voidType()))
    .build();

/**
 * listOf(...args) - creates a mutable list from varargs.
 *
 * @example
 * val items = listOf(1, 2, 3)
 */
export const listOfFunction: Method = globalFunction("listOf")
    .signature((sig) =>
        sig
            .generics("T")
            .param("elements", genericTypeRef("T"))
            .varArgs()
            .returns(
                typeRef("builtin", "List")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
    )
    .build();

/**
 * setOf(...args) - creates a mutable set from varargs.
 *
 * @example
 * val items = setOf("a", "b", "c")
 */
export const setOfFunction: Method = globalFunction("setOf")
    .signature((sig) =>
        sig
            .generics("T")
            .param("elements", genericTypeRef("T"))
            .varArgs()
            .returns(
                typeRef("builtin", "Set")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
    )
    .build();

/**
 * bagOf(...args) - creates a mutable bag from varargs.
 *
 * @example
 * val items = bagOf(1, 1, 2)
 */
export const bagOfFunction: Method = globalFunction("bagOf")
    .signature((sig) =>
        sig
            .generics("T")
            .param("elements", genericTypeRef("T"))
            .varArgs()
            .returns(
                typeRef("builtin", "Bag")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
    )
    .build();

/**
 * orderedSetOf(...args) - creates a mutable ordered set from varargs.
 *
 * @example
 * val items = orderedSetOf("z", "a", "m")
 */
export const orderedSetOfFunction: Method = globalFunction("orderedSetOf")
    .signature((sig) =>
        sig
            .generics("T")
            .param("elements", genericTypeRef("T"))
            .varArgs()
            .returns(
                typeRef("builtin", "OrderedSet")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
    )
    .build();

/**
 * emptyList<T>() - creates an empty mutable list.
 *
 * @example
 * val items = emptyList<string>()
 */
export const emptyListFunction: Method = globalFunction("emptyList")
    .signature((sig) =>
        sig.generics("T").returns(
            typeRef("builtin", "List")
                .withTypeArgs({ T: genericTypeRef("T") })
                .build()
        )
    )
    .build();

/**
 * emptySet<T>() - creates an empty mutable set.
 *
 * @example
 * val items = emptySet<int>()
 */
export const emptySetFunction: Method = globalFunction("emptySet")
    .signature((sig) =>
        sig.generics("T").returns(
            typeRef("builtin", "Set")
                .withTypeArgs({ T: genericTypeRef("T") })
                .build()
        )
    )
    .build();

/**
 * emptyBag<T>() - creates an empty mutable bag.
 *
 * @example
 * val items = emptyBag<string>()
 */
export const emptyBagFunction: Method = globalFunction("emptyBag")
    .signature((sig) =>
        sig.generics("T").returns(
            typeRef("builtin", "Bag")
                .withTypeArgs({ T: genericTypeRef("T") })
                .build()
        )
    )
    .build();

/**
 * emptyOrderedSet<T>() - creates an empty mutable ordered set.
 *
 * @example
 * val items = emptyOrderedSet<int>()
 */
export const emptyOrderedSetFunction: Method = globalFunction("emptyOrderedSet")
    .signature((sig) =>
        sig.generics("T").returns(
            typeRef("builtin", "OrderedSet")
                .withTypeArgs({ T: genericTypeRef("T") })
                .build()
        )
    )
    .build();

/**
 * All stdlib global functions combined.
 */
export const stdlibGlobalFunctions: Method[] = [
    printlnFunction,
    listOfFunction,
    setOfFunction,
    bagOfFunction,
    orderedSetOfFunction,
    emptyListFunction,
    emptySetFunction,
    emptyBagFunction,
    emptyOrderedSetFunction
];

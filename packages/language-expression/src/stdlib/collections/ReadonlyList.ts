import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyList type exported as `ReadonlyListType`.
 */
export const ReadonlyListType = classType("ReadonlyList", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

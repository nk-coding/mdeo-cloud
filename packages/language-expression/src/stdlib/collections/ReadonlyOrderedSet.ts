import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyOrderedSet type exported as `ReadonlyOrderedSetType`.
 */
export const ReadonlyOrderedSetType = classType("ReadonlyOrderedSet", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

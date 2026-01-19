import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyList type exported as `ReadonlyListType`.
 */
export const ReadonlyListType = classType("ReadonlyList")
    .generics("T")
    .extends("ReadonlyOrderedCollection", { T: genericTypeRef("T") })
    .build();

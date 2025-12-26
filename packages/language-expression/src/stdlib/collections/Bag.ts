import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Bag type exported as `BagType`.
 */
export const BagType = classType("Bag", "builtin")
    .generics("T")
    .extends("ReadonlyBag", new Map([["T", genericTypeRef("T")]]))
    .extends("Collection", new Map([["T", genericTypeRef("T")]]))
    .build();

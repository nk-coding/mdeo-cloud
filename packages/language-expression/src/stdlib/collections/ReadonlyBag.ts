import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyBag type exported as `ReadonlyBagType`.
 */
export const ReadonlyBagType = classType("ReadonlyBag", "builtin")
    .generics("T")
    .extends("ReadonlyCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlySet type exported as `ReadonlySetType`.
 */
export const ReadonlySetType = classType("ReadonlySet", "builtin")
    .generics("T")
    .extends("ReadonlyCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

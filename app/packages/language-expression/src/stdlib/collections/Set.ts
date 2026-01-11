import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Set type exported as `SetType`.
 */
export const SetType = classType("Set", "builtin")
    .generics("T")
    .extends("ReadonlySet", new Map([["T", genericTypeRef("T")]]))
    .extends("Collection", new Map([["T", genericTypeRef("T")]]))
    .build();

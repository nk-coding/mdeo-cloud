import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic List type exported as `ListType`.
 */
export const ListType = classType("List", "builtin")
    .generics("T")
    .extends("ReadonlyList", new Map([["T", genericTypeRef("T")]]))
    .extends("OrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

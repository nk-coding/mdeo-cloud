import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const OrderedSetType = classType("OrderedSet", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedSet", new Map([["T", genericTypeRef("T")]]))
    .extends("OrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const ReadonlyOrderedSetType = classType("ReadonlyOrderedSet", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

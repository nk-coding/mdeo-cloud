import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const ReadonlyListType = classType("ReadonlyList", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

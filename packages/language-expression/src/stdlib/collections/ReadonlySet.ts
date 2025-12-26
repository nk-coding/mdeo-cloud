import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const ReadonlySetType = classType("ReadonlySet", "builtin")
    .generics("T")
    .extends("ReadonlyCollection", new Map([["T", genericTypeRef("T")]]))
    .build();

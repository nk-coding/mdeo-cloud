import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const SetType = classType("Set", "builtin")
    .generics("T")
    .extends("ReadonlySet", new Map([["T", genericTypeRef("T")]]))
    .extends("Collection", new Map([["T", genericTypeRef("T")]]))
    .build();

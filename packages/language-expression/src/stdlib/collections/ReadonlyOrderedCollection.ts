import { classType, typeRef, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const ReadonlyOrderedCollectionType = classType("ReadonlyOrderedCollection", "builtin")
    .generics("T")
    .extends("ReadonlyCollection", new Map([["T", genericTypeRef("T")]]))
    .method("at", (m) => m.signature((s) => s.param("index", typeRef("int").build()).returns(genericTypeRef("T"))))
    .method("first", (m) => m.signature((s) => s.returns(genericTypeRef("T"))))
    .method("indexOf", (m) => m.signature((s) => s.param("item", genericTypeRef("T")).returns(typeRef("int").build())))
    .method("invert", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("ReadonlyOrderedCollection")
                    .withTypeArgs(new Map([["T", genericTypeRef("T")]]))
                    .build()
            )
        )
    )
    .method("last", (m) => m.signature((s) => s.returns(genericTypeRef("T"))))
    .build();

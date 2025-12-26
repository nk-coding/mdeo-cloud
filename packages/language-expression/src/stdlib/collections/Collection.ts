import { classType, typeRef, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Collection type exported as `CollectionType`.
 */
export const CollectionType = classType("Collection", "builtin")
    .generics("T")
    .extends("ReadonlyCollection", new Map([["T", genericTypeRef("T")]]))
    .method("add", (m) => m.signature((s) => s.param("item", genericTypeRef("T")).returns(typeRef("boolean").build())))
    .method("addAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs(new Map([["T", genericTypeRef("T")]]))
                        .build()
                )
                .returns(typeRef("boolean").build())
        )
    )
    .method("clear", (m) => m.signature((s) => s.returns(typeRef("void").build())))
    .method("remove", (m) =>
        m.signature((s) => s.param("item", genericTypeRef("T")).returns(typeRef("boolean").build()))
    )
    .method("removeAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs(new Map([["T", genericTypeRef("T")]]))
                        .build()
                )
                .returns(typeRef("boolean").build())
        )
    )
    .build();

import { classType, typeRef, genericTypeRef, lambdaType } from "../../typir-extensions/config/typeBuilder.js";

export const OrderedCollectionType = classType("OrderedCollection", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedCollection", new Map([["T", genericTypeRef("T")]]))
    .method("removeAt", (m) =>
        m.signature((s) => s.param("index", typeRef("int").build()).returns(genericTypeRef("T")))
    )
    .method("sortBy", (m) =>
        m.signature((s) =>
            s
                .generics("U")
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(genericTypeRef("U")))
                .returns(
                    typeRef("OrderedCollection")
                        .withTypeArgs(new Map([["T", genericTypeRef("T")]]))
                        .build()
                )
        )
    )
    .build();

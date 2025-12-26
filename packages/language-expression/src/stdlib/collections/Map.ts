import { classType, typeRef, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

export const MapType = classType("Map", "builtin")
    .generics("K", "V")
    .extends(
        "ReadonlyMap",
        new Map([
            ["K", genericTypeRef("K")],
            ["V", genericTypeRef("V")]
        ])
    )
    .method("clear", (m) => m.signature((s) => s.returns(typeRef("void").build())))
    .method("put", (m) =>
        m.signature((s) =>
            s.param("key", genericTypeRef("K")).param("value", genericTypeRef("V")).returns(typeRef("void").build())
        )
    )
    .method("putAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "map",
                    typeRef("Map")
                        .withTypeArgs(
                            new Map([
                                ["K", genericTypeRef("K")],
                                ["V", genericTypeRef("V")]
                            ])
                        )
                        .build()
                )
                .returns(typeRef("void").build())
        )
    )
    .method("remove", (m) => m.signature((s) => s.param("key", genericTypeRef("K")).returns(genericTypeRef("V"))))
    .build();

import { classType, typeRef, genericTypeRef, voidType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Map type exported as `MapType`.
 */
export const MapType = classType("Map", "builtin")
    .generics("K", "V")
    .extends(
        "ReadonlyMap",
        new Map([
            ["K", genericTypeRef("K")],
            ["V", genericTypeRef("V")]
        ])
    )
    .method("clear", (m) => m.signature((s) => s.returns(voidType())))
    .method("put", (m) =>
        m.signature((s) => s.param("key", genericTypeRef("K")).param("value", genericTypeRef("V")).returns(voidType()))
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
                .returns(voidType())
        )
    )
    .method("remove", (m) => m.signature((s) => s.param("key", genericTypeRef("K")).returns(genericTypeRef("V"))))
    .build();

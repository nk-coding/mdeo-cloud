import { classType, typeRef, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyMap type exported as `ReadonlyMapType`.
 */
export const ReadonlyMapType = classType("ReadonlyMap", "builtin")
    .generics("K", "V")
    .extends("Any")
    .method("containsKey", (m) =>
        m.signature((s) => s.param("key", genericTypeRef("K")).returns(typeRef("boolean").build()))
    )
    .method("containsValue", (m) =>
        m.signature((s) => s.param("value", genericTypeRef("V")).returns(typeRef("boolean").build()))
    )
    .method("get", (m) => m.signature((s) => s.param("key", genericTypeRef("K")).returns(genericTypeRef("V"))))
    .method("isEmpty", (m) => m.signature((s) => s.returns(typeRef("boolean").build())))
    .method("keySet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("ReadonlySet")
                    .withTypeArgs({ T: genericTypeRef("K") })
                    .build()
            )
        )
    )
    .method("size", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .method("values", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("ReadonlyBag")
                    .withTypeArgs({ T: genericTypeRef("V") })
                    .build()
            )
        )
    )
    .build();

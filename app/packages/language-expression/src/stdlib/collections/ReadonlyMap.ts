import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyMap type exported as `ReadonlyMapType`.
 */
export const ReadonlyMapType = classType("ReadonlyMap")
    .generics("K", "V")
    .extends(DefaultTypeNames.Any)
    .method("containsKey", (m) =>
        m.signature((s) => s.param("key", genericTypeRef("K")).returns(typeRef(DefaultTypeNames.String).build()))
    )
    .method("containsValue", (m) =>
        m.signature((s) => s.param("value", genericTypeRef("V")).returns(typeRef(DefaultTypeNames.String).build()))
    )
    .method("get", (m) => m.signature((s) => s.param("key", genericTypeRef("K")).returns(genericTypeRef("V"))))
    .method("isEmpty", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("keySet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("ReadonlySet")
                    .withTypeArgs({ T: genericTypeRef("K") })
                    .build()
            )
        )
    )
    .method("size", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
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

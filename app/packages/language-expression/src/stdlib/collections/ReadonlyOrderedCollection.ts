import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyOrderedCollection type exported as `ReadonlyOrderedCollectionType`.
 */
export const ReadonlyOrderedCollectionType = classType("ReadonlyOrderedCollection")
    .generics("T")
    .extends("ReadonlyCollection", { T: genericTypeRef("T") })
    .method("at", (m) =>
        m.signature((s) => s.param("index", typeRef(DefaultTypeNames.Int).build()).returns(genericTypeRef("T")))
    )
    .method("first", (m) => m.signature((s) => s.returns(genericTypeRef("T"))))
    .method("indexOf", (m) =>
        m.signature((s) => s.param("item", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Int).build()))
    )
    .method("invert", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("ReadonlyOrderedCollection")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("last", (m) => m.signature((s) => s.returns(genericTypeRef("T"))))
    .build();

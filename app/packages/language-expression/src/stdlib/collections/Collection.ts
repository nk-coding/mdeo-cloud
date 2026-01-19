import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef, genericTypeRef, voidType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Collection type exported as `CollectionType`.
 */
export const CollectionType = classType("Collection")
    .generics("T")
    .extends("ReadonlyCollection", { T: genericTypeRef("T") })
    .method("add", (m) =>
        m.signature((s) => s.param("item", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.String).build()))
    )
    .method("addAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
                .returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("clear", (m) => m.signature((s) => s.returns(voidType())))
    .method("remove", (m) =>
        m.signature((s) => s.param("item", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.String).build()))
    )
    .method("removeAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
                .returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .build();

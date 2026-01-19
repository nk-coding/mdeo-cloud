import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in Any type exported as `AnyType`.
 */
export const AnyType = classType(DefaultTypeNames.Any)
    .method("asBag", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Bag")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                    .build()
            )
        )
    )
    .method("asBoolean", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("asInteger", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("asOrderedSet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("OrderedSet")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                    .build()
            )
        )
    )
    .method("asReal", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("asDouble", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("asFloat", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Float).build())))
    .method("asList", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                    .build()
            )
        )
    )
    .method("asSet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Set")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                    .build()
            )
        )
    )
    .method("asString", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("format", (m) =>
        m.signature((s) =>
            s
                .param("pattern", typeRef(DefaultTypeNames.String).build())
                .returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("hasProperty", (m) =>
        m.signature((s) =>
            s.param("name", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("instanceOf", (m) =>
        m.signature((s) => s.param("type", typeRef("Type").build()).returns(typeRef(DefaultTypeNames.String).build()))
    )
    .method("isTypeOf", (m) =>
        m.signature((s) => s.param("type", typeRef("Type").build()).returns(typeRef(DefaultTypeNames.String).build()))
    )
    .method("type", (m) => m.signature((s) => s.returns(typeRef("Type").build())))
    .build();

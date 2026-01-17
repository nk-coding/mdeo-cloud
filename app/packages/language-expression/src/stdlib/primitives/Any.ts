import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in Any type exported as `AnyType`.
 */
export const AnyType = classType("Any", "builtin")
    .method("asBag", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Bag")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("asBoolean", (m) => m.signature((s) => s.returns(typeRef("boolean").build())))
    .method("asInteger", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .method("asOrderedSet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("OrderedSet")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("asReal", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("asDouble", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("asFloat", (m) => m.signature((s) => s.returns(typeRef("float").build())))
    .method("asList", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("asSet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Set")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("asString", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .method("format", (m) =>
        m.signature((s) => s.param("pattern", typeRef("string").build()).returns(typeRef("string").build()))
    )
    .method("hasProperty", (m) =>
        m.signature((s) => s.param("name", typeRef("string").build()).returns(typeRef("boolean").build()))
    )
    .method("instanceOf", (m) =>
        m.signature((s) => s.param("type", typeRef("Type").build()).returns(typeRef("boolean").build()))
    )
    .method("isTypeOf", (m) =>
        m.signature((s) => s.param("type", typeRef("Type").build()).returns(typeRef("boolean").build()))
    )
    .method("type", (m) => m.signature((s) => s.returns(typeRef("Type").build())))
    .build();

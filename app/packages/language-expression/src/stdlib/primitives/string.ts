import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in string type exported as `stringType`.
 */
export const stringType = classType("string", "builtin")
    .extends("Any")
    .method("characterAt", (m) =>
        m.signature((s) => s.param("index", typeRef("int").build()).returns(typeRef("string").build()))
    )
    .method("concat", (m) =>
        m.signature((s) => s.param("str", typeRef("string").build()).returns(typeRef("string").build()))
    )
    .method("endsWith", (m) =>
        m.signature((s) => s.param("str", typeRef("string").build()).returns(typeRef("boolean").build()))
    )
    .method("firstToLowerCase", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .method("firstToUpperCase", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .method("isInteger", (m) => m.signature((s) => s.returns(typeRef("boolean").build())))
    .method("isReal", (m) => m.signature((s) => s.returns(typeRef("boolean").build())))
    .method("isSubstringOf", (m) =>
        m.signature((s) => s.param("str", typeRef("string").build()).returns(typeRef("boolean").build()))
    )
    .method("length", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .method("matches", (m) =>
        m.signature((s) => s.param("reg", typeRef("string").build()).returns(typeRef("boolean").build()))
    )
    .method("pad", (m) =>
        m.signature((s) =>
            s
                .param("length", typeRef("int").build())
                .param("padding", typeRef("string").build())
                .param("right", typeRef("boolean").build())
                .returns(typeRef("string").build())
        )
    )
    .method("replace", (m) =>
        m.signature((s) =>
            s
                .param("reg", typeRef("string").build())
                .param("replacement", typeRef("string").build())
                .returns(typeRef("string").build())
        )
    )
    .method("split", (m) =>
        m.signature((s) =>
            s.param("reg", typeRef("string").build()).returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef("string").build() })
                    .build()
            )
        )
    )
    .method("startsWith", (m) =>
        m.signature((s) => s.param("str", typeRef("string").build()).returns(typeRef("boolean").build()))
    )
    .method("substring", (m) =>
        m
            .signature((s) => s.param("index", typeRef("int").build()).returns(typeRef("string").build()))
            .signature((s) =>
                s
                    .param("startIndex", typeRef("int").build())
                    .param("endIndex", typeRef("int").build())
                    .returns(typeRef("string").build())
            )
    )
    .method("toCharSequence", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef("string").build() })
                    .build()
            )
        )
    )
    .method("toLowerCase", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .method("toUpperCase", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .method("trim", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .build();

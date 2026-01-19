import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in string type exported as `stringType`.
 */
export const stringType = classType(DefaultTypeNames.String)
    .extends(DefaultTypeNames.Any)
    .method("characterAt", (m) =>
        m.signature((s) =>
            s.param("index", typeRef(DefaultTypeNames.Int).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("concat", (m) =>
        m.signature((s) =>
            s.param("str", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("endsWith", (m) =>
        m.signature((s) =>
            s.param("str", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("firstToLowerCase", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("firstToUpperCase", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("isInteger", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("isReal", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("isSubstringOf", (m) =>
        m.signature((s) =>
            s.param("str", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("length", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("matches", (m) =>
        m.signature((s) =>
            s.param("reg", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("pad", (m) =>
        m.signature((s) =>
            s
                .param("length", typeRef(DefaultTypeNames.Int).build())
                .param("padding", typeRef(DefaultTypeNames.String).build())
                .param("right", typeRef(DefaultTypeNames.String).build())
                .returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("replace", (m) =>
        m.signature((s) =>
            s
                .param("reg", typeRef(DefaultTypeNames.String).build())
                .param("replacement", typeRef(DefaultTypeNames.String).build())
                .returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("split", (m) =>
        m.signature((s) =>
            s.param("reg", typeRef(DefaultTypeNames.String).build()).returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.String).build() })
                    .build()
            )
        )
    )
    .method("startsWith", (m) =>
        m.signature((s) =>
            s.param("str", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("substring", (m) =>
        m
            .signature((s) =>
                s
                    .param("index", typeRef(DefaultTypeNames.Int).build())
                    .returns(typeRef(DefaultTypeNames.String).build())
            )
            .signature((s) =>
                s
                    .param("startIndex", typeRef(DefaultTypeNames.Int).build())
                    .param("endIndex", typeRef(DefaultTypeNames.Int).build())
                    .returns(typeRef(DefaultTypeNames.String).build())
            )
    )
    .method("toCharSequence", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.String).build() })
                    .build()
            )
        )
    )
    .method("toLowerCase", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("toUpperCase", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("trim", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .build();

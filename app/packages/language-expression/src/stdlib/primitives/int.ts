import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in int type exported as `intType`.
 */
export const intType = classType(DefaultTypeNames.Int)
    .extends(DefaultTypeNames.Any)
    .method("abs", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("ceiling", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("floor", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("log", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Float).build())))
    .method("log10", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Float).build())))
    .method("max", (m) =>
        m
            .signature(DefaultTypeNames.Int, (s) =>
                s.param("other", typeRef(DefaultTypeNames.Int).build()).returns(typeRef(DefaultTypeNames.Int).build())
            )
            .signature(DefaultTypeNames.Long, (s) =>
                s.param("other", typeRef(DefaultTypeNames.Long).build()).returns(typeRef(DefaultTypeNames.Long).build())
            )
            .signature(DefaultTypeNames.Float, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Float).build())
                    .returns(typeRef(DefaultTypeNames.Float).build())
            )
            .signature(DefaultTypeNames.Double, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Double).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
            )
    )
    .method("min", (m) =>
        m
            .signature(DefaultTypeNames.Int, (s) =>
                s.param("other", typeRef(DefaultTypeNames.Int).build()).returns(typeRef(DefaultTypeNames.Int).build())
            )
            .signature(DefaultTypeNames.Long, (s) =>
                s.param("other", typeRef(DefaultTypeNames.Long).build()).returns(typeRef(DefaultTypeNames.Long).build())
            )
            .signature(DefaultTypeNames.Float, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Float).build())
                    .returns(typeRef(DefaultTypeNames.Float).build())
            )
            .signature(DefaultTypeNames.Double, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Double).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
            )
    )
    .method("pow", (m) =>
        m.signature((s) =>
            s
                .param("exponent", typeRef(DefaultTypeNames.Double).build())
                .returns(typeRef(DefaultTypeNames.Double).build())
        )
    )
    .method("round", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("iota", (m) =>
        m.signature((s) =>
            s
                .param("end", typeRef(DefaultTypeNames.Int).build())
                .param("step", typeRef(DefaultTypeNames.Int).build())
                .returns(
                    typeRef("List")
                        .withTypeArgs({ T: typeRef(DefaultTypeNames.Int).build() })
                        .build()
                )
        )
    )
    .method("mod", (m) =>
        m.signature((s) =>
            s.param("divisor", typeRef(DefaultTypeNames.Int).build()).returns(typeRef(DefaultTypeNames.Int).build())
        )
    )
    .method("to", (m) =>
        m.signature((s) =>
            s.param("other", typeRef(DefaultTypeNames.Int).build()).returns(
                typeRef("List")
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.Int).build() })
                    .build()
            )
        )
    )
    .method("toBinary", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("toHex", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .build();

import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

export const longType = classType("long", "builtin")
    .extends("Any")
    .method("abs", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .method("ceiling", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .method("floor", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .method("log", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("log10", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("max", (m) =>
        m
            .signature((s) => s.param("other", typeRef("int").build()).returns(typeRef("long").build()))
            .signature((s) => s.param("other", typeRef("long").build()).returns(typeRef("long").build()))
            .signature((s) => s.param("other", typeRef("float").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("min", (m) =>
        m
            .signature((s) => s.param("other", typeRef("int").build()).returns(typeRef("long").build()))
            .signature((s) => s.param("other", typeRef("long").build()).returns(typeRef("long").build()))
            .signature((s) => s.param("other", typeRef("float").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("pow", (m) =>
        m.signature((s) => s.param("exponent", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("round", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .method("iota", (m) =>
        m.signature((s) =>
            s
                .param("end", typeRef("long").build())
                .param("step", typeRef("long").build())
                .returns(
                    typeRef("List")
                        .withTypeArgs(new Map([["T", typeRef("int").build()]]))
                        .build()
                )
        )
    )
    .method("mod", (m) =>
        m.signature((s) => s.param("divisor", typeRef("long").build()).returns(typeRef("long").build()))
    )
    .method("to", (m) =>
        m.signature((s) =>
            s.param("other", typeRef("long").build()).returns(
                typeRef("List")
                    .withTypeArgs(new Map([["T", typeRef("int").build()]]))
                    .build()
            )
        )
    )
    .method("toBinary", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .method("toHex", (m) => m.signature((s) => s.returns(typeRef("string").build())))
    .build();

import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

export const doubleType = classType("double", "builtin")
    .extends("Any")
    .method("abs", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("ceiling", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .method("floor", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .method("log", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("log10", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("max", (m) =>
        m
            .signature((s) => s.param("other", typeRef("int").build()).returns(typeRef("double").build()))
            .signature((s) => s.param("other", typeRef("long").build()).returns(typeRef("double").build()))
            .signature((s) => s.param("other", typeRef("float").build()).returns(typeRef("double").build()))
            .signature((s) => s.param("other", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("min", (m) =>
        m
            .signature((s) => s.param("other", typeRef("int").build()).returns(typeRef("double").build()))
            .signature((s) => s.param("other", typeRef("long").build()).returns(typeRef("double").build()))
            .signature((s) => s.param("other", typeRef("float").build()).returns(typeRef("double").build()))
            .signature((s) => s.param("other", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("pow", (m) =>
        m.signature((s) => s.param("exponent", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("round", (m) => m.signature((s) => s.returns(typeRef("long").build())))
    .build();

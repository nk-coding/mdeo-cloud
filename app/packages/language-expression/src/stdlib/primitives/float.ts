import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in float type exported as `floatType`.
 */
export const floatType = classType("float", "builtin")
    .extends("Any")
    .method("abs", (m) => m.signature((s) => s.returns(typeRef("float").build())))
    .method("ceiling", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .method("floor", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .method("log", (m) => m.signature((s) => s.returns(typeRef("float").build())))
    .method("log10", (m) => m.signature((s) => s.returns(typeRef("float").build())))
    .method("max", (m) =>
        m
            .signature((s) => s.param("other", typeRef("int").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("long").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("float").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("min", (m) =>
        m
            .signature((s) => s.param("other", typeRef("int").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("long").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("float").build()).returns(typeRef("float").build()))
            .signature((s) => s.param("other", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("pow", (m) =>
        m.signature((s) => s.param("exponent", typeRef("double").build()).returns(typeRef("double").build()))
    )
    .method("round", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .build();

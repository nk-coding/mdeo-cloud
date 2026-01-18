import { classType, typeRef, lambdaType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyCollection type exported as `ReadonlyCollectionType`.
 */
export const ReadonlyCollectionType = classType("ReadonlyCollection", "builtin")
    .generics("T")
    .extends("Any")
    .extends("Iterable", { T: genericTypeRef("T") })
    .method("asBag", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Bag")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("asOrderedSet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("OrderedSet")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("asList", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("List")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("asSet", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Set")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("clone", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Collection")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("concat", (m) =>
        m
            .signature("nosep", (s) => s.returns(typeRef("string").build()))
            .signature("sep", (s) => s.param("separator", typeRef("string").build()).returns(typeRef("string").build()))
    )
    .method("count", (m) =>
        m
            .signature("noit", (s) => s.param("item", typeRef("Any").build()).returns(typeRef("int").build()))
            .signature("it", (s) =>
                s
                    .param(
                        "iterator",
                        lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build())
                    )
                    .returns(typeRef("int").build())
            )
    )
    .method("excludes", (m) =>
        m.signature((s) => s.param("item", typeRef("Any").build()).returns(typeRef("boolean").build()))
    )
    .method("excludesAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: typeRef("Any").build() })
                        .build()
                )
                .returns(typeRef("boolean").build())
        )
    )
    .method("excluding", (m) =>
        m.signature((s) =>
            s.param("item", typeRef("Any").build()).returns(
                typeRef("Collection")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("excludingAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: typeRef("Any").build() })
                        .build()
                )
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("flatten", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Collection")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("includes", (m) =>
        m.signature((s) => s.param("item", typeRef("Any").build()).returns(typeRef("boolean").build()))
    )
    .method("includesAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: typeRef("Any").build() })
                        .build()
                )
                .returns(typeRef("boolean").build())
        )
    )
    .method("including", (m) =>
        m.signature((s) =>
            s.param("item", typeRef("Any").build()).returns(
                typeRef("Collection")
                    .withTypeArgs({ T: genericTypeRef("T") })
                    .build()
            )
        )
    )
    .method("includingAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: typeRef("Any").build() })
                        .build()
                )
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("isEmpty", (m) => m.signature((s) => s.returns(typeRef("boolean").build())))
    .method("notEmpty", (m) => m.signature((s) => s.returns(typeRef("boolean").build())))
    .method("random", (m) => m.signature((s) => s.returns(typeRef("Any").build())))
    .method("size", (m) => m.signature((s) => s.returns(typeRef("int").build())))
    .method("sum", (m) => m.signature((s) => s.returns(typeRef("double").build())))
    .method("atLeastNMatch", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .param("n", typeRef("int").build())
                .returns(typeRef("boolean").build())
        )
    )
    .method("atMostNMatch", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .param("n", typeRef("int").build())
                .returns(typeRef("boolean").build())
        )
    )
    .method("aggregate", (m) =>
        m.signature((s) =>
            s.param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("Any").build())).returns(
                typeRef("Map")
                    .withTypeArgs({
                        K: typeRef("Any").build(),
                        V: typeRef("Any").build()
                    })
                    .build()
            )
        )
    )
    .method("map", (m) =>
        m.signature((s) =>
            s
                .generics("U")
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(genericTypeRef("U")))
                .returns(
                    typeRef("ReadonlyCollection")
                        .withTypeArgs({ T: genericTypeRef("U") })
                        .build()
                )
        )
    )
    .method("exists", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(typeRef("boolean").build())
        )
    )
    .method("forAll", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(typeRef("boolean").build())
        )
    )
    .method("associate", (m) =>
        m.signature((s) =>
            s
                .generics("U")
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(genericTypeRef("U")))
                .returns(
                    typeRef("ReadonlyMap")
                        .withTypeArgs({
                            K: genericTypeRef("T"),
                            V: genericTypeRef("U")
                        })
                        .build()
                )
        )
    )
    .method("nMatch", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .param("n", typeRef("int").build())
                .returns(typeRef("boolean").build())
        )
    )
    .method("none", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(typeRef("boolean").build())
        )
    )
    .method("one", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(typeRef("boolean").build())
        )
    )
    .method("reject", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("rejectOne", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("filter", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("filterByKind", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Collection")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("filterByType", (m) =>
        m.signature((s) =>
            s.returns(
                typeRef("Collection")
                    .withTypeArgs({ T: typeRef("Any").build() })
                    .build()
            )
        )
    )
    .method("find", (m) =>
        m.signature((s) =>
            s
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(typeRef("boolean").build()))
                .returns(typeRef("Any").build())
        )
    )
    .method("sortedBy", (m) =>
        m.signature((s) =>
            s
                .generics("U")
                .param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(genericTypeRef("U")))
                .returns(
                    typeRef("ReadonlyOrderedCollection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .build();

import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef, lambdaType, genericTypeRef, voidType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyCollection type exported as `ReadonlyCollectionType`.
 */
export const ReadonlyCollectionType = classType("ReadonlyCollection")
    .generics("T")
    .extends(DefaultTypeNames.Any)
    .extends(DefaultTypeNames.Iterable, { T: genericTypeRef("T") })
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
            .signature("nosep", (s) => s.returns(typeRef(DefaultTypeNames.String).build()))
            .signature("sep", (s) =>
                s
                    .param("separator", typeRef(DefaultTypeNames.String).build())
                    .returns(typeRef(DefaultTypeNames.String).build())
            )
    )
    .method("count", (m) =>
        m
            .signature("noit", (s) =>
                s.param("item", typeRef(DefaultTypeNames.Any).build()).returns(typeRef(DefaultTypeNames.Int).build())
            )
            .signature("it", (s) =>
                s
                    .param(
                        "iterator",
                        lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                    )
                    .returns(typeRef(DefaultTypeNames.Int).build())
            )
    )
    .method("excludes", (m) =>
        m.signature((s) =>
            s.param("item", typeRef(DefaultTypeNames.Any).build()).returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("excludesAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                        .build()
                )
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("excluding", (m) =>
        m.signature((s) =>
            s.param("item", typeRef(DefaultTypeNames.Any).build()).returns(
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
                        .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
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
                    .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                    .build()
            )
        )
    )
    .method("includes", (m) =>
        m.signature((s) =>
            s.param("item", typeRef(DefaultTypeNames.Any).build()).returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("includesAll", (m) =>
        m.signature((s) =>
            s
                .param(
                    "col",
                    typeRef("Collection")
                        .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                        .build()
                )
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("including", (m) =>
        m.signature((s) =>
            s.param("item", typeRef(DefaultTypeNames.Any).build()).returns(
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
                        .withTypeArgs({ T: typeRef(DefaultTypeNames.Any).build() })
                        .build()
                )
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("isEmpty", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Boolean).build())))
    .method("notEmpty", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Boolean).build())))
    .method("random", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Any).build())))
    .method("size", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("sum", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("atLeastNMatch", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .param("n", typeRef(DefaultTypeNames.Int).build())
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("atMostNMatch", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .param("n", typeRef(DefaultTypeNames.Int).build())
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("aggregate", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Any).build())
                )
                .returns(
                    typeRef("Map")
                        .withTypeArgs({
                            K: typeRef(DefaultTypeNames.Any).build(),
                            V: typeRef(DefaultTypeNames.Any).build()
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
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("U") })
                        .build()
                )
        )
    )
    .method("exists", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("forEach", (m) =>
        m.signature((s) =>
            s.param("iterator", lambdaType().param("it", genericTypeRef("T")).returns(voidType())).returns(voidType())
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
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .param("n", typeRef(DefaultTypeNames.Int).build())
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("none", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("one", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("reject", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
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
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
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
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .returns(
                    typeRef("Collection")
                        .withTypeArgs({ T: genericTypeRef("T") })
                        .build()
                )
        )
    )
    .method("all", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .returns(typeRef(DefaultTypeNames.Boolean).build())
        )
    )
    .method("find", (m) =>
        m.signature((s) =>
            s
                .param(
                    "iterator",
                    lambdaType().param("it", genericTypeRef("T")).returns(typeRef(DefaultTypeNames.Boolean).build())
                )
                .returns(typeRef(DefaultTypeNames.Any).build())
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

import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in double type exported as `doubleType`.
 */
export const doubleType = classType(DefaultTypeNames.Double)
    .extends(DefaultTypeNames.Any)
    .method("abs", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("ceiling", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Long).build())))
    .method("floor", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Long).build())))
    .method("log", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("log10", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("max", (m) =>
        m
            .signature(DefaultTypeNames.Int, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Int).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
            )
            .signature(DefaultTypeNames.Long, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Long).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
            )
            .signature(DefaultTypeNames.Float, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Float).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
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
                s
                    .param("other", typeRef(DefaultTypeNames.Int).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
            )
            .signature(DefaultTypeNames.Long, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Long).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
            )
            .signature(DefaultTypeNames.Float, (s) =>
                s
                    .param("other", typeRef(DefaultTypeNames.Float).build())
                    .returns(typeRef(DefaultTypeNames.Double).build())
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
    .method("round", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Long).build())))
    .build();

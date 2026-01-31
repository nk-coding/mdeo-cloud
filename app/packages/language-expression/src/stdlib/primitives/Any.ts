import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in Any type exported as `AnyType`.
 */
export const AnyType = classType(DefaultTypeNames.Any)
    .method("asBoolean", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Boolean).build())))
    .method("asInteger", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Int).build())))
    .method("asReal", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("asDouble", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Double).build())))
    .method("asFloat", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.Float).build())))
    .method("toString", (m) => m.signature((s) => s.returns(typeRef(DefaultTypeNames.String).build())))
    .method("format", (m) =>
        m.signature((s) =>
            s
                .param("pattern", typeRef(DefaultTypeNames.String).build())
                .returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .method("hasProperty", (m) =>
        m.signature((s) =>
            s.param("name", typeRef(DefaultTypeNames.String).build()).returns(typeRef(DefaultTypeNames.String).build())
        )
    )
    .build();

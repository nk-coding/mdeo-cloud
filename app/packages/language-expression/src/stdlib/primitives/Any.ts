import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in Any type exported as `AnyType`.
 */
export const AnyType = classType(DefaultTypeNames.Any)
    .method("asBoolean", (m) => m.signature((s) => s.returns(typeRef("builtin", DefaultTypeNames.Boolean).build())))
    .method("asInteger", (m) => m.signature((s) => s.returns(typeRef("builtin", DefaultTypeNames.Int).build())))
    .method("asReal", (m) => m.signature((s) => s.returns(typeRef("builtin", DefaultTypeNames.Double).build())))
    .method("asDouble", (m) => m.signature((s) => s.returns(typeRef("builtin", DefaultTypeNames.Double).build())))
    .method("asFloat", (m) => m.signature((s) => s.returns(typeRef("builtin", DefaultTypeNames.Float).build())))
    .method("toString", (m) => m.signature((s) => s.returns(typeRef("builtin", DefaultTypeNames.String).build())))
    .method("format", (m) =>
        m.signature((s) =>
            s
                .param("pattern", typeRef("builtin", DefaultTypeNames.String).build())
                .returns(typeRef("builtin", DefaultTypeNames.String).build())
        )
    )
    .method("hasProperty", (m) =>
        m.signature((s) =>
            s
                .param("name", typeRef("builtin", DefaultTypeNames.String).build())
                .returns(typeRef("builtin", DefaultTypeNames.String).build())
        )
    )
    .build();

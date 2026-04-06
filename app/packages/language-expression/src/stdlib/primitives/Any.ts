import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType, typeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in Any type exported as `AnyType`.
 */
export const AnyType = classType(DefaultTypeNames.Any)
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

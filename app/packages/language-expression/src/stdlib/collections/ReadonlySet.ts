import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlySet type exported as `ReadonlySetType`.
 */
export const ReadonlySetType = classType("ReadonlySet", "builtin")
    .generics("T")
    .extends("ReadonlyCollection", { T: genericTypeRef("T") })
    .build();

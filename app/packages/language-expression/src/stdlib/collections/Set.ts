import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Set type exported as `SetType`.
 */
export const SetType = classType("Set")
    .generics("T")
    .extends("ReadonlySet", { T: genericTypeRef("T") })
    .extends("Collection", { T: genericTypeRef("T") })
    .build();

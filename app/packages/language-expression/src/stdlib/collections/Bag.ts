import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Bag type exported as `BagType`.
 */
export const BagType = classType("Bag")
    .generics("T")
    .extends("ReadonlyBag", { T: genericTypeRef("T") })
    .extends("Collection", { T: genericTypeRef("T") })
    .build();

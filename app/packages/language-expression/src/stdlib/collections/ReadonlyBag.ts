import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyBag type exported as `ReadonlyBagType`.
 */
export const ReadonlyBagType = classType("ReadonlyBag")
    .generics("T")
    .extends("ReadonlyCollection", { T: genericTypeRef("T") })
    .build();

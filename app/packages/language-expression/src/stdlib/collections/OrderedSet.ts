import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic OrderedSet type exported as `OrderedSetType`.
 */
export const OrderedSetType = classType("OrderedSet", "builtin")
    .generics("T")
    .extends("ReadonlyOrderedSet", { T: genericTypeRef("T") })
    .extends("OrderedCollection", { T: genericTypeRef("T") })
    .build();

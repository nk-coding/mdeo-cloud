import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic ReadonlyOrderedSet type exported as `ReadonlyOrderedSetType`.
 */
export const ReadonlyOrderedSetType = classType("ReadonlyOrderedSet")
    .generics("T")
    .extends("ReadonlyOrderedCollection", { T: genericTypeRef("T") })
    .build();

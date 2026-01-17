import { classType, genericTypeRef } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic List type exported as `ListType`.
 */
export const ListType = classType("List", "builtin")
    .generics("T")
    .extends("ReadonlyList", { T: genericTypeRef("T") })
    .extends("OrderedCollection", { T: genericTypeRef("T") })
    .build();

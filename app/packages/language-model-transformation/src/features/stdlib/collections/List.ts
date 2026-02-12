import { ListType, classTypeFrom, genericTypeRef } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable List type for model transformation.
 */
export const ModelTransformationListType: ClassType = classTypeFrom(ListType)
    .clearExtends()
    .extends("OrderedCollection", { T: genericTypeRef("T") })
    .build();

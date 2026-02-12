import { OrderedSetType, classTypeFrom, genericTypeRef } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable OrderedSet type for model transformation.
 */
export const ModelTransformationOrderedSetType: ClassType = classTypeFrom(OrderedSetType)
    .clearExtends()
    .extends("OrderedCollection", { T: genericTypeRef("T") })
    .build();

import { OrderedCollectionType, classTypeFrom, genericTypeRef } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable OrderedCollection type for model transformation.
 */
export const ModelTransformationOrderedCollectionType: ClassType = classTypeFrom(OrderedCollectionType)
    .clearExtends()
    .extends("Collection", { T: genericTypeRef("T") })
    .build();

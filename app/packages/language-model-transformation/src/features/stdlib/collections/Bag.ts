import { BagType, classTypeFrom, genericTypeRef } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable Bag type for model transformation.
 */
export const ModelTransformationBagType: ClassType = classTypeFrom(BagType)
    .clearExtends()
    .extends("Collection", { T: genericTypeRef("T") })
    .build();

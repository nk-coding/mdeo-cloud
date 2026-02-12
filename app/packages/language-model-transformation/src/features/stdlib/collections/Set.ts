import { SetType, classTypeFrom, genericTypeRef } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable Set type for model transformation.
 */
export const ModelTransformationSetType: ClassType = classTypeFrom(SetType)
    .clearExtends()
    .extends("Collection", { T: genericTypeRef("T") })
    .build();

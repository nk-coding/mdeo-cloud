import { ReadonlyCollectionType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable ReadonlyCollection type for model transformation.
 */
export const ModelTransformationReadonlyCollectionType: ClassType = classTypeFrom(ReadonlyCollectionType)
    .keepMembers(
        "size",
        "isEmpty",
        "notEmpty",
        "sum",
        "first",
        "last",
        "filter",
        "map",
        "exists",
        "all",
        "none",
        "one",
        "find",
        "reject"
    )
    .build();

import { ReadonlyOrderedCollectionType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable ReadonlyOrderedCollection type for model transformation.
 */
export const ModelTransformationReadonlyOrderedCollectionType: ClassType = classTypeFrom(ReadonlyOrderedCollectionType)
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

import { OrderedCollectionType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable OrderedCollection type for model transformation.
 */
export const ModelTransformationOrderedCollectionType: ClassType = classTypeFrom(OrderedCollectionType)
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

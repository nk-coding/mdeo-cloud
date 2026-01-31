import { OrderedSetType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable OrderedSet type for model transformation.
 */
export const ModelTransformationOrderedSetType: ClassType = classTypeFrom(OrderedSetType)
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

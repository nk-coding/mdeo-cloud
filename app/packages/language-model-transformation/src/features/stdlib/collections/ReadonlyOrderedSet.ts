import { ReadonlyOrderedSetType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable ReadonlyOrderedSet type for model transformation.
 */
export const ModelTransformationReadonlyOrderedSetType: ClassType = classTypeFrom(ReadonlyOrderedSetType)
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

import { ReadonlySetType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable ReadonlySet type for model transformation.
 */
export const ModelTransformationReadonlySetType: ClassType = classTypeFrom(ReadonlySetType)
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

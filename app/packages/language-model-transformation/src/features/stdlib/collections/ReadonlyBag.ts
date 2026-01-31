import { ReadonlyBagType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Immutable ReadonlyBag type for model transformation.
 */
export const ModelTransformationReadonlyBagType: ClassType = classTypeFrom(ReadonlyBagType)
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

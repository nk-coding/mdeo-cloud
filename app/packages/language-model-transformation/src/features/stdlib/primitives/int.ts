import { intType, classTypeFrom } from "@mdeo/language-expression";
import type { ClassType } from "@mdeo/language-expression";

/**
 * Int type for model transformation.
 */
export const modelTransformationIntType: ClassType = classTypeFrom(intType)
    .keepMembers("abs", "floor", "ceiling", "round", "log", "log10", "pow", "mod", "max", "min")
    .build();

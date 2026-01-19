import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in boolean type exported as `booleanType`.
 */
export const booleanType = classType(DefaultTypeNames.Boolean).extends(DefaultTypeNames.Any).build();

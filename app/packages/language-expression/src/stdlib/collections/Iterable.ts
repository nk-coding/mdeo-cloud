import { DefaultTypeNames } from "../../type-system/typeSystemConfig.js";
import { classType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Iterable type exported as `IterableType`
 */
export const IterableType = classType(DefaultTypeNames.Iterable).generics("T").extends(DefaultTypeNames.Any).build();

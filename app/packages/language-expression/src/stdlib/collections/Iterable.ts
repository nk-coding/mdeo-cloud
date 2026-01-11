import { classType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in generic Iterable type exported as `IterableType`
 */
export const IterableType = classType("Iterable", "builtin").generics("T").extends("Any").build();

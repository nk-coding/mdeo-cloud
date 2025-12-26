import { classType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The `void` type represents the absence of a value.
 */
export const voidType = classType("void", "builtin").build();
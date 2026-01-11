import { classType } from "../../typir-extensions/config/typeBuilder.js";

/**
 * The built-in boolean type exported as `booleanType`.
 */
export const booleanType = classType("boolean", "builtin").extends("Any").build();

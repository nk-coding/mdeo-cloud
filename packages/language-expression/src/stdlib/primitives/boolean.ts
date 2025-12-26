import { classType } from "../../typir-extensions/config/typeBuilder.js";

export const booleanType = classType("boolean", "builtin").extends("Any").build();

import type { GrammarAST } from "langium";
import type { Primitive } from "./types.js";

/**
 * Maps TypeScript primitive type constructors to their corresponding Langium grammar primitive types.
 * 
 * This lookup table is essential for converting TypeScript type definitions into Langium grammar
 * constructs. It bridges the gap between TypeScript's type system and Langium's grammar definition
 * language, enabling type-safe parsing and code generation.
 * 
 * The mapping handles the standard JavaScript/TypeScript primitive types:
 * - `String` → `"string"` - For text values
 * - `Number` → `"number"` - For numeric values  
 * - `Boolean` → `"boolean"` - For true/false values
 * - `BigInt` → `"bigint"` - For large integer values
 * - `Date` → `"Date"` - For date/time values
 */
export const primitiveTypeLookup = new Map<Primitive, GrammarAST.PrimitiveType>(
    [
        [String, "string"],
        [Number, "number"],
        [Boolean, "boolean"],
        [BigInt, "bigint"],
        [Date, "Date"],
    ]
);

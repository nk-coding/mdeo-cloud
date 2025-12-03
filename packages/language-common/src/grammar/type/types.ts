import type { AstNode } from "langium";
import type { Interface } from "./interface/types.js";
import type { Type } from "./type/types.js";

/**
 * Union type representing all possible base types in the type system.
 * Base types are the fundamental building blocks for defining grammar structures
 * and can be either interfaces (for structured data) or types (for unions).
 * 
 * @template T The AST node type that this base type represents
 */
export type BaseType<T extends AstNode> = Interface<T> | Type<T>;

/**
 * Union type representing all supported primitive TypeScript type constructors.
 * These primitives can be used in interface definitions and terminal rules
 * to define the basic data types that the grammar can handle.
 * 
 * Supported primitives:
 * - `String` - For text values
 * - `Number` - For numeric values
 * - `Boolean` - For true/false values  
 * - `BigInt` - For large integer values
 * - `Date` - For date/time values
 */
export type Primitive = typeof String | typeof Number | typeof Boolean | typeof BigInt | typeof Date;

/**
 * Utility type that extracts the TypeScript AST node type from a base type definition.
 * This type mapping is crucial for maintaining type safety when working with
 * grammar definitions and their corresponding TypeScript representations.
 * 
 * The type mapping works as follows:
 * - If `T` is an `Interface<U>`, returns `U` (the AST node type)
 * - If `T` is a `Type<U>`, returns `U` (the union AST node type)
 * - Otherwise, returns `never` (should not happen with proper usage)
 * 
 * @template T The base type to extract the AST node type from
 * 
 * @example
 * ```typescript
 * // Define an interface
 * const PersonInterface = createInterface("Person").attrs({
 *     name: String,
 *     age: Number
 * });
 * 
 * // Extract the TypeScript type
 * type PersonType = ASTType<typeof PersonInterface>;
 * // PersonType is now: { name: string; age: number } & AstNode
 * 
 * // Define a union type  
 * const ExpressionType = createType("Expression").types(NumberExpr, StringExpr);
 * type ExpressionType = ASTType<typeof ExpressionType>;
 * // ExpressionType is now: (NumberExprType | StringExprType)
 * ```
 */
export type ASTType<T extends BaseType<any>> = T extends Interface<infer U> ? U : T extends Type<infer U> ? U : never;

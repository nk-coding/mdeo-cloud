import type { SimpleInterfaceDeclarationValue, OptionalType, RefType, UnionType, Resolve, TypeType } from "./types.js";

/**
 * Type guard to check if a value is wrapped in the Optional type.
 * 
 * @param type The value to check
 * @returns True if the type is Optional, false otherwise
 */
export function isOptional(
    type: unknown
): type is OptionalType<SimpleInterfaceDeclarationValue> {
    return typeof type === "object" && type !== null && "optional" in type;
}

/**
 * Creates an Optional wrapper that makes an interface attribute optional.
 * Optional attributes may be undefined in the resulting AST node.
 * 
 * @template T The type to make optional
 * @param type The value type to wrap as optional
 * @returns An Optional wrapper containing the type
 * 
 * @example
 * ```typescript
 * const PersonInterface = createInterface("Person").attrs({
 *     name: String,                    // Required property
 *     age: Optional(Number),           // Optional property: age?: number | undefined
 *     email: Optional(String)          // Optional property: email?: string | undefined
 * });
 * ```
 */
export function Optional<T extends SimpleInterfaceDeclarationValue>(
    type: T
): OptionalType<T> {
    return { optional: type };
}

/**
 * Creates a Ref wrapper that represents a cross-reference to another AST node.
 * References allow one part of the AST to point to another part by name or identifier.
 * 
 * @template T The type being referenced
 * @param type The type to create a reference to
 * @returns A Ref wrapper for cross-referencing
 * 
 * @example
 * ```typescript
 * const FunctionCall = createInterface("FunctionCall").attrs({
 *     name: String,
 *     target: Ref(() => FunctionDeclaration)  // Reference to function definition
 * });
 * 
 * // In grammar: target will be a Reference<FunctionDeclaration>
 * // that can be resolved to find the actual function definition
 * ```
 */
export function Ref<T extends TypeType>(type: T): RefType<T> {
    return { ref: type };
}

/**
 * Type guard to check if a value is wrapped in the Ref type.
 * 
 * @param type The value to check
 * @returns True if the type is a reference, false otherwise
 */
export function isReference(type: unknown): type is RefType<TypeType> {
    return typeof type === "object" && type !== null && "ref" in type;
}

/**
 * Creates a Union wrapper that represents a union of string literal types.
 * Unions allow an attribute to accept one of several predefined string values.
 * 
 * @template T The union of string literal types
 * @param types The string literal values that form the union
 * @returns A Union wrapper containing the string literals
 * 
 * @example
 * ```typescript
 * const BinaryExpression = createInterface("BinaryExpression").attrs({
 *     left: Expression,
 *     right: Expression,
 *     operator: Union("+", "-", "*", "/", "==", "!=")  // operator: "+" | "-" | "*" | "/" | "==" | "!="
 * });
 * ```
 */
export function Union<T extends string>(...types: T[]): UnionType<T> {
    return { union: types };
}

/**
 * Type guard to check if a value is wrapped in the Union type.
 * 
 * @param type The value to check
 * @returns True if the type is a union, false otherwise
 */
export function isUnion(type: unknown): type is UnionType<string> {
    return typeof type === "object" && type !== null && "union" in type;
}

/**
 * Creates a Resolve wrapper that resolves a type reference to the actual AST node type.
 * Resolve types embed the actual AST node rather than just a reference to it.
 * This only needs to be used for some circular references, where typescript otherwise cannot infer the correct type.
 * In all other cases, using the type directly is sufficient.
 * 
 * @template T The type to resolve
 * @param type The type to resolve to its actual value
 * @returns A Resolve wrapper for the type
 * 
 * @example
 * ```typescript
 * const ClassDeclaration = createInterface("ClassDeclaration").attrs({
 *     name: String,
 *     methods: [R(() => MethodDeclaration)],  // Embed full method data
 *     parent: Ref(() => ClassDeclaration)           // Just reference to parent class
 * });
 * 
 * // methods contains actual MethodDeclaration objects
 * // parent contains a Reference<ClassDeclaration> that needs to be resolved
 * ```
 */
export function R<T extends TypeType>(type: T): Resolve<T> {
    return { resolve: type };
}

/**
 * Type guard to check if a value is wrapped in the Resolve type.
 * 
 * @param type The value to check
 * @returns True if the type is a resolve wrapper, false otherwise
 */
export function isResolve(type: unknown): type is Resolve<TypeType> {
    return typeof type === "object" && type !== null && "resolve" in type;
}

import type { AstNode, Reference, GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { BaseType, Primitive } from "../types.js";
import type { SerializableExternalReference } from "../../serialization/grammarSerializer.js";

/**
 * Represents an interface definition in the grammar that corresponds to a structured AST node.
 * Interfaces define the shape and properties of language constructs in the grammar.
 *
 * @template T The AST node type that this interface represents
 */
export interface Interface<T extends AstNode> {
    /**
     * TypeScript type information for the AST node this interface represents.
     * Only used for type inference and validation during compilation,
     * never assigned at runtime.
     */
    tsType?: T;

    /**
     * The name of the interface as it appears in the grammar.
     */
    name: string;

    /**
     * Converts this interface into a serializable grammar node that can be used
     * in the Langium grammar generation process.
     *
     * @returns A serializable representation of the interface or external reference
     */
    toType: () => SerializableGrammarNode<GrammarAST.Interface> | SerializableExternalReference;
}

/**
 * Utility type that merges multiple interface types through intersection.
 * This enables interface inheritance where a derived interface combines
 * properties from multiple parent interfaces.
 *
 * @template T Array of interface types to merge
 */
type MergeInterface<T extends Interface<AstNode>[]> = T extends [Interface<infer First>, ...infer Rest]
    ? First & MergeInterface<Rest extends Interface<AstNode>[] ? Rest : []>
    : object;

/**
 * Utility type that creates a clean object type representation by distributing
 * intersection types. This improves TypeScript's type display and IntelliSense.
 *
 * @template T The type to prettify
 */
type Prettify<T> = { [K in keyof T]: T[K] } & {};

/**
 * Union type representing all possible simple value types that can be used
 * in interface attribute declarations. These are the basic building blocks
 * for defining interface properties.
 */
export type SimpleInterfaceDeclarationValue = Primitive | TypeType | RefType<any> | UnionType<any> | Resolve<any>;

/**
 * Union type representing all possible value types for interface attributes.
 * This includes simple values, arrays of simple values, and optional values.
 */
export type InterfaceDeclarationValue =
    | SimpleInterfaceDeclarationValue
    | [SimpleInterfaceDeclarationValue]
    | OptionalType<SimpleInterfaceDeclarationValue>;

/**
 * Type representing the complete attribute declaration for an interface.
 * Maps attribute names to their corresponding value types.
 */
export type InterfaceDeclaration = Record<string, InterfaceDeclarationValue>;

/**
 * Represents a type reference that can be either a direct base type or
 * a function that returns a base type (for handling circular references).
 */
export type TypeType = BaseType<any> | (() => BaseType<any>);

/**
 * Wrapper type that makes an interface attribute optional.
 * Optional attributes may be undefined in the resulting AST node.
 *
 * @template T The type of the optional value
 */
export interface OptionalType<T extends SimpleInterfaceDeclarationValue> {
    /**
     * The underlying type that is made optional.
     */
    optional: T;
}

/**
 * Wrapper type that creates a cross-reference to another AST node.
 * References allow one part of the AST to point to another part by name,
 * enabling relationships between different language constructs.
 *
 * @template T The type being referenced
 */
export interface RefType<T extends TypeType> {
    /**
     * The type being referenced.
     */
    ref: T;
}

/**
 * Wrapper type that creates a union of string literal types.
 * Unions allow an attribute to accept one of several predefined string values.
 *
 * @template T The union of string literal types
 */
export interface UnionType<T extends string> {
    /**
     * Array of string literal values that form the union.
     */
    union: T[];
}

/**
 * Wrapper type that resolves a type reference to the actual AST node type.
 * Resolve types are used when you want to embed the actual AST node rather
 * than just a reference to it.
 *
 * @template T The type to resolved
 */
export interface Resolve<T extends TypeType> {
    /**
     * The type to resolve to its actual value.
     */
    resolve: T;
}

/**
 * Complex type mapping that converts interface declaration value types
 * to their corresponding TypeScript types. This is the core type transformation
 * logic that bridges grammar definitions to TypeScript types.
 *
 * Transformation rules:
 * - Primitive types (String, Number, etc.) → corresponding TypeScript primitives
 * - BaseType<U> → U (extracts the AST node type)
 * - () => BaseType<U> → U (resolves function to AST node type)
 * - Ref<BaseType<U>> → Reference<U> (creates Langium reference)
 * - Union<T> → T (string literal union)
 * - Resolve<BaseType<U>> → U (resolves to actual type)
 *
 * @template T The simple interface declaration value to map
 */
export type MapPrimitive<T extends SimpleInterfaceDeclarationValue> = T extends typeof BigInt
    ? bigint
    : T extends typeof Number
      ? number
      : T extends typeof String
        ? string
        : T extends typeof Boolean
          ? boolean
          : T extends typeof Date
            ? Date
            : T extends BaseType<infer U>
              ? U
              : T extends () => BaseType<infer U>
                ? U
                : T extends RefType<BaseType<infer U>>
                  ? Reference<U>
                  : T extends RefType<() => BaseType<infer U>>
                    ? Reference<U>
                    : T extends UnionType<infer U>
                      ? U
                      : T extends Resolve<BaseType<infer U>>
                        ? U
                        : T extends Resolve<() => BaseType<infer U>>
                          ? U
                          : never;

/**
 * Maps interface declaration values to their TypeScript equivalents.
 * Handles arrays and optional types in addition to simple values.
 *
 * @template T The interface declaration value to map
 */
type MapInterfaceDeclarationValue<T extends InterfaceDeclarationValue> = T extends SimpleInterfaceDeclarationValue
    ? MapPrimitive<T>
    : T extends [infer U extends SimpleInterfaceDeclarationValue]
      ? MapPrimitive<U>[]
      : T extends OptionalType<infer U>
        ? MapPrimitive<U> | undefined
        : never;

/**
 * Maps a complete interface declaration to its TypeScript object type.
 * Transforms all attributes according to their declaration types.
 *
 * @template T The interface declaration to map
 */
type MapInterfaceDeclaration<T extends InterfaceDeclaration> = {
    [K in keyof T]: MapInterfaceDeclarationValue<T[K]>;
};

/**
 * The final return type for created interfaces. Combines the mapped interface
 * declaration with any inherited interfaces and ensures it extends AstNode.
 *
 * This type represents the complete TypeScript type that will be available
 * when using the interface in grammar rules and AST manipulation.
 *
 * @template T The interface attribute declaration
 * @template E Array of interfaces this interface extends
 *
 * @example
 * ```typescript
 * const PersonInterface = createInterface("Person").attrs({
 *     name: String,
 *     age: Optional(Number),
 *     children: [Ref(() => PersonInterface)]
 * });
 *
 * // CreateInterfaceReturnType results in:
 * // Interface<{
 * //     name: string;
 * //     age: number | undefined;
 * //     children: Reference<PersonType>[];
 * // } & AstNode>
 * ```
 */
export type CreateInterfaceReturnType<T extends InterfaceDeclaration, E extends Interface<any>[]> = Interface<
    Prettify<MapInterfaceDeclaration<T> & Omit<MergeInterface<E>, keyof AstNode>> & AstNode
>;

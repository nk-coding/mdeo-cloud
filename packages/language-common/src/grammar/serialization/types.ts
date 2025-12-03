import type { AstNode, Reference } from "langium";

/**
 * Transforms a Langium AST node type into an easily serializable form by removing
 * circular references and converting Reference types to function references.
 * 
 * This type mapping is essential for generating grammar definitions that can
 * be serialized and processed by the Langium framework. It handles the complex
 * transformation of AST node structures while preserving type safety.
 * 
 * Key transformations:
 * - Removes `$container` properties to break circular references
 * - Converts `Reference<T>` to `() => SerializableGrammarNode<T>` 
 * - Converts `Reference<T>[]` to `(() => SerializableGrammarNode<T>)[]`
 * - Recursively processes nested AST nodes
 * - Preserves arrays and primitive types unchanged
 * 
 * To serialize this, these changes are later reversed.
 * 
 * @template T The AST node type to make serializable
 */
export type SerializableGrammarNode<T extends AstNode> = Omit<
    {
        [K in keyof T]: K extends "$container"
            ? never
            : MapLangiumAstValue<T[K]>;
    },
    "$container"
>;

/**
 * Internal helper type that recursively maps Langium AST values to their
 * serializable equivalents. This type handles the complex transformation
 * logic for different value types.
 * 
 * Transformation rules:
 * - `Reference<U>` → `() => MapLangiumAstValue<U>`
 * - `Reference<U>[]` → `(() => MapLangiumAstValue<U>)[]`
 * - `AstNode` → `SerializableGrammarNode<AstNode>`
 * - `AstNode[]` → `SerializableGrammarNode<AstNode>[]`
 * - Other types remain unchanged
 * 
 * @template T The value type to transform
 */
type MapLangiumAstValue<T> = T extends Reference<infer U>
    ? () => MapLangiumAstValue<U>
    : T extends Reference<infer U>[]
    ? (() => MapLangiumAstValue<U>)[]
    : T extends AstNode
    ? SerializableGrammarNode<T>
    : T extends (infer U)[]
    ? U extends AstNode
        ? SerializableGrammarNode<U>[]
        : T
    : T;

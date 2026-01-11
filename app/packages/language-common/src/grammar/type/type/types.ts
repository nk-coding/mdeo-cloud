import type { AstNode, GrammarAST } from "langium";
import type { SerializableGrammarNode } from "../../serialization/types.js";
import type { BaseType } from "../types.js";

/**
 * Represents a union type definition in the grammar that can contain one of several
 * alternative types. Types are used to define language constructs that can be
 * one of multiple different forms.
 *
 * @template T The AST node union type that this type represents
 */
export type Type<T extends AstNode> = {
    /**
     * TypeScript type information for the union AST node this type represents.
     * Only used for type inference and validation during compilation,
     * never assigned at runtime.
     */
    tsType?: T;
} & SerializableGrammarNode<GrammarAST.Type>;

/**
 * Internal helper type that computes the union of types from an array of base types.
 * This recursive type extracts the AST node types from each base type and combines
 * them into a single union type.
 *
 * @template T Array of base types to union together
 */
export type UnionTypes<T extends BaseType<any>[]> = T extends [BaseType<infer First>, ...infer Rest]
    ? First | UnionTypes<Rest extends BaseType<any>[] ? Rest : []>
    : never;

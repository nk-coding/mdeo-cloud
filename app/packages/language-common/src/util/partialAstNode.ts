import type { AstNode } from "langium";

/**
 * A utility type that makes all properties of an AST node optional,
 * except for the `$type` property which remains required.
 */
export type PartialAstNode<T extends AstNode> = Pick<T, "$type"> & Partial<Omit<T, "$type">>;

import type { CstNode, AstNode, LangiumDocument, LeafCstNode } from "langium";
import type { AstPath, Doc, doc, ParserOptions } from "prettier";
import type { TerminalRule } from "../../grammar/rule/terminal/types.js";
import type { IToken } from "chevrotain";

/**
 * Function type for printing a node at a given path to a Prettier Doc.
 */

export type Print = (path: AstPath) => Doc;
/**
 * Type alias for Prettier's document builders
 */

export type Builders = typeof doc.builders;
/**
 * Represents a primitive value along with its optional CST node.
 *
 * @template T The type of the primitive value
 */
export interface PrimitiveValue<T> {
    /**
     * The actual primitive value
     */
    value: T;
    /**
     * Optional CST node associated with this value
     */
    cstNode?: CstNode;
}

/**
 * Type that extends an AST node to include optional comments.
 *
 * @template T The type of the AST node
 */
export type WithComments<T extends AstNode> = T & { comments?: Comment[] };

/**
 * Context passed to all printer functions.
 *
 * Provides utilities for navigating the AST, accessing primitives, and formatting nested nodes.
 *
 * @template T The type of the root context node
 */
export interface PrintContext<T extends AstNode = AstNode> {
    /**
     * The current AST node being printed
     */
    ctx: WithComments<T>;
    /**
     * he path to the current node in the AST
     */
    path: AstPath<WithComments<T>>;
    /**
     * Prettier parser options for formatting
     */
    options: ParserOptions<T>;
    /**
     * The Langium document containing the AST being printed
     */
    document: LangiumDocument;
    /**
     * Function to recursively print child nodes
     */
    print: Print;
    /**
     * Prints a primitive value using its registered serializer.
     *
     * @template T The type of the primitive value
     * @param value The primitive value to print
     * @param rule The terminal rule associated with this primitive
     * @returns Prettier Doc representation of the primitive
     */
    printPrimitive<T>(value: PrimitiveValue<T>, rule: TerminalRule<T>): Doc;
    /**
     * Retrieves a primitive property value along with its CST node.
     *
     * @template T The AST node type
     * @template V The property key type
     * @param node The AST node to get the property from
     * @param property The name of the property to retrieve
     * @returns The primitive value and its associated CST node
     */
    getPrimitive<T extends AstNode, V extends keyof T & string>(node: T, property: V): PrimitiveValue<T[V]>;
}

/**
 * Prettier comment type
 */
export interface Comment {
    /**
     * The actual comment node
     */
    node: LeafCstNode;
    /**
     * The text of the comment
     */
    value: string;
    leading?: boolean;
    trailing?: boolean;
    printed?: boolean;
}

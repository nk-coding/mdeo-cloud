import type { AstNode, CstNode, LangiumDocument, LeafCstNode, Reference } from "langium";
import type { FormattingOptions } from "vscode-languageserver-types";
import type { TerminalRule } from "../grammar/rule/terminal/types.js";
import type { Interface } from "../grammar/type/interface/types.js";
import type { AstPath, Doc, ParserOptions, doc } from "prettier";

/**
 * Service for serializing AST nodes to formatted text using Prettier.
 *
 * This service allows registering custom serializers for different node types and terminal rules,
 * then uses Prettier's formatting engine to produce consistently formatted output.
 */
export interface AstSerializer {
    /**
     * Serializes an AST node to a formatted string.
     *
     * @param node The AST node to serialize
     * @param document The Langium document containing the AST node
     * @param options Formatting options to guide the serialization
     * @returns A promise that resolves to the formatted string representation of the node
     */
    serializeNode(node: AstNode, document: LangiumDocument, options: FormattingOptions): Promise<string>;

    /**
     * Serializes a primitive value using the registered serializer for the given terminal rule.
     *
     * @template T The TypeScript type of the primitive value
     * @param primitive the primitive value to serialize
     * @param rule the terminal rule that matches this primitive type
     * @returns the string representation of the primitive value
     */
    serializePrimitive<T>(primitive: PrimitiveValue<T>, rule: TerminalRule<T>): string;

    /**
     * Registers a custom printer function for a specific AST node type.
     *
     * @template T The type of AST node this printer handles
     * @param type The interface defining the node type to print
     * @param printer The function that converts nodes of this type to Prettier Doc format
     */
    registerNodeSerializer<T extends AstNode>(type: Interface<T>, printer: (context: PrintContext<T>) => Doc): void;

    /**
     * Registers a custom serializer function for primitive values matched by a terminal rule.
     *
     * @template T The TypeScript type of the primitive value
     * @param rule The terminal rule that matches this primitive type
     * @param serializer The function that converts primitive values to a string
     */
    registerPrimitiveSerializer<T>(rule: TerminalRule<T>, serializer: (primitive: PrimitiveValue<T>) => string): void;
}

/**
 * Additional services that can be injected into Langium services.
 */
export interface AstSerializerAdditionalServices {
    AstSerializer: AstSerializer;
}

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
    /**
     * Prints a reference value using the registered serializer for the terminal
     *
     * @template T The AstNode type being referenced
     * @param value The reference value to print
     * @param rule The terminal rule associated with this reference
     * @returns Prettier Doc representation of the reference
     */
    printReference<T extends AstNode>(value: AstPath<Reference<T>>, rule: TerminalRule<string>): Doc;
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

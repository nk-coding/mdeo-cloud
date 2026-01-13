import type { AstNode, AstReflection, CstNode, LangiumCoreServices, LangiumDocument, Reference } from "langium";
import type {
    AstSerializer,
    Comment,
    PrimitiveValue,
    PrintContext,
    TerminalRule,
    WithComments
} from "@mdeo/language-common";
import type { Interface } from "@mdeo/language-common";
import type { AstPath, Doc, Plugin } from "prettier";
import type { FormattingOptions } from "vscode-languageserver-types";
import { printComment } from "./comments.js";
import { ML_COMMENT, SL_COMMENT } from "@mdeo/language-common";
import { sharedImport } from "../sharedImport.js";

const { doc: prettierDoc, format: prettierFormat } = sharedImport("prettier");
const { GrammarUtils, isAstNode, isCompositeCstNode, isLeafCstNode } = sharedImport("langium");

/**
 * Default AST serializer service using Prettier.
 *
 * Creates a Prettier-based serializer that uses registered node and primitive serializers
 * to format AST nodes consistently. The serializer integrates with Prettier's plugin system
 * to provide custom parsing and printing logic.
 */
export class DefaultAstSerializer implements AstSerializer {
    /**
     * Map of AST node type names to their registered printer functions.
     */
    private readonly nodePrinters = new Map<string, (context: PrintContext) => Doc>();
    /**
     * Map of terminal rule names to their registered primitive serializer functions.
     */
    private readonly primitiveSerializers = new Map<string, (primitive: any) => string>();
    /**
     * Reflection utilities for AST nodes.
     */
    private readonly astReflection: AstReflection;

    constructor(protected readonly services: LangiumCoreServices) {
        this.astReflection = services.shared.AstReflection;
    }

    /**
     * Helper to get the primitive value and its CST node for a given property of an AST node.
     *
     * @param node the AST node
     * @param property the property name
     * @returns the primitive value and its CST node
     */
    private getPrimitive<T extends AstNode, V extends keyof T & string>(node: T, property: V): PrimitiveValue<T[V]> {
        const value = node[property];
        const cstNode = GrammarUtils.findNodeForProperty(node.$cstNode, property);
        return { value, cstNode };
    }

    /**
     * Helper class for generating Prettier plugins with proper offset normalization.
     * Handles the conversion between original text offsets and Prettier's normalized text.
     */
    private createPrettierPluginGenerator(text: string) {
        class PrettierPluginGenerator {
            private readonly crPositions: number[];

            constructor(text: string) {
                this.crPositions = this.buildCrPositionsLookup(text);
            }

            /**
             * Builds a lookup of all \r positions (that are followed by \n).
             * Prettier replaces \r\n with \n, so we need to adjust offsets accordingly.
             */
            private buildCrPositionsLookup(text: string): number[] {
                const positions: number[] = [];
                for (let i = 0; i < text.length - 1; i++) {
                    if (text[i] === "\r" && text[i + 1] === "\n") {
                        positions.push(i);
                    }
                }
                return positions;
            }

            /**
             * Normalizes an offset from original text to Prettier's normalized text.
             * Uses binary search to find how many \r characters were removed before this offset.
             */
            private normalizeOffset(offset: number): number {
                let left = 0;
                let right = this.crPositions.length;
                while (left < right) {
                    const mid = Math.floor((left + right) / 2);
                    if (this.crPositions[mid] < offset) {
                        left = mid + 1;
                    } else {
                        right = mid;
                    }
                }
                return offset - left;
            }

            /**
             * Generates a Prettier plugin for serializing AST nodes.
             */
            generate(
                node: AstNode,
                document: LangiumDocument,
                nodePrinters: Map<string, (context: PrintContext) => Doc>,
                primitiveSerializers: Map<string, (primitive: any) => Doc>,
                astReflection: AstReflection,
                getPrimitive: <T extends AstNode, V extends keyof T & string>(
                    node: T,
                    property: V
                ) => PrimitiveValue<T[V]>
            ): Plugin<AstNode> {
                return {
                    parsers: {
                        custom: {
                            parse: () => {
                                const copy: WithComments<AstNode> = this.copyAstNode(node);
                                copy.comments = this.getComments(node);
                                return copy;
                            },
                            astFormat: "custom",
                            locStart: (node: AstNode | Comment) => {
                                const offset = isAstNode(node) ? (node.$cstNode?.offset ?? 0) : (node.node.offset ?? 0);
                                return this.normalizeOffset(offset);
                            },
                            locEnd: (node: AstNode | Comment) => {
                                const offset = isAstNode(node) ? (node.$cstNode?.end ?? 0) : (node.node.end ?? 0);
                                return this.normalizeOffset(offset);
                            }
                        }
                    },
                    printers: {
                        custom: {
                            print: (path, options, print) => {
                                const node = path.node;
                                const printer = nodePrinters.get(node.$type);
                                if (printer == undefined) {
                                    throw new Error(`No printer registered for node type: ${node.$type}`);
                                }
                                const context: PrintContext = {
                                    ctx: path.node,
                                    path,
                                    options,
                                    document,
                                    print,
                                    printPrimitive: <T>(value: PrimitiveValue<T>, rule: TerminalRule<T>): Doc => {
                                        const serializer = primitiveSerializers.get(rule.name);
                                        if (serializer == undefined) {
                                            throw new Error(
                                                `No primitive serializer registered for rule: ${rule.name}`
                                            );
                                        }
                                        return serializer(value);
                                    },
                                    getPrimitive,
                                    printReference: <T extends AstNode>(
                                        value: AstPath<Reference<T>>,
                                        rule: TerminalRule<string>
                                    ): Doc => {
                                        const serializer = primitiveSerializers.get(rule.name);
                                        if (serializer == undefined) {
                                            throw new Error(
                                                `No primitive serializer registered for rule: ${rule.name}`
                                            );
                                        }
                                        return serializer({
                                            value: value.node.$refText,
                                            cstNode: value.node.$refNode
                                        } satisfies PrimitiveValue<string>);
                                    }
                                };
                                return printer(context);
                            },
                            printComment: (commentPath, options) => {
                                return printComment(
                                    commentPath.node as unknown as Comment,
                                    options,
                                    prettierDoc.builders
                                );
                            },
                            canAttachComment: (node) => {
                                return isAstNode(node);
                            },
                            isBlockComment: (comment) => {
                                return (comment as unknown as Comment).node.tokenType.name === ML_COMMENT.name;
                            },
                            getVisitorKeys: (node) => {
                                return Object.keys(astReflection.getTypeMetaData(node.$type).properties);
                            }
                        }
                    }
                };
            }

            /**
             * Copies an AST node recursively.
             * Does NOT copy references
             */
            private copyAstNode(node: AstNode): AstNode {
                const copy: Record<string, any> = {};
                for (const [key, value] of Object.entries(node)) {
                    if (key.startsWith("$")) {
                        copy[key] = value;
                    } else if (Array.isArray(value)) {
                        copy[key] = value.map((item) => {
                            if (isAstNode(item)) {
                                return this.copyAstNode(item);
                            } else {
                                return item;
                            }
                        });
                    } else if (isAstNode(value)) {
                        copy[key] = this.copyAstNode(value);
                    } else {
                        copy[key] = value;
                    }
                }
                return copy as AstNode;
            }

            /**
             * Gets all comments associated with an AST node.
             */
            private getComments(node: AstNode): Comment[] {
                const comments: Comment[] = [];
                const cstNode = node.$cstNode;
                if (cstNode != undefined) {
                    this.getCommentsRecursive(cstNode, comments);
                }
                return comments;
            }

            /**
             * Recursively collects comments from a CST node.
             */
            private getCommentsRecursive(node: CstNode, comments: Comment[]): void {
                if (isCompositeCstNode(node)) {
                    for (const child of node.content) {
                        this.getCommentsRecursive(child, comments);
                    }
                } else if (isLeafCstNode(node)) {
                    if (node.tokenType.name === ML_COMMENT.name || node.tokenType.name === SL_COMMENT.name) {
                        comments.push({
                            node,
                            value: node.text
                        });
                    }
                }
            }
        }

        return new PrettierPluginGenerator(text);
    }

    /**
     * Serializes an AST node to a formatted string.
     *
     * @param node the AST node to serialize
     * @param document the Langium document containing the AST node
     * @param options formatting options
     * @returns a promise that resolves to the formatted string representation of the node
     */
    async serializeNode(node: AstNode, document: LangiumDocument, options: FormattingOptions): Promise<string> {
        const text = document.textDocument.getText();
        const pluginGenerator = this.createPrettierPluginGenerator(text);
        const plugin = pluginGenerator.generate(
            node,
            document,
            this.nodePrinters,
            this.primitiveSerializers,
            this.astReflection,
            this.getPrimitive.bind(this)
        );
        return prettierFormat(text, {
            parser: "custom",
            plugins: [plugin],
            useTabs: !options.insertSpaces,
            tabWidth: options.tabSize
        });
    }

    /**
     * Serializes a primitive value using the registered serializer for the given terminal rule.
     *
     * @template T The TypeScript type of the primitive value
     * @param primitive the primitive value to serialize
     * @param rule the terminal rule that matches this primitive type
     * @returns the string representation of the primitive value
     */
    serializePrimitive<T>(primitive: PrimitiveValue<T>, rule: TerminalRule<T>): string {
        const serializer = this.primitiveSerializers.get(rule.name);
        if (serializer == undefined) {
            throw new Error(`No primitive serializer registered for rule: ${rule.name}`);
        }
        return serializer(primitive);
    }

    /**
     * Registers a custom printer function for a specific AST node type.
     *
     * @template T The type of AST node this printer handles
     * @param type The interface defining the node type to print
     * @param printer The function that converts nodes of this type to Prettier Doc format
     */
    registerNodeSerializer<T extends AstNode>(type: Interface<T>, printer: (context: PrintContext<T>) => Doc): void {
        this.nodePrinters.set(type.name, printer as unknown as (context: PrintContext) => Doc);
    }

    /**
     * Registers a custom serializer function for primitive values matched by a terminal rule.
     *
     * @template T The TypeScript type of the primitive value
     * @param rule The terminal rule that matches this primitive type
     * @param serializer The function that converts primitive values to a string
     */
    registerPrimitiveSerializer<T>(rule: TerminalRule<T>, serializer: (primitive: PrimitiveValue<T>) => string): void {
        this.primitiveSerializers.set(rule.name, serializer as (primitive: PrimitiveValue<any>) => string);
    }
}

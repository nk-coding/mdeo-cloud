import type { AstNode, CstNode, LangiumCoreServices, LangiumDocument } from "langium";
import type { TerminalRule } from "../../grammar/rule/terminal/types.js";
import type { Interface } from "../../grammar/type/interface/types.js";
import type { AstPath, doc, Doc, Parser, ParserOptions, Plugin } from "prettier";
import type { PluginContext } from "../../plugin/pluginContext.js";
import type { FormattingOptions } from "vscode-languageserver-types";

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
     * @param serializer The function that converts primitive values to Prettier Doc format
     */
    registerPrimitiveSerializer<T>(rule: TerminalRule<T>, serializer: (primitive: PrimitiveValue<T>) => Doc): void;
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
interface PrimitiveValue<T> {
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
    ctx: T;
    /**
     * he path to the current node in the AST
     */
    path: AstPath<T>;
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
 * Generates the default AST serializer service provider.
 *
 * Creates a Prettier-based serializer that uses registered node and primitive serializers
 * to format AST nodes consistently. The serializer integrates with Prettier's plugin system
 * to provide custom parsing and printing logic.
 *
 * @param context The plugin context containing Langium and Prettier dependencies
 * @returns An object containing the AstSerializer service provider
 */
export function generateDefaultAstSerializer(context: PluginContext): {
    AstSerializer: (services: LangiumCoreServices) => AstSerializer;
} {
    const { prettier, langium } = context;

    function getPrimitive<T extends AstNode, V extends keyof T & string>(node: T, property: V): PrimitiveValue<T[V]> {
        const value = node[property];
        const cstNode = langium.GrammarUtils.findNodeForProperty(node.$cstNode, property);
        return { value, cstNode };
    }

    class PrettierAstSerializer implements AstSerializer {
        private readonly nodePrinters = new Map<string, (context: PrintContext) => Doc>();
        private readonly primitiveSerializers = new Map<string, (primitive: any) => Doc>();

        constructor(protected readonly services: LangiumCoreServices) {}

        async serializeNode(node: AstNode, document: LangiumDocument, options: FormattingOptions): Promise<string> {
            const plugin = this.generatePlugin(node, document);
            return prettier.format("unused", {
                parser: "custom",
                plugins: [plugin],
                useTabs: !options.insertSpaces,
                tabWidth: options.tabSize
            });
        }

        registerNodeSerializer<T extends AstNode>(
            type: Interface<T>,
            printer: (context: PrintContext<T>) => Doc
        ): void {
            this.nodePrinters.set(type.name, printer as unknown as (context: PrintContext) => Doc);
        }

        registerPrimitiveSerializer<T>(rule: TerminalRule<T>, serializer: (primitive: PrimitiveValue<T>) => Doc): void {
            this.primitiveSerializers.set(rule.name, serializer as (primitive: PrimitiveValue<any>) => Doc);
        }

        private printPrimitive<T>(value: PrimitiveValue<T>, rule: TerminalRule<T>): Doc {
            const serializer = this.primitiveSerializers.get(rule.name);
            if (serializer == undefined) {
                throw new Error(`No primitive serializer registered for rule: ${rule.name}`);
            }
            return serializer(value);
        }

        private generatePlugin(node: AstNode, document: LangiumDocument): Plugin<AstNode> {
            return {
                parsers: {
                    custom: {
                        parse: () => {
                            return this.copyAstNode(node);
                        },
                        astFormat: "custom",
                        locStart: (node: AstNode) => {
                            return (node.$cstNode?.offset ?? -1) + 1;
                        },
                        locEnd: (node: AstNode) => {
                            return (node.$cstNode?.end ?? -1) + 1;
                        }
                    }
                },
                printers: {
                    custom: {
                        print: (path, options, print) => {
                            const node = path.node;
                            const printer = this.nodePrinters.get(node.$type);
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
                                    return this.printPrimitive(value, rule);
                                },
                                getPrimitive
                            };
                            return printer(context);
                        }
                    }
                }
            };
        }

        /**
         * Copies an AST node recursively.
         * Does NOT copy references
         *
         * @param node The AST node to copy
         * @returns The copied AST node
         */
        private copyAstNode(node: AstNode): AstNode {
            const copy: Record<string, any> = {};
            for (const [key, value] of Object.entries(node)) {
                if (key.startsWith("$")) {
                    copy[key] = value;
                } else if (Array.isArray(value)) {
                    copy[key] = value.map((item) => {
                        if (langium.isAstNode(item)) {
                            return this.copyAstNode(item);
                        } else {
                            return item;
                        }
                    });
                } else if (langium.isAstNode(value)) {
                    copy[key] = this.copyAstNode(value);
                } else {
                    copy[key] = value;
                }
            }
            return copy as AstNode;
        }
    }

    return {
        AstSerializer: (services: LangiumCoreServices) => new PrettierAstSerializer(services)
    };
}

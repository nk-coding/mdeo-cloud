import { URI } from "vscode-uri";
import type { LangiumDocumentFactory, LangiumSharedCoreServices } from "langium";
import type {
    AstNode,
    GenericAstNode,
    MultiReference,
    MultiReferenceItem,
    Mutable,
    Reference,
    LangiumDocument,
    LangiumDocuments
} from "langium";
import { DefaultAstNodeLocator, isAstNode, isMultiReference, isReference } from "langium";
import type { ServiceAdditionalSharedServices } from "./types.js";
import type { ExtendedIndexManager } from "./extendedIndexManager.js";

/**
 * Data structure representing the serialized AST along with metadata.
 */
export interface AstData {
    /**
     * Indicates whether the AST contains lexing, parsing, or validation errors.
     */
    hasErrors: boolean;
    /**
     * Indicates whether the AST or any of its dependencies have errors.
     */
    hasTransitiveErrors: boolean;
    /**
     * The serialized AST representation.
     */
    ast: SerializedAst;
    /**
     * List of link dependencies (paths) to other documents needed for reference resolution.
     */
    linkDependencies: string[];
    /**
     * The path of the document from which this AST was serialized.
     */
    path: string;
    /**
     * List of exported elements from the AST node.
     */
    exportedElements: SerializedAstNodeDescription[];
}

/**
 * Serialized representation of an AST node.
 */
export interface SerializedAst extends Pick<AstNode, "$type"> {
    [key: string]: SerializedAstEntry | SerializedAstEntry[];
}

/**
 * An entry in the serialized AST representation.
 */
export type SerializedAstEntry =
    | string
    | number
    | boolean
    | null
    | SerializedAst
    | SerializedReference
    | SerializedMultiReference;

/**
 * Base interface for serialized references.
 */
interface SerializedReferenceBase {
    /**
     * The original reference text from the source.
     */
    $refText: string;
    /**
     * Error message if the reference could not be resolved.
     */
    $error: string | undefined;
}

/**
 * Serialized single-reference
 */
export interface SerializedReference extends SerializedReferenceBase {
    /**
     * The reference URI in the format 'uri#path' or '#path' for local references.
     * Undefined if the reference could not be resolved.
     */
    $ref: string | undefined;
}

/**
 * Serialized reference for multi-references.
 */
export interface SerializedMultiReference extends SerializedReferenceBase {
    /**
     * The array of reference URIs.
     */
    $refs: string[];
}

/**
 * Serialized AST node description
 */
export interface SerializedAstNodeDescription {
    /**
     * The name of the exported element
     */
    name: string;
    /**
     * The reference path to obtain the AST node in the serialized AST
     */
    path: string;
}

/**
 * Serializer for converting AST nodes to JavaScript objects and deserializing them back.
 * Handles references between nodes, including cross-document references.
 */
export class JsonAstSerializer {
    /**
     * Properties to ignore during serialization.
     */
    protected ignoreProperties = new Set([
        "$container",
        "$containerProperty",
        "$containerIndex",
        "$document",
        "$cstNode"
    ]);

    /**
     * LangiumDocuments service for document management.
     */
    protected readonly langiumDocuments: LangiumDocuments;

    /**
     * LangiumDocumentFactory for creating documents.
     */
    protected readonly langiumDocumentFactory: LangiumDocumentFactory;

    /**
     * IndexManager for managing AST node indices.
     */
    protected readonly indexManager: ExtendedIndexManager;

    /**
     * AstNodeLocator for locating AST nodes
     */
    protected readonly astNodeLocator = new DefaultAstNodeLocator();

    /**
     * Creates a new JsonAstSerializer.
     *
     * @param services The Langium shared core services used for document management.
     */
    constructor(services: LangiumSharedCoreServices & ServiceAdditionalSharedServices) {
        this.langiumDocuments = services.workspace.LangiumDocuments;
        this.langiumDocumentFactory = services.workspace.LangiumDocumentFactory;
        this.indexManager = services.workspace.IndexManager;
    }

    /**
     * Serializes an AST node into a JavaScript object representation.
     *
     * @param document The Langium document containing the AST to serialize.
     * @returns The serialized representation of the AST node.
     */
    serialize(document: LangiumDocument): SerializedAst {
        return this.serializeNode(document.parseResult.value, document);
    }

    /**
     * Serializes the exported elements of a document's AST.
     *
     * @param document The Langium document whose exported elements are to be serialized.
     * @returns An array of serialized AST node descriptions for the exported elements.
     */
    serializeExportedElements(document: LangiumDocument): SerializedAstNodeDescription[] {
        const allElements = this.indexManager.allElements(undefined, new Set([document.uri.toString()]));
        return allElements
            .filter((description) => description.node != undefined)
            .map((description) => ({
                name: description.name,
                path: this.astNodeLocator.getAstNodePath(description.node!)
            }))
            .toArray();
    }

    /**
     * Deserializes multiple documents from their serialized representations.
     * Creates LangiumDocument instances for each document and registers them with the workspace.
     *
     * @param documents Array of documents with their serialized content and URIs.
     * @returns Array of deserialized root AST nodes.
     */
    deserializeDocuments(documents: AstData[]): AstNode[] {
        const roots: AstNode[] = [];
        const langiumDocs: LangiumDocument[] = [];

        for (const doc of documents) {
            const root = doc.ast as AstNode;
            roots.push(root);
            const uri = URI.file(doc.path);

            const document = this.langiumDocumentFactory.fromModel(root, uri);
            this.langiumDocuments.addDocument(document);

            langiumDocs.push(document);
        }

        for (let i = 0; i < roots.length; i++) {
            this.linkNode(roots[i] as GenericAstNode, roots[i]);
            this.registerExportedElements(documents[i], langiumDocs[i]);
        }

        return roots;
    }

    /**
     * Registers the exported elements of a document in the index manager directly.
     *
     * @param data The AST data containing exported elements
     * @param document The Langium document
     */
    protected registerExportedElements(data: AstData, document: LangiumDocument): void {
        this.indexManager.updateExternalContent(
            document,
            data.exportedElements.map((description) => {
                const node = this.astNodeLocator.getAstNode(document.parseResult.value, description.path);
                if (node == undefined) {
                    throw new Error(`Could not find exported element at path: ${description.path}`);
                }
                return {
                    name: description.name,
                    node,
                    path: description.path,
                    type: node.$type,
                    documentUri: document.uri
                };
            })
        );
    }

    /**
     * Serializes an AST node and all its properties into a JavaScript object.
     *
     * @param node The AST node to serialize.
     * @param currentDocument The current document
     * @returns The serialized representation with all node properties.
     */
    protected serializeNode(node: AstNode, currentDocument: LangiumDocument): SerializedAst {
        const result: SerializedAst = {
            $type: node.$type
        };

        for (const [key, value] of Object.entries(node)) {
            if (this.ignoreProperties.has(key)) {
                continue;
            }

            const serializedValue = this.serializeValue(value, currentDocument);
            if (serializedValue !== undefined) {
                result[key] = serializedValue;
            }
        }

        return result;
    }

    /**
     * Serializes a single value which can be a primitive, array, reference, or AST node.
     *
     * @param value The value to serialize.
     * @param currentDocument The current document
     * @returns The serialized representation of the value, or undefined if it cannot be serialized.
     */
    protected serializeValue(
        value: unknown,
        currentDocument: LangiumDocument
    ): SerializedAstEntry | SerializedAstEntry[] | undefined {
        if (value == undefined) {
            return value;
        } else if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
            return value;
        } else if (Array.isArray(value)) {
            return value
                .map((item) => this.serializeValue(item, currentDocument))
                .filter((item) => item !== undefined) as SerializedAstEntry[];
        } else if (isReference(value)) {
            return this.serializeReference(value, currentDocument);
        } else if (isMultiReference(value)) {
            return this.serializeMultiReference(value, currentDocument);
        } else if (isAstNode(value)) {
            return this.serializeNode(value, currentDocument);
        } else {
            return undefined;
        }
    }

    /**
     * Serializes a single Reference.
     *
     * @param value The Reference to serialize.
     * @param currentDocument The current document
     * @returns The serialized representation of the Reference.
     */
    protected serializeReference(value: Reference, currentDocument: LangiumDocument): SerializedReference {
        const refValue = value.ref;
        const $refText = value.$refText;
        if (refValue) {
            const targetDocument = this.getDocumentForNode(refValue);
            let targetUri = "";
            if (currentDocument !== targetDocument) {
                targetUri = targetDocument.uri.toString();
            }
            const targetPath = this.astNodeLocator.getAstNodePath(refValue);
            return {
                $ref: `${targetUri}#${targetPath}`,
                $refText,
                $error: undefined
            } satisfies SerializedReference;
        } else {
            return {
                $ref: "",
                $refText,
                $error: value.error?.message ?? "Could not resolve reference"
            } satisfies SerializedReference;
        }
    }

    /**
     * Serializes a MultiReference.
     *
     * @param value The MultiReference to serialize.
     * @param currentDocument The current document
     * @returns The serialized representation of the MultiReference.
     */
    protected serializeMultiReference(
        value: MultiReference,
        currentDocument: LangiumDocument
    ): SerializedMultiReference {
        const $refText = value.$refText;
        const $refs: string[] = [];
        for (const item of value.items) {
            const refValue = item.ref;
            if (refValue == undefined) {
                continue;
            }
            const targetDocument = this.getDocumentForNode(refValue);
            let targetUri = "";
            if (currentDocument !== targetDocument) {
                targetUri = targetDocument.uri.toString();
            }
            const targetPath = this.astNodeLocator.getAstNodePath(refValue);
            $refs.push(`${targetUri}#${targetPath}`);
        }
        return {
            $refs,
            $refText,
            $error: undefined
        } satisfies SerializedMultiReference;
    }

    /**
     * Links a deserialized node by restoring references and setting container properties.
     * Recursively processes all child nodes and references.
     * @param node The node to link.
     * @param root The root node of the document.
     * @param container The parent container of this node.
     * @param containerProperty The property name in the container that holds this node.
     * @param containerIndex The index in the container array if applicable.
     */
    protected linkNode(
        node: GenericAstNode,
        root: AstNode,
        container?: AstNode,
        containerProperty?: string,
        containerIndex?: number
    ): void {
        for (const [propertyName, item] of Object.entries(node)) {
            if (Array.isArray(item)) {
                for (let index = 0; index < item.length; index++) {
                    const element = item[index];
                    if (this.isSerializedReference(element) || this.isSerializedMultiReference(element)) {
                        item[index] = this.reviveReference(node, propertyName, root, element);
                    } else if (isAstNode(element)) {
                        this.linkNode(element as GenericAstNode, root, node, propertyName, index);
                    }
                }
            } else if (this.isSerializedReference(item) || this.isSerializedMultiReference(item)) {
                node[propertyName] = this.reviveReference(node, propertyName, root, item);
            } else if (isAstNode(item)) {
                this.linkNode(item as GenericAstNode, root, node, propertyName);
            }
        }
        const mutable = node as Mutable<AstNode>;
        mutable.$container = container;
        mutable.$containerProperty = containerProperty;
        mutable.$containerIndex = containerIndex;
    }

    /**
     * Revives a serialized reference or multi-reference by resolving the target nodes.
     * @param container The container node holding the reference.
     * @param property The property name of the reference.
     * @param root The root node of the document.
     * @param reference The serialized reference to revive.
     * @returns The revived reference object, or undefined if resolution fails.
     */
    protected reviveReference(
        container: AstNode,
        property: string,
        root: AstNode,
        reference: SerializedReference | SerializedMultiReference
    ): Reference | MultiReference | undefined {
        const refText = reference.$refText;
        let error = reference.$error;
        let ref: Mutable<Reference> | Mutable<MultiReference> | undefined;

        if (reference.$error != undefined) {
            error = reference.$error;
        } else if (this.isSerializedReference(reference)) {
            const refNode = this.getRefNode(root, reference.$ref!);
            if (isAstNode(refNode)) {
                return {
                    $refText: refText ?? "",
                    ref: refNode
                };
            } else {
                error = refNode;
            }
        } else if (this.isSerializedMultiReference(reference)) {
            const refs: MultiReferenceItem[] = [];
            for (const refUri of reference.$refs) {
                const refNode = this.getRefNode(root, refUri);
                if (isAstNode(refNode)) {
                    refs.push({ ref: refNode });
                }
            }
            if (refs.length === 0) {
                ref = {
                    $refText: refText ?? "",
                    items: refs
                };
                error ??= "Could not resolve multi-reference";
            } else {
                return {
                    $refText: refText ?? "",
                    items: refs
                };
            }
        }

        if (error) {
            ref ??= {
                $refText: refText ?? "",
                ref: undefined
            };
            ref.error = {
                info: {
                    container,
                    property,
                    reference: ref
                },
                message: error
            };
            return ref;
        } else {
            return undefined;
        }
    }

    /**
     * Resolves a reference URI to its target AST node.
     * Handles both local references (fragment-only) and cross-document references.
     * @param root The root node of the current document.
     * @param uri The reference URI in the format 'uri#path' or '#path' for local references.
     * @returns The resolved AST node, or an error message string if resolution fails.
     */
    protected getRefNode(root: AstNode, uri: string): AstNode | string {
        try {
            const fragmentIndex = uri.indexOf("#");
            if (fragmentIndex === 0) {
                const node = this.astNodeLocator.getAstNode(root, uri.substring(1));
                if (node == undefined) {
                    return "Could not resolve path: " + uri;
                }
                return node;
            }
            if (fragmentIndex < 0) {
                const documentUri = URI.parse(uri);
                const document = this.langiumDocuments.getDocument(documentUri);
                if (document == undefined) {
                    return "Could not find document for URI: " + uri;
                }
                return document.parseResult.value;
            }
            const documentUri = URI.parse(uri.substring(0, fragmentIndex));
            const document = this.langiumDocuments.getDocument(documentUri);
            if (document == undefined) {
                return "Could not find document for URI: " + uri;
            }
            if (fragmentIndex === uri.length - 1) {
                return document.parseResult.value;
            }
            const node = this.astNodeLocator.getAstNode(document.parseResult.value, uri.substring(fragmentIndex + 1));
            if (node == undefined) {
                return "Could not resolve URI: " + uri;
            }
            return node;
        } catch (err) {
            return String(err);
        }
    }

    /**
     * Type guard to check if an object is a serialized reference.
     * @param obj The object to check.
     * @returns True if the object is a serialized reference.
     */
    protected isSerializedReference(obj: unknown): obj is SerializedReference {
        return typeof obj === "object" && !!obj && "$ref" in obj;
    }

    /**
     * Type guard to check if an object is a serialized multi-reference.
     * @param obj The object to check.
     * @returns True if the object is a serialized multi-reference.
     */
    protected isSerializedMultiReference(obj: unknown): obj is SerializedMultiReference {
        return typeof obj === "object" && !!obj && "$refs" in obj;
    }

    /**
     * Retrieves the LangiumDocument for a given AST node.
     * Traverses up the containment hierarchy to find the root node's document.
     *
     * @param node The AST node to get the document for.
     * @returns The LangiumDocument containing the node.
     * @throws Error if no document can be found.
     */
    protected getDocumentForNode(node: AstNode): LangiumDocument {
        let current = node;
        while (current.$container) {
            current = current.$container;
        }
        if (current.$document) {
            return current.$document;
        }
        throw new Error("Could not find document for AST node");
    }
}

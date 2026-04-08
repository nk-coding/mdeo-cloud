import type { AstNode } from "langium";
import type { ModelIdProvider } from "./modelIdProvider.js";
import type { GraphMetadata, NodeMetadata, EdgeMetadata } from "./metadata.js";
import type { MetadataEdits } from "./handler/operationHandlerCommand.js";
import { DefaultModelIdRegistry } from "./modelIdRegistry.js";

interface InsertionEntry {
    elements: AstNode[];
    index: number | undefined;
}

/**
 * Specifies a set of AstNodes to insert into a specific array property of a container node.
 *
 * @example
 * ```ts
 * const spec: InsertSpecification = {
 *     container: matchStatement.pattern,
 *     property: "elements",
 *     elements: [newPatternInstance, newPatternLink]
 * };
 * ```
 */
export interface InsertSpecification {
    /** The container AstNode that owns the array property. */
    readonly container: AstNode;
    /** The name of the array property on the container where elements will be inserted. */
    readonly property: string;
    /** The AstNode elements to insert into the array property. */
    readonly elements: AstNode[];
    /**
     * The index at which to insert the elements. When omitted the elements are
     * appended at the end of the array.
     */
    readonly index?: number;
}

/**
 * Describes an id that changed because of structural modifications to the model
 * (e.g. index shifts when inserting an element before existing siblings).
 */
export interface IdChange {
    /** The id before the insertion. */
    readonly oldId: string;
    /** The id after the insertion. */
    readonly newId: string;
}

/**
 * Result of computing ids for a model with pending insertions.
 */
export interface InsertIdResult {
    /**
     * Maps each originally provided inserted AstNode to the id it would receive
     * after insertion. The keys are the exact AstNode references from
     * {@link InsertSpecification.elements}.
     */
    readonly insertedIds: ReadonlyMap<AstNode, string>;

    /**
     * List of id changes for pre-existing nodes whose ids shifted as a result
     * of the insertions (e.g. index-based ids that moved).
     */
    readonly idChanges: readonly IdChange[];

    /**
     * Resolves the post-insertion id for any AstNode that participates in the
     * model – either a pre-existing node (whose id may have shifted) or a
     * newly inserted node. Returns `undefined` when the node is unknown.
     */
    resolveId(node: AstNode): string | undefined;
}

/**
 * Computes the ids that newly inserted AstNodes would receive, along with any
 * id changes to pre-existing nodes, by simulating the insertions on a
 * shallow-copied model tree and running the standard {@link DefaultModelIdRegistry}.
 *
 * The original model and the provided AstNodes are not mutated.
 *
 * @param rootNode The root of the current model (before insertions)
 * @param idProvider The id provider used for id generation
 * @param insertions The list of insert specifications describing what to insert where
 * @returns An {@link InsertIdResult} with the new ids and any id changes
 */
export function computeInsertIds(
    rootNode: AstNode,
    idProvider: ModelIdProvider,
    insertions: InsertSpecification[]
): InsertIdResult {
    return new InsertIdComputer(rootNode, idProvider, insertions).compute();
}

class InsertIdComputer {
    private readonly insertionMap = new Map<AstNode, Map<string, InsertionEntry>>();
    private readonly copyToOriginal = new Map<AstNode, AstNode>();
    private readonly originalToCopy = new Map<AstNode, AstNode>();
    private readonly insertedOriginalToCopy = new Map<AstNode, AstNode>();
    private newRegistry!: DefaultModelIdRegistry;
    private oldRegistry!: DefaultModelIdRegistry;

    constructor(
        private readonly rootNode: AstNode,
        private readonly idProvider: ModelIdProvider,
        insertions: InsertSpecification[]
    ) {
        for (const spec of insertions) {
            let propMap = this.insertionMap.get(spec.container);
            if (propMap == undefined) {
                propMap = new Map();
                this.insertionMap.set(spec.container, propMap);
            }
            const existing = propMap.get(spec.property);
            if (existing != undefined) {
                existing.elements.push(...spec.elements);
            } else {
                propMap.set(spec.property, { elements: [...spec.elements], index: spec.index });
            }
        }
    }

    compute(): InsertIdResult {
        const copiedRoot = this.deepCopyNode(this.rootNode, undefined, undefined, undefined);
        this.newRegistry = new DefaultModelIdRegistry(copiedRoot, this.idProvider);
        this.oldRegistry = new DefaultModelIdRegistry(this.rootNode, this.idProvider);

        const insertedIds = new Map<AstNode, string>();
        for (const [original, copy] of this.insertedOriginalToCopy) {
            if (this.newRegistry.hasId(copy)) {
                insertedIds.set(original, this.newRegistry.getId(copy));
            }
        }

        const idChanges: IdChange[] = [];
        for (const [copy, original] of this.copyToOriginal) {
            if (this.insertedOriginalToCopy.has(original)) continue;
            if (!this.oldRegistry.hasId(original) || !this.newRegistry.hasId(copy)) continue;
            const oldId = this.oldRegistry.getId(original);
            const newId = this.newRegistry.getId(copy);
            if (oldId !== newId) {
                idChanges.push({ oldId, newId });
            }
        }

        return { insertedIds, idChanges, resolveId: (node) => this.resolveId(node) };
    }

    private resolveId(node: AstNode): string | undefined {
        const insertedCopy = this.insertedOriginalToCopy.get(node);
        if (insertedCopy != undefined) {
            return this.newRegistry.hasId(insertedCopy) ? this.newRegistry.getId(insertedCopy) : undefined;
        }
        const copy = this.originalToCopy.get(node);
        if (copy != undefined) {
            return this.newRegistry.hasId(copy) ? this.newRegistry.getId(copy) : undefined;
        }
        return undefined;
    }

    private deepCopyNode(
        node: AstNode,
        parent: AstNode | undefined,
        containerProperty: string | undefined,
        containerIndex: number | undefined
    ): AstNode {
        const nodeRecord = node as unknown as Record<string, unknown>;
        const copy: Record<string, unknown> = {};

        for (const key of Object.keys(node)) {
            if (key.startsWith("$")) {
                copy[key] = nodeRecord[key];
                continue;
            }
            const value = nodeRecord[key];

            if (isAstNode(value)) {
                copy[key] = this.deepCopyNode(value, copy as unknown as AstNode, key, undefined);
            } else if (Array.isArray(value)) {
                copy[key] = this.copyArray(node, copy as unknown as AstNode, key, value);
            } else {
                copy[key] = value;
            }
        }

        copy.$container = parent;
        copy.$containerProperty = containerProperty;
        copy.$containerIndex = containerIndex;
        copy.$cstNode = undefined;
        if (parent == undefined) {
            copy.$document = nodeRecord.$document;
        } else {
            copy.$document = undefined;
        }

        const castCopy = copy as unknown as AstNode;
        this.copyToOriginal.set(castCopy, node);
        this.originalToCopy.set(node, castCopy);
        return castCopy;
    }

    private copyArray(sourceNode: AstNode, copyParent: AstNode, key: string, value: unknown[]): unknown[] {
        const newArray: unknown[] = [];
        const propMap = this.insertionMap.get(sourceNode);
        const entry = propMap?.get(key);
        const insertAt = entry?.index;
        let inserted = false;

        for (let i = 0; i < value.length; i++) {
            if (!inserted && entry != undefined && insertAt != undefined && i === insertAt) {
                this.appendInsertedElements(entry.elements, copyParent, key, newArray);
                inserted = true;
            }
            const element = value[i];
            if (isAstNode(element)) {
                newArray.push(this.deepCopyNode(element, copyParent, key, newArray.length));
            } else {
                newArray.push(element);
            }
        }

        if (!inserted && entry != undefined) {
            this.appendInsertedElements(entry.elements, copyParent, key, newArray);
        }

        return newArray;
    }

    private appendInsertedElements(elements: AstNode[], parent: AstNode, key: string, target: unknown[]): void {
        for (const element of elements) {
            const insertedCopy = this.shallowCopyNode(element, parent, key, target.length);
            this.insertedOriginalToCopy.set(element, insertedCopy);
            this.copyToOriginal.set(insertedCopy, element);
            target.push(insertedCopy);
        }
    }

    private shallowCopyNode(
        node: AstNode,
        parent: AstNode,
        containerProperty: string,
        containerIndex: number
    ): AstNode {
        const nodeRecord = node as unknown as Record<string, unknown>;
        const copy: Record<string, unknown> = {};
        for (const key of Object.keys(node)) {
            copy[key] = nodeRecord[key];
        }
        copy.$container = parent;
        copy.$containerProperty = containerProperty;
        copy.$containerIndex = containerIndex;
        copy.$cstNode = undefined;
        copy.$document = undefined;
        return copy as unknown as AstNode;
    }
}

/**
 * Builds {@link MetadataEdits} that rename metadata entries according to the given
 * id changes. For each changed id, the old entry is removed (set to `null`) and
 * the existing metadata is re-inserted under the new id.
 *
 * @param idChanges The list of id changes from {@link InsertIdResult.idChanges}
 * @param currentMetadata The current graph metadata to look up existing entries
 * @returns MetadataEdits applying the renames, or `undefined` if there are no changes
 */
export function buildIdChangeMetadataEdits(
    idChanges: readonly IdChange[],
    currentMetadata: GraphMetadata
): MetadataEdits | undefined {
    if (idChanges.length === 0) {
        return undefined;
    }

    const nodeEdits: Record<string, Partial<NodeMetadata> | null> = {};
    const edgeEdits: Record<string, Partial<EdgeMetadata> | null> = {};

    for (const { oldId, newId } of idChanges) {
        const nodeEntry = currentMetadata.nodes[oldId];
        if (nodeEntry != undefined) {
            nodeEdits[oldId] = null;
            nodeEdits[newId] = nodeEntry;
        }

        const edgeEntry = currentMetadata.edges[oldId];
        if (edgeEntry != undefined) {
            edgeEdits[oldId] = null;
            edgeEdits[newId] = edgeEntry;
        }
    }

    const hasNodeEdits = Object.keys(nodeEdits).length > 0;
    const hasEdgeEdits = Object.keys(edgeEdits).length > 0;

    if (!hasNodeEdits && !hasEdgeEdits) {
        return undefined;
    }

    return {
        nodes: hasNodeEdits ? nodeEdits : undefined,
        edges: hasEdgeEdits ? edgeEdits : undefined
    };
}

/**
 * Merges two {@link MetadataEdits} objects. Entries from `b` override those from `a`
 * for the same key.
 *
 * @param a The first metadata edits (may be undefined)
 * @param b The second metadata edits (may be undefined)
 * @returns The merged result, or undefined if both inputs are undefined
 */
export function mergeMetadataEdits(
    a: MetadataEdits | undefined,
    b: MetadataEdits | undefined
): MetadataEdits | undefined {
    if (a == undefined) return b;
    if (b == undefined) return a;

    return {
        nodes: a.nodes != undefined || b.nodes != undefined ? { ...a.nodes, ...b.nodes } : undefined,
        edges: a.edges != undefined || b.edges != undefined ? { ...a.edges, ...b.edges } : undefined
    };
}

/**
 * Type guard for AstNode, checking for the presence of the `$type` property.
 */
function isAstNode(value: unknown): value is AstNode {
    return typeof value === "object" && value !== null && typeof (value as AstNode).$type === "string";
}

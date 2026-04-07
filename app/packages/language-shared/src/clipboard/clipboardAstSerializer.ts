import type { AstNode, MultiReference, Reference } from "langium";
import { isAstNode, isMultiReference, isReference } from "langium";
import type { Point } from "@eclipse-glsp/protocol";

/**
 * Format identifier for the clipboard data, used to verify compatibility on paste.
 */
export const CLIPBOARD_AST_FORMAT = "mdeo-ast-clipboard";

/**
 * Version number for the clipboard data format to handle potential future migrations.
 */
export const CLIPBOARD_AST_VERSION = 1;

/**
 * Format identifier for clipboard position data stored alongside the AST data.
 */
export const CLIPBOARD_POSITION_FORMAT = "mdeo-clipboard-positions";

/**
 * The fallback offset applied per axis when pasting without a known mouse
 * position, so that pasted content does not exactly overlap the original.
 */
export const PASTE_DEFAULT_OFFSET = 20;

/**
 * Position metadata stored in the clipboard alongside the serialized AST nodes.
 * Keys are the original node names; values are diagram-coordinate positions.
 */
export interface ClipboardPositionData {
    /**
     * Map from original node name to its position in diagram coordinates at
     * the time of the copy operation.
     */
    positions: Record<string, Point>;
    /**
     * Optional layout metadata for association edges in the clipboard selection.
     * Class names stored here are the original (pre-rename) values as written in
     * the source text.
     */
    edges?: ClipboardEdgeMetadata[];
}

/**
 * Layout metadata for a single association edge stored in the clipboard.
 * Carries routing information so the edge path can be restored (with offset)
 * after paste, even when source or target classes have been renamed.
 */
export interface ClipboardEdgeMetadata {
    /** Original (pre-rename) source class name as written in the source text. */
    sourceClass: string;
    /** Source-end property name on the association (not subject to rename). */
    sourceProperty: string;
    /** Original (pre-rename) target class name as written in the source text. */
    targetClass: string;
    /** Target-end property name on the association (not subject to rename). */
    targetProperty: string;
    /** Routing points of the edge in diagram coordinates at the time of copy. */
    routingPoints: Point[];
    /** Optional anchor at the source end of the edge. */
    sourceAnchor?: { side: string; value: number };
    /** Optional anchor at the target end of the edge. */
    targetAnchor?: { side: string; value: number };
}

/**
 * Root container of clipboard data, holding a list of serialized top-level AST nodes.
 */
export interface ClipboardAstData {
    /** Identifies the clipboard format. Must equal {@link CLIPBOARD_AST_FORMAT}. */
    format: typeof CLIPBOARD_AST_FORMAT;
    /** Data format version. */
    version: number;
    /** The serialized top-level AST nodes. */
    nodes: SerializedClipboardNode[];
}

/**
 * Serialized representation of an AST node within clipboard data.
 * Mirrors the structure of an AST node but without Langium runtime fields
 * ({@code $container}, {@code $cstNode}, etc.).
 */
export interface SerializedClipboardNode {
    /** The AST node type discriminator. */
    $type: string;
    /** Additional serialized properties. */
    [key: string]: SerializedClipboardEntry | SerializedClipboardEntry[];
}

/**
 * A single entry in a serialized clipboard node. Can be a primitive value,
 * a nested node, a reference, or {@code null}.
 */
export type SerializedClipboardEntry =
    | string
    | number
    | boolean
    | null
    | SerializedClipboardNode
    | ClipboardReference
    | ClipboardMultiReference;

/**
 * A serialized single reference.
 * If {@link $internal} is {@code true}, the reference target is part of the
 * clipboard data and its {@link $refText} will be updated during paste when
 * names are adjusted for uniqueness.
 */
export interface ClipboardReference {
    /** When {@code true}, the reference target is included in the clipboard data. */
    $internal?: true;
    /** The reference text (name) used when serializing back to source text. */
    $refText: string;
}

/**
 * A serialized multi-reference for clipboard data.
 * Contains multiple reference items, each of which can be internal or external.
 */
export interface ClipboardMultiReference {
    /** The individual reference items. */
    $refs: ClipboardReference[];
    /** The combined reference text. */
    $refText: string;
}

/**
 * Properties that must be excluded from clipboard serialization because they
 * represent Langium runtime state rather than semantic content.
 */
const IGNORE_PROPERTIES = new Set(["$container", "$containerProperty", "$containerIndex", "$document", "$cstNode"]);

/**
 * Type guard that checks whether an object is a {@link ClipboardReference}.
 *
 * @param obj - The value to test.
 * @returns {@code true} when the value has a {@code $refText} property and no {@code $refs} array.
 */
export function isClipboardReference(obj: unknown): obj is ClipboardReference {
    return typeof obj === "object" && obj !== null && "$refText" in obj && !("$refs" in obj) && !("$type" in obj);
}

/**
 * Type guard that checks whether an object is a {@link ClipboardMultiReference}.
 *
 * @param obj - The value to test.
 * @returns {@code true} when the value has both {@code $refs} and {@code $refText} properties.
 */
export function isClipboardMultiReference(obj: unknown): obj is ClipboardMultiReference {
    return typeof obj === "object" && obj !== null && "$refs" in obj && "$refText" in obj;
}

/**
 * Type guard that checks whether an object is a {@link SerializedClipboardNode}.
 *
 * @param obj - The value to test.
 * @returns {@code true} when the value has a {@code $type} property.
 */
export function isSerializedClipboardNode(obj: unknown): obj is SerializedClipboardNode {
    return typeof obj === "object" && obj !== null && "$type" in obj;
}

/**
 * Serializes a set of top-level AST nodes into a clipboard-compatible JSON structure.
 *
 * <p>Unlike {@code JsonAstSerializer}, this serializer does not require a full document.
 * It is designed for partial selections where only some top-level nodes are included.
 * Nodes may originate from different documents.
 *
 * <p>References are classified as either <em>internal</em> (pointing to a node that is
 * part of the clipboard data) or <em>external</em> (pointing to a node outside the
 * clipboard data). Internal references are tagged with {@code $internal: true} so that
 * their {@code $refText} can be updated during paste when names are changed for uniqueness.
 *
 * @param nodes - The top-level AST nodes to serialize.
 * @param includedNodes - The complete set of top-level AST nodes in the clipboard selection.
 *   Used to determine whether a reference target is internal or external.
 * @returns The serialized clipboard data ready for JSON stringification.
 */
export function serializeForClipboard(nodes: AstNode[], includedNodes: Set<AstNode>): ClipboardAstData {
    const serializedNodes = nodes.map((node) => serializeClipboardNode(node, includedNodes));
    return {
        format: CLIPBOARD_AST_FORMAT,
        version: CLIPBOARD_AST_VERSION,
        nodes: serializedNodes
    };
}

/**
 * Deserializes clipboard JSON text into a {@link ClipboardAstData} structure.
 * Validates the format identifier and version number before returning.
 *
 * @param json - The JSON string from the clipboard.
 * @returns The parsed clipboard data, or {@code undefined} if the data is invalid.
 */
export function deserializeClipboardData(json: string): ClipboardAstData | undefined {
    try {
        const data = JSON.parse(json) as ClipboardAstData;
        if (data.format !== CLIPBOARD_AST_FORMAT || typeof data.version !== "number" || !Array.isArray(data.nodes)) {
            return undefined;
        }
        return data;
    } catch {
        return undefined;
    }
}

/**
 * Generates unique names for all named top-level nodes in the clipboard data,
 * avoiding conflicts with existing names in the target document and with
 * other nodes being pasted simultaneously.
 *
 * <p>When a name needs to change, all <em>internal</em> references
 * ({@code $internal: true}) that match the old name are updated to the new name.
 *
 * @param data - The clipboard data whose nodes will be renamed in place.
 * @param existingNames - Set of names already present in the target document.
 * @returns A map from old names to new names (only entries where names changed).
 */
export function resolveUniqueNames(data: ClipboardAstData, existingNames: Set<string>): Map<string, string> {
    const renameMap = new Map<string, string>();
    const usedNames = new Set(existingNames);

    // First pass: determine new names for all named top-level nodes
    for (const node of data.nodes) {
        const name = getNodeName(node);
        if (name === undefined) {
            continue;
        }

        if (!usedNames.has(name)) {
            usedNames.add(name);
            continue;
        }

        const newName = findUniqueName(name, usedNames);
        renameMap.set(name, newName);
        usedNames.add(newName);
    }

    // Second pass: apply renames to node names and internal references
    if (renameMap.size > 0) {
        for (const node of data.nodes) {
            applyRenames(node, renameMap);
        }
    }

    return renameMap;
}

/**
 * Recursively serializes a single AST node for clipboard storage.
 *
 * @param node - The AST node to serialize.
 * @param includedNodes - Top-level nodes included in the clipboard selection.
 * @returns The serialized clipboard node.
 */
function serializeClipboardNode(node: AstNode, includedNodes: Set<AstNode>): SerializedClipboardNode {
    const result: SerializedClipboardNode = {
        $type: node.$type
    };

    for (const [key, value] of Object.entries(node)) {
        if (IGNORE_PROPERTIES.has(key)) {
            continue;
        }

        const serialized = serializeValue(value, includedNodes);
        if (serialized !== undefined) {
            result[key] = serialized;
        }
    }

    return result;
}

/**
 * Serializes a single property value, dispatching to the appropriate handler
 * based on the value's type (primitive, array, reference, AST node).
 *
 * @param value - The property value to serialize.
 * @param includedNodes - Top-level nodes included in the clipboard selection.
 * @returns The serialized value, or {@code undefined} when the value cannot be serialized.
 */
function serializeValue(
    value: unknown,
    includedNodes: Set<AstNode>
): SerializedClipboardEntry | SerializedClipboardEntry[] | undefined {
    if (value === undefined || value === null) {
        return value ?? null;
    }
    if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
        return value;
    }
    if (Array.isArray(value)) {
        return value
            .map((item) => serializeValue(item, includedNodes))
            .filter((item): item is SerializedClipboardEntry => item !== undefined);
    }
    if (isReference(value)) {
        return serializeReference(value, includedNodes);
    }
    if (isMultiReference(value)) {
        return serializeMultiReference(value, includedNodes);
    }
    if (isAstNode(value)) {
        return serializeClipboardNode(value, includedNodes);
    }
    return undefined;
}

/**
 * Serializes a single Langium {@link Reference} to a clipboard reference.
 * Marks the reference as internal when its resolved target is among the clipboard nodes.
 *
 * @param ref - The Langium reference to serialize.
 * @param includedNodes - Top-level nodes included in the clipboard selection.
 * @returns The serialized clipboard reference.
 */
function serializeReference(ref: Reference, includedNodes: Set<AstNode>): ClipboardReference {
    const refText = ref.$refText ?? "";
    const resolvedNode = ref.ref;

    if (resolvedNode && isNodeIncluded(resolvedNode, includedNodes)) {
        return { $internal: true, $refText: refText };
    }

    return { $refText: refText };
}

/**
 * Serializes a Langium {@link MultiReference} to a clipboard multi-reference.
 *
 * @param multiRef - The Langium multi-reference to serialize.
 * @param includedNodes - Top-level nodes included in the clipboard selection.
 * @returns The serialized clipboard multi-reference.
 */
function serializeMultiReference(multiRef: MultiReference, includedNodes: Set<AstNode>): ClipboardMultiReference {
    const items: ClipboardReference[] = [];
    for (const item of multiRef.items) {
        const resolvedNode = item.ref;
        if (resolvedNode && isNodeIncluded(resolvedNode, includedNodes)) {
            items.push({ $internal: true, $refText: resolvedNode.$type });
        }
    }
    return {
        $refs: items,
        $refText: multiRef.$refText ?? ""
    };
}

/**
 * Determines whether an AST node (or one of its ancestors) is among the
 * included top-level clipboard nodes.
 *
 * @param node - The AST node to check.
 * @param includedNodes - The set of included top-level nodes.
 * @returns {@code true} when the node or an ancestor is in the set.
 */
function isNodeIncluded(node: AstNode, includedNodes: Set<AstNode>): boolean {
    let current: AstNode | undefined = node;
    while (current) {
        if (includedNodes.has(current)) {
            return true;
        }
        current = current.$container;
    }
    return false;
}

/**
 * Extracts the name from a serialized clipboard node, if it has one.
 *
 * @param node - The serialized node.
 * @returns The name string, or {@code undefined} if the node has no {@code name} property.
 */
function getNodeName(node: SerializedClipboardNode): string | undefined {
    const name = node["name"];
    return typeof name === "string" ? name : undefined;
}

/**
 * Finds a unique name by appending a numeric suffix when needed.
 *
 * @param baseName - The desired name.
 * @param usedNames - Names already in use (both existing and previously assigned).
 * @returns A name not present in {@code usedNames}.
 */
function findUniqueName(baseName: string, usedNames: Set<string>): string {
    let suffix = 1;
    let candidate = `${baseName}${suffix}`;
    while (usedNames.has(candidate)) {
        suffix++;
        candidate = `${baseName}${suffix}`;
    }
    return candidate;
}

/**
 * Recursively applies name renames to a serialized clipboard node.
 * Updates the node's own {@code name} property and any internal references
 * whose {@code $refText} matches a renamed name.
 *
 * @param node - The serialized node to update in place.
 * @param renameMap - Mapping from old names to new names.
 */
function applyRenames(node: SerializedClipboardNode, renameMap: Map<string, string>): void {
    // Rename the node itself if it has a name
    const name = getNodeName(node);
    if (name !== undefined && renameMap.has(name)) {
        node["name"] = renameMap.get(name)!;
    }

    // Recursively update all properties
    for (const [key, value] of Object.entries(node)) {
        if (key === "$type") {
            continue;
        }
        applyRenamesInValue(node, key, value, renameMap);
    }
}

/**
 * Applies renames within a single property value (which may be a reference,
 * a nested node, an array, or a primitive).
 *
 * @param owner - The owning serialized node.
 * @param key - The property key.
 * @param value - The property value.
 * @param renameMap - Mapping from old names to new names.
 */
function applyRenamesInValue(
    owner: SerializedClipboardNode,
    key: string,
    value: SerializedClipboardEntry | SerializedClipboardEntry[],
    renameMap: Map<string, string>
): void {
    if (Array.isArray(value)) {
        for (const item of value) {
            applyRenamesInValue(owner, key, item, renameMap);
        }
        return;
    }

    if (isClipboardReference(value)) {
        if (value.$internal && renameMap.has(value.$refText)) {
            value.$refText = renameMap.get(value.$refText)!;
        }
        return;
    }

    if (isClipboardMultiReference(value)) {
        for (const ref of value.$refs) {
            if (ref.$internal && renameMap.has(ref.$refText)) {
                ref.$refText = renameMap.get(ref.$refText)!;
            }
        }
        return;
    }

    if (isSerializedClipboardNode(value)) {
        applyRenames(value, renameMap);
    }
}

/**
 * Converts a serialized clipboard node back into a plain AST-node-like object
 * suitable for the AST text serializer. References are converted to
 * {@code { $refText, ref: undefined }} format expected by Langium serializers.
 *
 * @param node - The serialized clipboard node to convert.
 * @returns A plain object with the shape of an AST node.
 */
export function toAstNodeLike(node: SerializedClipboardNode): Record<string, unknown> {
    const result: Record<string, unknown> = {
        $type: node.$type
    };

    for (const [key, value] of Object.entries(node)) {
        if (key === "$type") {
            continue;
        }
        result[key] = convertValue(value);
    }

    return result;
}

/**
 * Converts a single serialized clipboard entry back to an AST-compatible value.
 *
 * @param value - The serialized entry.
 * @returns The converted value.
 */
function convertValue(value: SerializedClipboardEntry | SerializedClipboardEntry[]): unknown {
    if (value === null || value === undefined) {
        return undefined;
    }
    if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
        return value;
    }
    if (Array.isArray(value)) {
        return value.map((item) => convertValue(item));
    }
    if (isClipboardReference(value)) {
        return { $refText: value.$refText, ref: undefined };
    }
    if (isClipboardMultiReference(value)) {
        return {
            $refText: value.$refText,
            items: value.$refs.map((ref) => ({ ref: undefined, $refText: ref.$refText }))
        };
    }
    if (isSerializedClipboardNode(value)) {
        return toAstNodeLike(value);
    }
    return undefined;
}

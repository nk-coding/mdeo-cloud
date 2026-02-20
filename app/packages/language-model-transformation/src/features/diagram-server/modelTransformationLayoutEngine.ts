import type { GModelRoot } from "@eclipse-glsp/server";
import type { LayoutOperation } from "@mdeo/editor-protocol";
import type { NodePositionMetadata, EdgeLayoutMetadata } from "@mdeo/editor-protocol";
import { BaseLayoutEngine, GEdge, GNode, sharedImport } from "@mdeo/language-shared";
import type { ElkExtendedEdge, ElkNode } from "elkjs";
import type { MetadataEdits } from "@mdeo/language-shared";
import { GMatchNode } from "./model/matchNode.js";
import { GPatternInstanceNode } from "./model/patternInstanceNode.js";
import { GPatternLinkEdge } from "./model/patternLinkEdge.js";
import { GControlFlowLabelNode } from "./model/controlFlowLabelNode.js";
import { GPatternLinkEndNode } from "./model/patternLinkEndNode.js";

const { injectable } = sharedImport("inversify");
const { Point } = sharedImport("@eclipse-glsp/protocol");

/**
 * Layout engine for model transformation diagrams.
 * Uses ELK for automatic layout computation.
 *
 * Handles nested nodes: match nodes contain pattern elements as children.
 * Pattern instances are positioned globally but displayed relative to match node.
 * ELK returns relative positions for children, which must be converted to global positions.
 */
@injectable()
export class ModelTransformationLayoutEngine extends BaseLayoutEngine {
    /**
     * Transforms the GModel to an ELK graph for layout computation.
     * Creates a hierarchical graph with match nodes containing pattern instances.
     *
     * @param model The GModelRoot to transform
     * @param operation The layout operation containing bounds information
     * @returns The ELK node representation of the graph
     */
    protected override transformToElk(model: GModelRoot, operation: LayoutOperation): ElkNode {
        const bounds = operation.bounds;
        const nodes: ElkNode[] = [];
        const edges: ElkExtendedEdge[] = [];

        for (const child of model.children) {
            if (child instanceof GMatchNode) {
                // Match nodes have children (pattern instances, links)
                const matchNode = this.transformMatchNode(child, bounds);
                nodes.push(matchNode);
            } else if (child instanceof GNode) {
                // Other nodes: start, end, split, merge
                nodes.push({
                    id: child.id,
                    width: bounds[child.id]?.width,
                    height: bounds[child.id]?.height
                });
            } else if (child instanceof GEdge) {
                // Control flow edges
                edges.push({
                    id: child.id,
                    sources: [child.sourceId],
                    targets: [child.targetId],
                    labels: child.children
                        .filter((label) => label instanceof GControlFlowLabelNode)
                        .map((label) => ({
                            text: "_",
                            width: bounds[label.id]?.width,
                            height: bounds[label.id]?.height,
                            layoutOptions: {
                                "elk.edgeLabels.placement": "HEAD"
                            }
                        }))
                });
            }
        }

        return {
            id: model.id,
            layoutOptions: {
                "elk.algorithm": "layered",
                "elk.direction": "DOWN",
                "elk.spacing.nodeNode": "50",
                "elk.layered.spacing.nodeNodeBetweenLayers": "50"
            },
            children: nodes,
            edges: edges
        };
    }

    /**
     * Transforms a match node to an ELK node with children.
     * Pattern instances become ELK child nodes, pattern links become ELK edges.
     *
     * @param matchNode The match node to transform
     * @param bounds The bounds information from the layout operation
     * @returns The ELK node representation
     */
    private transformMatchNode(matchNode: GMatchNode, bounds: LayoutOperation["bounds"]): ElkNode {
        const childNodes: ElkNode[] = [];
        const childEdges: ElkExtendedEdge[] = [];

        // Process children: pattern instances and links
        for (const child of matchNode.children) {
            if (child instanceof GPatternInstanceNode) {
                childNodes.push({
                    id: child.id,
                    width: bounds[child.id]?.width,
                    height: bounds[child.id]?.height
                });
            } else if (child instanceof GPatternLinkEdge) {
                childEdges.push({
                    id: child.id,
                    sources: [child.sourceId],
                    targets: [child.targetId],
                    labels: child.children
                        .filter((label) => label instanceof GPatternLinkEndNode)
                        .map((label) => {
                            const endNode = label as GPatternLinkEndNode;
                            return {
                                text: "_",
                                width: bounds[label.id]?.width,
                                height: bounds[label.id]?.height,
                                layoutOptions: {
                                    "elk.edgeLabels.placement": endNode.end === "source" ? "HEAD" : "TAIL"
                                }
                            };
                        })
                });
            }
        }

        return {
            id: matchNode.id,
            width: bounds[matchNode.id]?.width,
            height: bounds[matchNode.id]?.height,
            layoutOptions: {
                "elk.algorithm": "layered",
                "elk.direction": "RIGHT",
                "elk.spacing.nodeNode": "30",
                "elk.padding": "[top=40,left=20,bottom=20,right=20]"
            },
            children: childNodes,
            edges: childEdges
        };
    }

    /**
     * Extracts layout metadata from the ELK graph after layouting.
     * Converts ELK's relative positions for nested nodes to global positions.
     *
     * @param graph The ELK graph with computed layout
     * @returns The metadata edits to apply
     */
    protected override extractMetadata(graph: ElkNode): MetadataEdits {
        const currentMetadata = this.modelState.getValidatedMetadata();
        const edits: Required<MetadataEdits> = {
            nodes: {},
            edges: {}
        };

        // Process top-level nodes and their children
        this.extractMetadataRecursive(graph, 0, 0, currentMetadata, edits);

        return edits;
    }

    /**
     * Recursively extracts metadata from ELK nodes.
     * Converts relative positions to global by adding parent offsets.
     *
     * @param node The current ELK node
     * @param parentX The global X offset from parent
     * @param parentY The global Y offset from parent
     * @param currentMetadata The current metadata for checking existence
     * @param edits The edits to accumulate
     */
    private extractMetadataRecursive(
        node: ElkNode,
        parentX: number,
        parentY: number,
        currentMetadata: { nodes: Record<string, unknown>; edges: Record<string, unknown> },
        edits: Required<MetadataEdits>
    ): void {
        // Process this node if it's in the metadata
        if (node.id in currentMetadata.nodes) {
            const nodeMeta = this.extractNodeMetadataWithOffset(node, parentX, parentY);
            if (nodeMeta != undefined) {
                edits.nodes[node.id] = { meta: nodeMeta };
            }
        }

        // Calculate the global offset for children
        // Children are positioned relative to parent, so add parent's position
        const nodeX = (node.x ?? 0) + parentX;
        const nodeY = (node.y ?? 0) + parentY;

        // Process edges within this node
        for (const edge of node.edges ?? []) {
            if (!(edge.id in currentMetadata.edges)) {
                continue;
            }
            const edgeMeta = this.extractEdgeMetadataWithOffset(edge as ElkExtendedEdge, nodeX, nodeY);
            if (edgeMeta != undefined) {
                edits.edges[edge.id] = { meta: edgeMeta };
            }
        }

        // Process children with accumulated offset
        for (const child of node.children ?? []) {
            this.extractMetadataRecursive(child, nodeX, nodeY, currentMetadata, edits);
        }
    }

    /**
     * Extracts node position metadata with parent offset.
     * Converts ELK's relative position to global position.
     *
     * @param node The ELK node
     * @param parentX Parent's global X position
     * @param parentY Parent's global Y position
     * @returns The position metadata
     */
    private extractNodeMetadataWithOffset(
        node: ElkNode,
        parentX: number,
        parentY: number
    ): NodePositionMetadata | undefined {
        if (node.x == undefined || node.y == undefined) {
            return undefined;
        }
        // Convert relative position to global position
        return {
            position: {
                x: node.x + parentX,
                y: node.y + parentY
            }
        };
    }

    /**
     * Extracts edge layout metadata with parent offset.
     * Converts ELK's relative routing points to global positions.
     *
     * @param edge The ELK edge
     * @param parentX Parent's global X position
     * @param parentY Parent's global Y position
     * @returns The edge layout metadata
     */
    private extractEdgeMetadataWithOffset(
        edge: ElkExtendedEdge,
        parentX: number,
        parentY: number
    ): EdgeLayoutMetadata | undefined {
        const section = edge.sections?.[0];
        if (section?.bendPoints == undefined) {
            return undefined;
        }

        // Convert relative routing points to global positions
        const points =
            section.bendPoints.length > 0
                ? section.bendPoints.map((pt) => ({
                      x: pt.x + parentX,
                      y: pt.y + parentY
                  }))
                : [
                      Point.linear(
                          { x: section.startPoint.x + parentX, y: section.startPoint.y + parentY },
                          { x: section.endPoint.x + parentX, y: section.endPoint.y + parentY },
                          0.5
                      )
                  ];

        return {
            routingPoints: points
        };
    }
}

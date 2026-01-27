import type { ELK, ElkExtendedEdge, ElkNode } from "elkjs";
import { sharedImport } from "../sharedImport.js";
import type { ActionDispatcher, GModelRoot } from "@eclipse-glsp/server";
import type { ModelState } from "./modelState.js";
import type { EdgeLayoutMetadata, LayoutOperation, NodePositionMetadata } from "@mdeo/editor-protocol";
import type { MetadataEdits } from "./handler/operationHandlerCommand.js";

const { injectable, inject } = sharedImport("inversify");
const { ModelState: ModelStateKey, ActionDispatcher: ActionDispatcherKey } = sharedImport("@eclipse-glsp/server");
const { Point } = sharedImport("@eclipse-glsp/protocol");
const elkjs = sharedImport("elkjs");

/**
 * Workaround to obtain the Elk class from the elkjs module
 */
const Elk = elkjs.default as unknown as typeof elkjs.default.default;

/**
 * Base implementation of a layout engine using ELK
 */
@injectable()
export abstract class BaseLayoutEngine {
    protected readonly elk: ELK;

    /**
     * The current model state
     */
    @inject(ModelStateKey)
    protected readonly modelState!: ModelState;

    /**
     * The action dispatcher
     */
    @inject(ActionDispatcherKey)
    protected readonly actionDispatcher!: ActionDispatcher;

    constructor() {
        this.elk = new Elk();
    }

    /**
     * Performs layouting for the current model state based on the given operation
     *
     * @param operation the layout operation
     * @returns the metadata edits resulting from the layout
     */
    async layout(operation: LayoutOperation): Promise<MetadataEdits> {
        const root = this.modelState.root;
        const elkNode = this.transformToElk(root, operation);
        const elkLayout = await this.elk.layout(elkNode);
        return this.extractMetadata(elkLayout);
    }

    /**
     * Transforms the given model to an ELK node for layouting
     *
     * @param model the model to transform
     * @param operation the layout operation, provides layout options and bounds
     * @return the transformed ELK node
     */
    protected abstract transformToElk(model: GModelRoot, operation: LayoutOperation): ElkNode;

    /**
     * Extracts layout metadata from the given ELK graph after layouting
     *
     * @param graph the ELK graph to extract metadata from
     * @returns the extracted metadata edits
     */
    protected extractMetadata(graph: ElkNode): MetadataEdits {
        const currentMetadata = this.modelState.getValidatedMetadata();
        const edits: Required<MetadataEdits> = {
            nodes: {},
            edges: {}
        };
        const traverse = (node: ElkNode) => {
            if (node.id in currentMetadata.nodes) {
                const nodeMeta = this.extractNodeMetadata(node);
                if (nodeMeta != undefined) {
                    edits.nodes[node.id] = { meta: nodeMeta };
                }
            }
            for (const edge of node.edges ?? []) {
                if (!(edge.id in currentMetadata.edges)) {
                    continue;
                }
                const edgeMeta = this.extractEdgeMetadata(edge as ElkExtendedEdge);
                if (edgeMeta != undefined) {
                    edits.edges[edge.id] = { meta: edgeMeta };
                }
            }
            for (const child of node.children ?? []) {
                traverse(child);
            }
        };
        traverse(graph);
        return edits;
    }

    /**
     * Extracts position metadata from the given ELK node
     *
     * @param node the ELK node to extract metadata from
     * @returns the extracted position metadata, or undefined if no metadata is available
     */
    protected extractNodeMetadata(node: ElkNode): NodePositionMetadata | undefined {
        if (node.x == undefined || node.y == undefined) {
            return undefined;
        }
        return {
            position: { x: node.x, y: node.y }
        };
    }

    /**
     * Extracts layout metadata from the given ELK edge
     *
     * @param edge the ELK edge to extract metadata from
     * @returns the extracted edge layout metadata, or undefined if no metadata is available
     */
    protected extractEdgeMetadata(edge: ElkExtendedEdge): EdgeLayoutMetadata | undefined {
        const section = edge.sections?.[0];
        if (section?.bendPoints == undefined) {
            return undefined;
        }
        const points =
            section.bendPoints.length > 0
                ? section.bendPoints.map((pt) => ({ x: pt.x, y: pt.y }))
                : [Point.linear(section.startPoint, section.endPoint, 0.5)];
        return {
            routingPoints: points
        };
    }
}

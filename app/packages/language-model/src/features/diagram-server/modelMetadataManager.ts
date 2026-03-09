import {
    sharedImport,
    MetadataManager,
    type GraphMetadata,
    type NodeMetadata,
    type EdgeMetadata,
    type ModelIdRegistry,
    DefaultModelIdRegistry,
    ModelIdProvider,
    type ModelIdProvider as ModelIdProviderType
} from "@mdeo/language-shared";
import type { NodeAttributes, EdgeAttributes } from "@mdeo/language-shared";
import { ModelElementType } from "./model/elementTypes.js";
import type { PartialModel, PartialObjectInstance, PartialLink } from "../../grammar/modelPartialTypes.js";
import { EdgeLayoutMetadataUtil, NodeLayoutMetadataUtil } from "./metadataTypes.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Manages metadata validation and synchronization for model diagrams.
 * Implements cost calculations based on semantic similarity between model elements.
 */
@injectable()
export class ModelMetadataManager extends MetadataManager<PartialModel> {
    @inject(ModelIdProvider)
    protected modelIdProvider!: ModelIdProviderType;

    /**
     * Verifies and corrects invalid metadata for nodes and edges.
     *
     * @param model The node or edge metadata to verify
     * @returns Corrected metadata if invalid, undefined if valid
     */
    protected override verifyMetadata(model: NodeMetadata | EdgeMetadata): object | undefined {
        if (model.type === ModelElementType.NODE_OBJECT) {
            return NodeLayoutMetadataUtil.verify(model.meta, 250);
        }

        if (model.type === ModelElementType.NODE_LINK_END) {
            return NodeLayoutMetadataUtil.verify(model.meta);
        }

        if (model.type === ModelElementType.EDGE_LINK) {
            const edgeModel = model as EdgeMetadata;
            return EdgeLayoutMetadataUtil.verify(edgeModel.meta);
        }

        return undefined;
    }

    /**
     * Calculate the cost of transforming one node to another.
     *
     * @param node1 First NodeAttributes
     * @param node2 Second NodeAttributes
     * @returns Cost of transformation
     */
    protected calculateNodeCost(node1: NodeAttributes | undefined, node2: NodeAttributes | undefined): number {
        if (node1 == undefined || node2 == undefined) {
            return 1;
        }
        if (node1.id === node2.id) {
            return 0;
        }

        const type1 = node1.type as string;
        const type2 = node2.type as string;

        if (type1 !== type2) {
            return 2;
        }

        if (type1 === ModelElementType.NODE_OBJECT) {
            const similarity = this.calculateObjectSimilarity(node1, node2);
            return 2 - similarity;
        }

        return 1;
    }

    /**
     * Calculate the cost of transforming one edge to another.
     *
     * @param edge1 First EdgeAttributes
     * @param edge2 Second EdgeAttributes
     * @returns Cost of transformation
     */
    protected calculateEdgeCost(edge1: EdgeAttributes | undefined, edge2: EdgeAttributes | undefined): number {
        if (edge1 == undefined || edge2 == undefined) {
            return 1;
        }
        if (edge1.id === edge2.id) {
            return 0;
        }

        const type1 = edge1.type as string;
        const type2 = edge2.type as string;

        if (type1 !== type2) {
            return 2;
        }

        if (type1 === ModelElementType.EDGE_LINK) {
            const similarity = this.calculateLinkSimilarity(edge1, edge2);
            return 2 - similarity;
        }

        return 1;
    }

    /**
     * Calculates similarity between two object nodes.
     *
     * @param node1 First node attributes
     * @param node2 Second node attributes
     * @returns Similarity score between 0 and 1
     */
    private calculateObjectSimilarity(node1: NodeAttributes, node2: NodeAttributes): number {
        const attrs1 = node1 as Record<string, unknown>;
        const attrs2 = node2 as Record<string, unknown>;
        const name1 = attrs1.name as string | undefined;
        const name2 = attrs2.name as string | undefined;
        const type1 = attrs1.typeName as string | undefined;
        const type2 = attrs2.typeName as string | undefined;

        let score = 0;

        if (name1 === name2) {
            score += 0.5;
        }

        if (type1 === type2) {
            score += 0.5;
        }

        return score;
    }

    /**
     * Calculates similarity between two link edges.
     *
     * @param edge1 First edge attributes
     * @param edge2 Second edge attributes
     * @returns Similarity score between 0 and 1
     */
    private calculateLinkSimilarity(edge1: EdgeAttributes, edge2: EdgeAttributes): number {
        const attrs1 = edge1 as Record<string, unknown>;
        const attrs2 = edge2 as Record<string, unknown>;
        const source1 = attrs1.sourceId as string | undefined;
        const source2 = attrs2.sourceId as string | undefined;
        const target1 = attrs1.targetId as string | undefined;
        const target2 = attrs2.targetId as string | undefined;

        let score = 0;

        if (source1 === source2) {
            score += 0.5;
        }

        if (target1 === target2) {
            score += 0.5;
        }

        return score;
    }

    /**
     * Extracts graph metadata from the model source.
     *
     * @param sourceModel The model source
     * @returns Extracted graph metadata
     */
    protected extractGraphMetadata(sourceModel: PartialModel): GraphMetadata {
        const nodes: Record<string, NodeMetadata> = {};
        const edges: Record<string, EdgeMetadata> = {};

        const idRegistry = new DefaultModelIdRegistry(sourceModel, this.modelIdProvider);
        const { objects, links } = this.extractObjectsAndLinks(sourceModel);

        this.extractObjectMetadata(objects, idRegistry, nodes);
        this.extractLinkMetadata(links, idRegistry, edges, nodes);

        return { nodes, edges };
    }

    /**
     * Extracts objects and links from the model.
     *
     * @param sourceModel The model source
     * @returns Objects and links arrays
     */
    private extractObjectsAndLinks(sourceModel: PartialModel): {
        objects: PartialObjectInstance[];
        links: PartialLink[];
    } {
        const objects: PartialObjectInstance[] = [];
        const links: PartialLink[] = [];

        for (const obj of sourceModel.objects ?? []) {
            if (obj != undefined) {
                objects.push(obj);
            }
        }

        for (const link of sourceModel.links ?? []) {
            if (link != undefined) {
                links.push(link);
            }
        }

        return { objects, links };
    }

    /**
     * Extracts metadata for all objects.
     *
     * @param objects List of objects in the model
     * @param idRegistry Model ID registry
     * @param nodes Record to populate with node metadata
     */
    private extractObjectMetadata(
        objects: PartialObjectInstance[],
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>
    ): void {
        for (const obj of objects) {
            const nodeId = idRegistry.getId(obj);
            if (nodeId) {
                nodes[nodeId] = {
                    type: ModelElementType.NODE_OBJECT,
                    attrs: this.createObjectAttributes(obj)
                };
            }
        }
    }

    /**
     * Creates attributes for an object node.
     *
     * @param obj The object instance
     * @returns The object attributes
     */
    private createObjectAttributes(obj: PartialObjectInstance): Record<string, unknown> {
        const classRef = obj.class;
        const typeName = classRef?.$refText ?? (classRef?.ref as { name?: string } | undefined)?.name ?? "Unknown";
        return {
            name: obj.name ?? "unnamed",
            typeName
        };
    }

    /**
     * Extracts metadata for all links and their label nodes.
     *
     * @param links List of links in the model
     * @param idRegistry Model ID registry
     * @param edges Record to populate with edge metadata
     * @param nodes Record to populate with node metadata
     */
    private extractLinkMetadata(
        links: PartialLink[],
        idRegistry: ModelIdRegistry,
        edges: Record<string, EdgeMetadata>,
        nodes: Record<string, NodeMetadata>
    ): void {
        for (const link of links) {
            const edgeId = idRegistry.getId(link);
            const sourceObj = link.source?.object?.ref;
            const targetObj = link.target?.object?.ref;

            if (edgeId && sourceObj && targetObj) {
                edges[edgeId] = {
                    type: ModelElementType.EDGE_LINK,
                    from: idRegistry.getId(sourceObj),
                    to: idRegistry.getId(targetObj),
                    attrs: this.createLinkAttributes(link)
                };

                this.extractLinkLabelMetadata(link, edgeId, nodes);
            }
        }
    }

    /**
     * Extracts metadata for link label nodes.
     *
     * @param link The link definition
     * @param edgeId The edge ID
     * @param nodes Record to populate with node metadata
     */
    private extractLinkLabelMetadata(link: PartialLink, edgeId: string, nodes: Record<string, NodeMetadata>): void {
        if (link.source?.property != undefined) {
            nodes[`${edgeId}#source-node`] = {
                type: ModelElementType.NODE_LINK_END,
                attrs: {}
            };
        }

        if (link.target?.property != undefined) {
            nodes[`${edgeId}#target-node`] = {
                type: ModelElementType.NODE_LINK_END,
                attrs: {}
            };
        }
    }

    /**
     * Creates attributes for a link edge.
     *
     * @param link The link
     * @returns The link attributes
     */
    private createLinkAttributes(link: PartialLink): Record<string, unknown> {
        return {
            sourceProperty: link.source?.property?.$refText,
            targetProperty: link.target?.property?.$refText
        };
    }
}

import type { GModelFactory } from "@eclipse-glsp/server";
import { sharedImport, ModelState } from "@mdeo/language-shared";
import type { GraphMetadata } from "@mdeo/language-shared";
import { NodeLayoutMetadata, EdgeVisualMetadata, EdgePlacementMetadata } from "@mdeo/language-shared";
import type {
    MetaModelType,
    MetaClassType,
    AssociationType,
    MultiplicityType,
    SingleMultiplicityType,
    RangeMultiplicityType,
    MetaClassOrImportType
} from "../../grammar/metamodelTypes.js";
import {
    MetaClassNode,
    InheritanceEdge,
    AssociationEdge,
    MetaClassLabel,
    PropertyLabel,
    AssociationEndLabel
} from "./metamodelModelExtensions.js";
import type { Reference } from "langium";

const { injectable, inject } = sharedImport("inversify");
const { GGraph } = sharedImport("@eclipse-glsp/server");

type GGraphType = ReturnType<typeof GGraph.builder>["proxy"];

/**
 * Factory for creating GLSP graph models from metamodel AST.
 * Transforms the metamodel source model into a visual graph representation
 * with nodes for classes and edges for relationships.
 */
@injectable()
export class MetamodelGModelFactory implements GModelFactory {
    @inject(ModelState)
    protected modelState!: ModelState<MetaModelType>;

    /**
     * Creates the graph model from the current source model in the model state.
     * This method orchestrates the transformation of the metamodel AST into
     * a GLSP graph with positioned nodes and edges.
     */
    createModel(): void {
        const metamodel = this.modelState.sourceModel;
        if (metamodel == undefined) {
            return;
        }

        const graph = GGraph.builder().id("metamodel-graph").build();

        const { classes, associations } = this.extractClassesAndAssociations(metamodel);
        const nodeMap = this.createMetaClassNodes(graph, classes);
        this.createInheritanceEdges(graph, classes, nodeMap);
        this.createAssociationEdges(graph, associations, nodeMap);

        this.modelState.updateRoot(graph);
    }

    /**
     * Extracts and separates classes and associations from the metamodel.
     *
     * @param metamodel - The metamodel containing classes and associations
     * @returns An object containing separate arrays of classes and associations
     */
    private extractClassesAndAssociations(metamodel: MetaModelType): {
        classes: MetaClassType[];
        associations: AssociationType[];
    } {
        const classes: MetaClassType[] = [];
        const associations: AssociationType[] = [];

        for (const item of metamodel.classesAndAssociations) {
            if (item.$type === "MetaClass") {
                classes.push(item as MetaClassType);
            } else if (item.$type === "Association") {
                associations.push(item as AssociationType);
            }
        }

        return { classes, associations };
    }

    /**
     * Creates visual nodes for all metaclasses with their properties.
     * Nodes are positioned based on metadata or default grid layout.
     *
     * @param graph - The graph to add nodes to
     * @param classes - Array of metaclasses to create nodes for
     * @returns A map from class name to created node
     */
    private createMetaClassNodes(graph: GGraphType, classes: MetaClassType[]): Map<string, MetaClassNode> {
        const nodeMap = new Map<string, MetaClassNode>();
        let nodeIndex = 0;

        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const metaClass of classes) {
            const nodeId = `metaclass_${metaClass.name}`;
            const metadata = validatedMetadata.nodes[nodeId];

            const node = MetaClassNode.builder()
                .id(nodeId)
                .name(metaClass.name)
                .isAbstract(metaClass.isAbstract)
                .layout("vbox")
                .build();

            this.applyNodePosition(node, metadata, nodeIndex, classes.length);
            this.addNodeLabels(node, nodeId, metaClass);

            nodeMap.set(metaClass.name, node);
            graph.children.push(node);
            nodeIndex++;
        }

        return nodeMap;
    }

    /**
     * Applies position to a node from metadata or uses default grid layout.
     *
     * @param node - The node to position
     * @param metadata - Metadata containing position information
     * @param nodeIndex - Index for default grid positioning
     * @param totalNodes - Total number of nodes for grid calculation
     */
    private applyNodePosition(
        node: MetaClassNode,
        metadata: { meta?: object } | undefined,
        nodeIndex: number,
        totalNodes: number
    ): void {
        if (metadata?.meta && NodeLayoutMetadata.isValid(metadata.meta)) {
            node.position = metadata.meta.position;
            if (metadata.meta.preferredWidth !== undefined) {
                node.size = { width: metadata.meta.preferredWidth, height: node.size?.height ?? 0 };
            }
        } else {
            const gridSize = Math.ceil(Math.sqrt(totalNodes));
            const col = nodeIndex % gridSize;
            const row = Math.floor(nodeIndex / gridSize);
            node.position = { x: col * 250, y: row * 200 };
        }
    }

    /**
     * Adds label children to a metaclass node for the class name and properties.
     *
     * @param node - The node to add labels to
     * @param nodeId - The ID of the node for creating label IDs
     * @param metaClass - The metaclass containing the data to display
     */
    private addNodeLabels(node: MetaClassNode, nodeId: string, metaClass: MetaClassType): void {
        const nameLabel = MetaClassLabel.builder().id(`${nodeId}_name`).text(metaClass.name).build();
        node.children.push(nameLabel);

        for (const prop of metaClass.properties) {
            const multiplicityStr = this.formatMultiplicity(prop.multiplicity);
            const propText = `${prop.name}: ${prop.type.name}${multiplicityStr}`;

            const propLabel = PropertyLabel.builder().id(`${nodeId}_prop_${prop.name}`).text(propText).build();
            node.children.push(propLabel);
        }
    }

    /**
     * Creates inheritance edges for all extends relationships.
     *
     * @param graph - The graph to add edges to
     * @param classes - Array of metaclasses with extends relationships
     * @param nodeMap - Map from class name to node for edge connections
     */
    private createInheritanceEdges(
        graph: GGraphType,
        classes: MetaClassType[],
        nodeMap: Map<string, MetaClassNode>
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        let edgeIndex = 0;

        for (const metaClass of classes) {
            for (const superClass of metaClass.extends) {
                const superClassName = this.resolveClassName(superClass);
                if (superClassName) {
                    const sourceNode = nodeMap.get(metaClass.name);
                    const targetNode = nodeMap.get(superClassName);

                    if (sourceNode && targetNode) {
                        const edgeId = `inheritance_${edgeIndex++}`;
                        const edge = this.createInheritanceEdge(
                            edgeId,
                            sourceNode.id,
                            targetNode.id,
                            validatedMetadata
                        );
                        graph.children.push(edge);
                    }
                }
            }
        }
    }

    /**
     * Creates a single inheritance edge with routing points from metadata if available.
     *
     * @param edgeId - The ID for the edge
     * @param sourceId - The source node ID
     * @param targetId - The target node ID
     * @param validatedMetadata - The validated metadata containing edge information
     * @returns The created edge
     */
    private createInheritanceEdge(
        edgeId: string,
        sourceId: string,
        targetId: string,
        validatedMetadata: GraphMetadata
    ): InheritanceEdge {
        const metadata = validatedMetadata.edges[edgeId];
        const edge = InheritanceEdge.builder().id(edgeId).sourceId(sourceId).targetId(targetId).build();

        this.applyRoutingPoints(edge, metadata);
        return edge;
    }

    /**
     * Creates association edges for all associations in the metamodel.
     *
     * @param graph - The graph to add edges to
     * @param associations - Array of associations to create edges for
     * @param nodeMap - Map from class name to node for edge connections
     */
    private createAssociationEdges(
        graph: GGraphType,
        associations: AssociationType[],
        nodeMap: Map<string, MetaClassNode>
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();
        let edgeIndex = 0;

        for (const assoc of associations) {
            const startClassName = this.resolveClassName(assoc.start.class);
            const targetClassName = this.resolveClassName(assoc.target.class);

            if (startClassName && targetClassName) {
                const startNode = nodeMap.get(startClassName);
                const targetNode = nodeMap.get(targetClassName);

                if (startNode && targetNode) {
                    const edgeId = `association_${edgeIndex++}`;
                    const edge = this.createAssociationEdge(
                        edgeId,
                        assoc,
                        startNode.id,
                        targetNode.id,
                        validatedMetadata
                    );
                    graph.children.push(edge);
                }
            }
        }
    }

    /**
     * Creates a single association edge with all properties.
     *
     * @param edgeId - The ID for the edge
     * @param assoc - The association definition
     * @param sourceId - The source node ID
     * @param targetId - The target node ID
     * @param validatedMetadata - The validated metadata containing edge information
     * @returns The created edge
     */
    private createAssociationEdge(
        edgeId: string,
        assoc: AssociationType,
        sourceId: string,
        targetId: string,
        validatedMetadata: GraphMetadata
    ): AssociationEdge {
        const metadata = validatedMetadata.edges[edgeId];
        const edge = AssociationEdge.builder()
            .id(edgeId)
            .sourceId(sourceId)
            .targetId(targetId)
            .operator(assoc.operator)
            .build();

        this.applyRoutingPoints(edge, metadata);

        const startLabel = this.createAssociationEndLabel(
            `${edgeId}_start`,
            assoc.start.property,
            assoc.start.multiplicity,
            validatedMetadata,
            0.2
        );
        if (startLabel) {
            edge.children.push(startLabel);
        }

        const targetLabel = this.createAssociationEndLabel(
            `${edgeId}_target`,
            assoc.target.property,
            assoc.target.multiplicity,
            validatedMetadata,
            0.8
        );
        if (targetLabel) {
            edge.children.push(targetLabel);
        }

        return edge;
    }

    /**
     * Creates a label for an association endpoint with property name and multiplicity.
     *
     * @param labelId - The ID for the label
     * @param property - The property name (optional)
     * @param multiplicity - The multiplicity (optional)
     * @param validatedMetadata - The validated metadata containing label placement
     * @param defaultPosition - Default position along edge if no metadata
     * @returns The created label or undefined if no property or multiplicity
     */
    private createAssociationEndLabel(
        labelId: string,
        property: string | undefined,
        multiplicity: MultiplicityType | undefined,
        validatedMetadata: GraphMetadata,
        defaultPosition: number
    ): AssociationEndLabel | undefined {
        const multiplicityStr = this.formatMultiplicity(multiplicity);

        if (!property && !multiplicityStr) {
            return undefined;
        }

        const text = property && multiplicityStr ? `${property} ${multiplicityStr}` : property || multiplicityStr;

        const label = AssociationEndLabel.builder().id(labelId).text(text).build();

        const labelMeta = validatedMetadata.nodes[labelId];
        if (labelMeta?.meta && EdgePlacementMetadata.isValid(labelMeta.meta)) {
            const placement = labelMeta.meta;
            label.edgePlacement = {
                position: placement.position,
                rotate: false,
                side: placement.side ?? "top",
                offset: placement.offset ?? 0
            };
        } else {
            label.edgePlacement = {
                position: defaultPosition,
                rotate: false,
                side: "top",
                offset: 0
            };
        }

        return label;
    }

    /**
     * Applies routing points to an edge from metadata if available.
     *
     * @param edge - The edge to apply routing points to
     * @param metadata - Metadata containing routing points
     */
    private applyRoutingPoints(edge: InheritanceEdge | AssociationEdge, metadata: { meta?: object } | undefined): void {
        if (metadata?.meta && EdgeVisualMetadata.isValid(metadata.meta)) {
            edge.routingPoints = metadata.meta.routingPoints;
        }
    }

    /**
     * Resolves the class name from a reference, handling both local and imported classes.
     * For imported classes, uses the import name if available, otherwise falls back to the referenced name.
     *
     * @param ref - Reference to a metaclass (local or imported)
     * @returns The resolved class name or undefined if not resolvable
     */
    private resolveClassName(ref: Reference<MetaClassOrImportType>): string | undefined {
        const resolved = ref.ref;
        if (!resolved) {
            return undefined;
        }

        if (resolved.$type === "MetaClass") {
            return (resolved as MetaClassType).name;
        }

        if (resolved.$type === "MetaClassImport") {
            const importNode = resolved as { name?: string; element?: Reference<MetaClassType> };
            if (importNode.name) {
                return importNode.name;
            }
            const importedClass = importNode.element?.ref;
            if (importedClass && importedClass.$type === "MetaClass") {
                return (importedClass as MetaClassType).name;
            }
        }

        return undefined;
    }

    /**
     * Formats multiplicity for display in labels.
     * Handles both single multiplicities (*, +, ?, or numeric) and range multiplicities.
     *
     * @param multiplicity - The multiplicity to format
     * @returns Formatted string like "[*]", "[1]", "[0..1]", or empty string if no multiplicity
     */
    private formatMultiplicity(multiplicity: MultiplicityType | undefined): string {
        if (!multiplicity) {
            return "";
        }

        if (multiplicity.$type === "SingleMultiplicity") {
            const single = multiplicity as SingleMultiplicityType;
            if (single.value) {
                return `[${single.value}]`;
            }
            if (single.numericValue !== undefined) {
                return `[${single.numericValue}]`;
            }
        } else if (multiplicity.$type === "RangeMultiplicity") {
            const range = multiplicity as RangeMultiplicityType;
            const upper = range.upper || range.upperNumeric;
            return `[${range.lower}..${upper}]`;
        }

        return "";
    }
}

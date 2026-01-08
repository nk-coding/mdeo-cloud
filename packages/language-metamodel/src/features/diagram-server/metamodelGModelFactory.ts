import type { GModelFactory } from "@eclipse-glsp/server";
import { sharedImport, ModelIdProvider, ModelIdRegistry } from "@mdeo/language-shared";
import type { GraphMetadata, ModelState } from "@mdeo/language-shared";
import { NodeLayoutMetadata, EdgeVisualMetadata, EdgePlacementMetadata } from "@mdeo/language-shared";
import type {
    MetaModelType,
    ClassType,
    AssociationType,
    MultiplicityType,
    SingleMultiplicityType,
    RangeMultiplicityType
} from "../../grammar/metamodelTypes.js";
import {
    ClassNode,
    InheritanceEdge,
    AssociationEdge,
    ClassLabel,
    PropertyLabel,
    AssociationEndLabel,
    ClassCompartment,
    ClassDivider
} from "./metamodelModelExtensions.js";

const { injectable, inject } = sharedImport("inversify");
const { GGraph, ModelState: ModelStateKey } = sharedImport("@eclipse-glsp/server");

type GGraphType = ReturnType<typeof GGraph.builder>["proxy"];

/**
 * Factory for creating GLSP graph models from metamodel AST.
 * Transforms the metamodel source model into a visual graph representation
 * with nodes for classes and edges for relationships.
 */
@injectable()
export class MetamodelGModelFactory implements GModelFactory {
    /**
     * Injected model state
     */
    @inject(ModelStateKey)
    protected modelState!: ModelState<MetaModelType>;

    /**
     * Injected model ID provider for generating unique IDs
     */
    @inject(ModelIdProvider)
    protected modelIdProvider!: ModelIdProvider;

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

        const idRegistry = new ModelIdRegistry(metamodel, this.modelIdProvider);

        const graph = GGraph.builder().id("metamodel-graph").addCssClass("editor-metamodel").build();

        const { classes, associations } = this.extractClassesAndAssociations(metamodel);
        this.createClassNodes(graph, classes, idRegistry);
        this.createInheritanceEdges(graph, classes, idRegistry);
        this.createAssociationEdges(graph, associations, idRegistry);

        this.modelState.updateRoot(graph);
    }

    /**
     * Extracts and separates classes and associations from the metamodel.
     *
     * @param metamodel The metamodel containing classes and associations
     * @returns An object containing separate arrays of classes and associations
     */
    private extractClassesAndAssociations(metamodel: MetaModelType): {
        classes: ClassType[];
        associations: AssociationType[];
    } {
        const classes: ClassType[] = [];
        const associations: AssociationType[] = [];

        for (const item of metamodel.classesAndAssociations) {
            if (item.$type === "Class") {
                classes.push(item as ClassType);
            } else if (item.$type === "Association") {
                associations.push(item as AssociationType);
            }
        }

        return { classes, associations };
    }

    /**
     * Creates visual nodes for all classes with their properties.
     * Nodes are positioned based on metadata or default grid layout.
     *
     * @param graph The graph to add nodes to
     * @param classes Array of classes to create nodes for
     * @param idRegistry The ID registry for AST node ID generation
     * @returns A map from node ID to created node
     */
    private createClassNodes(graph: GGraphType, classes: ClassType[], idRegistry: ModelIdRegistry): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const cls of classes) {
            const nodeId = idRegistry.getId(cls);
            if (!nodeId) {
                continue;
            }
            const metadata = validatedMetadata.nodes[nodeId];

            const node = ClassNode.builder()
                .id(nodeId)
                .name(cls.name)
                .isAbstract(cls.isAbstract)
                .layout("vbox")
                .build();

            this.applyNodePosition(node, metadata);
            this.addNodeLabels(node, nodeId, cls, idRegistry);

            graph.children.push(node);
        }
    }

    /**
     * Applies position to a node from metadata or uses default grid layout.
     *
     * @param node The node to position
     * @param metadata Metadata containing position information
     */
    private applyNodePosition(node: ClassNode, metadata: { meta?: object } | undefined): void {
        if (metadata?.meta && NodeLayoutMetadata.isValid(metadata.meta)) {
            node.position = metadata.meta.position;
            if (metadata.meta.preferredWidth !== undefined) {
                node.size = { width: metadata.meta.preferredWidth, height: node.size?.height ?? 0 };
            }
        } else {
            node.position = { x: 0, y: 0 };
        }
    }

    /**
     * Adds label children to a class node for the class name and properties.
     *
     * @param node The node to add labels to
     * @param nodeId The ID of the node for creating label IDs
     * @param cls The class containing the data to display
     * @param idRegistry The ID registry for AST node ID generation
     */
    private addNodeLabels(node: ClassNode, nodeId: string, cls: ClassType, idRegistry: ModelIdRegistry): void {
        // Create title compartment with class name
        const titleCompartment = ClassCompartment.builder().id(`${nodeId}#title-compartment`).build();

        const nameLabel = ClassLabel.builder().id(`${nodeId}#name`).text(cls.name).build();
        titleCompartment.children.push(nameLabel);
        node.children.push(titleCompartment);

        // Add divider and properties compartment only if there are properties
        if (cls.properties.length > 0) {
            // Add divider
            const divider = ClassDivider.builder().id(`${nodeId}#divider`).build();
            node.children.push(divider);

            // Create properties compartment
            const propertiesCompartment = ClassCompartment.builder().id(`${nodeId}#properties-compartment`).build();

            for (const prop of cls.properties) {
                const propId = idRegistry.getId(prop);
                if (!propId) continue;

                const multiplicityStr = this.formatMultiplicity(prop.multiplicity);
                const propText = `${prop.name}: ${prop.type.name}${multiplicityStr}`;

                const propLabel = PropertyLabel.builder().id(`${propId}#label`).text(propText).build();
                propertiesCompartment.children.push(propLabel);
            }

            node.children.push(propertiesCompartment);
        }
    }

    /**
     * Creates inheritance edges for all extends relationships.
     *
     * @param graph The graph to add edges to
     * @param classes Array of classes with extends relationships
     * @param idRegistry The ID registry for AST node ID generation
     */
    private createInheritanceEdges(graph: GGraphType, classes: ClassType[], idRegistry: ModelIdRegistry): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const cls of classes) {
            for (const superClass of cls.extends) {
                const superClassRef = superClass.ref;
                if (superClassRef) {
                    const sourceId = idRegistry.getId(cls);
                    const targetId = idRegistry.getId(superClassRef);
                    if (!sourceId || !targetId) continue;

                    const edgeId = `${sourceId}#extends#${targetId}`;
                    const edge = this.createInheritanceEdge(edgeId, sourceId, targetId, validatedMetadata);
                    graph.children.push(edge);
                }
            }
        }
    }

    /**
     * Creates a single inheritance edge with routing points from metadata if available.
     *
     * @param edgeId The ID for the edge
     * @param sourceId The source node ID
     * @param targetId The target node ID
     * @param validatedMetadata The validated metadata containing edge information
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
     * @param graph The graph to add edges to
     * @param associations Array of associations to create edges for
     * @param idRegistry The ID registry for AST node ID generation
     */
    private createAssociationEdges(
        graph: GGraphType,
        associations: AssociationType[],
        idRegistry: ModelIdRegistry
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const assoc of associations) {
            const startClassRef = assoc.start.class.ref;
            const targetClassRef = assoc.target.class.ref;

            if (startClassRef && targetClassRef) {
                const startId = idRegistry.getId(startClassRef);
                const targetId = idRegistry.getId(targetClassRef);
                if (!startId || !targetId) continue;

                const edgeId = idRegistry.getId(assoc);
                if (!edgeId) continue;

                const edge = this.createAssociationEdge(
                    edgeId,
                    assoc,
                    startId,
                    targetId,
                    validatedMetadata,
                    idRegistry
                );
                graph.children.push(edge);
            }
        }
    }

    /**
     * Creates a single association edge with all properties.
     *
     * @param edgeId The ID for the edge
     * @param assoc The association definition
     * @param sourceId The source node ID
     * @param targetId The target node ID
     * @param validatedMetadata The validated metadata containing edge information
     * @param idRegistry The ID registry for AST node ID generation
     * @returns The created edge
     */
    private createAssociationEdge(
        edgeId: string,
        assoc: AssociationType,
        sourceId: string,
        targetId: string,
        validatedMetadata: GraphMetadata,
        idRegistry: ModelIdRegistry
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
            `${edgeId}#start`,
            assoc.start.property,
            assoc.start.multiplicity,
            validatedMetadata,
            0.2,
            idRegistry
        );
        if (startLabel) {
            edge.children.push(startLabel);
        }

        const targetLabel = this.createAssociationEndLabel(
            `${edgeId}#target`,
            assoc.target.property,
            assoc.target.multiplicity,
            validatedMetadata,
            0.8,
            idRegistry
        );
        if (targetLabel) {
            edge.children.push(targetLabel);
        }

        return edge;
    }

    /**
     * Creates a label for an association endpoint with property name and multiplicity.
     *
     * @param labelId The ID for the label
     * @param property The property name (optional)
     * @param multiplicity The multiplicity (optional)
     * @param validatedMetadata The validated metadata containing label placement
     * @param defaultPosition Default position along edge if no metadata
     * @param idRegistry The ID registry for AST node ID generation
     * @returns The created label or undefined if no property or multiplicity
     */
    private createAssociationEndLabel(
        labelId: string,
        property: string | undefined,
        multiplicity: MultiplicityType | undefined,
        validatedMetadata: GraphMetadata,
        defaultPosition: number,
        idRegistry: ModelIdRegistry
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
     * @param edge The edge to apply routing points to
     * @param metadata Metadata containing routing points
     */
    private applyRoutingPoints(edge: InheritanceEdge | AssociationEdge, metadata: { meta?: object } | undefined): void {
        if (metadata?.meta && EdgeVisualMetadata.isValid(metadata.meta)) {
            edge.routingPoints = metadata.meta.routingPoints;
        }
    }

    /**
     * Formats multiplicity for display in labels.
     * Handles both single multiplicities (*, +, ?, or numeric) and range multiplicities.
     *
     * @param multiplicity The multiplicity to format
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

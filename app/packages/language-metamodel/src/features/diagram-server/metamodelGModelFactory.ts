import type { GModelElement, GModelRoot } from "@eclipse-glsp/server";
import { sharedImport, BaseGModelFactory } from "@mdeo/language-shared";
import type { GraphMetadata, ModelIdRegistry } from "@mdeo/language-shared";
import type { NodeLayoutMetadata } from "@mdeo/editor-protocol";
import type { PartialAstNode } from "@mdeo/language-common";
import type { SingleMultiplicityType, RangeMultiplicityType } from "../../grammar/metamodelTypes.js";
import type {
    PartialMetaModel,
    PartialClass,
    PartialAssociation,
    PartialMultiplicity,
    PartialClassImport,
    PartialClassExtension
} from "../../grammar/metamodelPartialTypes.js";
import { ClassNode } from "./model/classNode.js";
import { InheritanceEdge } from "./model/inheritanceEdge.js";
import { AssociationEdge } from "./model/associationEdge.js";
import { ClassLabel } from "./model/classLabel.js";
import { PropertyLabel } from "./model/propertyLabel.js";
import { AssociationEndLabel } from "./model/associationEndLabel.js";
import { ClassCompartment } from "./model/classCompartment.js";
import { ClassDivider } from "./model/classDivider.js";
import { EdgePlacementMetadataUtil, EdgeVisualMetadataUtil } from "./metadataTypes.js";

const { injectable } = sharedImport("inversify");
const { GGraph } = sharedImport("@eclipse-glsp/server");

type GGraphType = ReturnType<typeof GGraph.builder>["proxy"];

/**
 * Factory for creating GLSP graph models from metamodel AST.
 * Transforms the metamodel source model into a visual graph representation
 * with nodes for classes and edges for relationships.
 */
@injectable()
export class MetamodelGModelFactory extends BaseGModelFactory<PartialMetaModel> {
    override createModelInternal(sourceModel: PartialMetaModel, idRegistry: ModelIdRegistry): GModelRoot {
        const graph = GGraph.builder().id("metamodel-graph").addCssClass("editor-metamodel").build();

        const { classes, associations, imports } = this.extractClassesAndAssociations(sourceModel);
        this.createClassNodes(graph, classes, idRegistry);
        this.createClassNodesFromImports(graph, imports, idRegistry);
        this.createInheritanceEdges(graph, classes, idRegistry);
        this.createAssociationEdges(graph, associations, idRegistry);

        return graph;
    }

    /**
     * Extracts and separates classes, associations, and imports from the metamodel.
     *
     * @param metamodel The metamodel containing classes, associations, and imports
     * @returns An object containing separate arrays of classes, associations, and imports
     */
    private extractClassesAndAssociations(metamodel: PartialMetaModel): {
        classes: PartialClass[];
        associations: PartialAssociation[];
        imports: PartialClassImport[];
    } {
        const classes: PartialClass[] = [];
        const associations: PartialAssociation[] = [];
        const imports: PartialClassImport[] = [];

        const items = metamodel.classesAndAssociations ?? [];
        for (const item of items) {
            if (item?.$type === "Class") {
                classes.push(item as PartialClass);
            } else if (item?.$type === "Association") {
                associations.push(item as PartialAssociation);
            }
        }

        const fileImports = metamodel.imports ?? [];
        for (const fileImport of fileImports) {
            const classImports = fileImport?.imports ?? [];
            for (const classImport of classImports) {
                if (classImport != undefined) {
                    imports.push(classImport as PartialClassImport);
                }
            }
        }

        return { classes, associations, imports };
    }

    /**
     * Creates visual nodes for all classes with their properties.
     * Nodes are positioned based on metadata or default grid layout.
     *
     * @param graph The graph to add nodes to
     * @param classes Array of classes to create nodes for
     * @param idRegistry The ID registry for AST node ID generation
     */
    private createClassNodes(graph: GGraphType, classes: PartialClass[], idRegistry: ModelIdRegistry): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const cls of classes) {
            if (cls == undefined) {
                continue;
            }

            const nodeId = idRegistry.getId(cls);
            const metadata = validatedMetadata.nodes[nodeId].meta as NodeLayoutMetadata;
            const displayName = cls.name ?? "Unnamed";

            const node = ClassNode.builder()
                .id(nodeId)
                .name(cls.name ?? "Unnamed")
                .isAbstract(cls.isAbstract ?? false)
                .meta(metadata)
                .build();

            node.children.push(
                ...this.createClassTitle(nodeId, displayName, false),
                ...this.createClassProperties(nodeId, cls, idRegistry, false)
            );

            graph.children.push(node);
        }
    }

    /**
     * Creates visual nodes for imported classes.
     * Similar to createClassNodes but for ClassImport nodes with readonly labels.
     *
     * @param graph The graph to add nodes to
     * @param imports Array of class imports to create nodes for
     * @param idRegistry The ID registry for AST node ID generation
     */
    private createClassNodesFromImports(
        graph: GGraphType,
        imports: PartialClassImport[],
        idRegistry: ModelIdRegistry
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const classImport of imports) {
            if (classImport == undefined) {
                continue;
            }

            const importedClass = classImport.entity?.ref;
            if (importedClass == undefined) {
                continue;
            }

            const nodeId = idRegistry.getId(classImport);
            const metadata = validatedMetadata.nodes[nodeId].meta as NodeLayoutMetadata;
            const displayName = classImport.name ?? importedClass.name ?? "Unnamed";

            const node = ClassNode.builder()
                .id(nodeId)
                .name(displayName)
                .isAbstract(importedClass.isAbstract ?? false)
                .meta(metadata)
                .build();

            node.children.push(
                ...this.createClassTitle(nodeId, displayName, classImport.name == undefined),
                ...this.createClassProperties(nodeId, importedClass, idRegistry, true)
            );

            graph.children.push(node);
        }
    }

    /**
     * Creates the title compartment containing the class name label.
     *
     * @param nodeId The ID of the class node
     * @param name The name of the class
     * @param readonly Whether the label should be readonly
     * @returns An array with the title compartment GModelElement
     */
    private createClassTitle(nodeId: string, name: string | undefined, readonly: boolean): GModelElement[] {
        const titleCompartment = ClassCompartment.builder().id(`${nodeId}#title-compartment`).build();

        const nameLabel = ClassLabel.builder()
            .id(`${nodeId}#name`)
            .text(name ?? "Unnamed")
            .readonly(readonly)
            .build();

        titleCompartment.children.push(nameLabel);
        return [titleCompartment];
    }

    /**
     * Creates the properties compartment (and divider) for a class node.
     *
     * @param nodeId The ID of the class node
     * @param cls The class containing the properties
     * @param idRegistry The ID registry for AST node ID generation
     * @param readonly Whether the property labels should be readonly
     * @returns An array of GModelElements representing the properties compartment
     */
    private createClassProperties(
        nodeId: string,
        cls: PartialClass,
        idRegistry: ModelIdRegistry,
        readonly: boolean
    ): GModelElement[] {
        const children: GModelElement[] = [];
        const properties = cls.properties ?? [];
        if (properties.length === 0) {
            return children;
        }

        const divider = ClassDivider.builder().id(`${nodeId}#divider`).build();
        children.push(divider);

        const propertiesCompartment = ClassCompartment.builder().id(`${nodeId}#properties-compartment`).build();

        for (const prop of properties) {
            if (prop == undefined) {
                continue;
            }

            const propId = readonly ? idRegistry.getIdOrUnresolved(prop) : idRegistry.getId(prop);

            const multiplicityStr = this.formatMultiplicity(prop.multiplicity);
            const propName = prop.name ?? "unnamed";
            const typeName = prop.type?.name ?? "unknown";
            const propText = `${propName}: ${typeName}${multiplicityStr}`;

            const propLabel = PropertyLabel.builder().id(`${propId}#label`).text(propText).readonly(readonly).build();
            propertiesCompartment.children.push(propLabel);
        }

        children.push(propertiesCompartment);
        return children;
    }

    /**
     * Creates inheritance edges for all extends relationships.
     *
     * @param graph The graph to add edges to
     * @param classes Array of classes with extends relationships
     * @param idRegistry The ID registry for AST node ID generation
     */
    private createInheritanceEdges(graph: GGraphType, classes: PartialClass[], idRegistry: ModelIdRegistry): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const cls of classes) {
            if (cls == undefined) {
                continue;
            }

            const extendsRefs = cls.extends ?? [];
            for (const extendsDef of extendsRefs) {
                if (extendsDef == undefined) {
                    continue;
                }

                const superClassRef = extendsDef.class?.ref;
                if (superClassRef == undefined) {
                    continue;
                }

                const edge = this.createInheritanceEdge(extendsDef, cls, superClassRef, validatedMetadata, idRegistry);
                graph.children.push(edge);
            }
        }
    }

    /**
     * Creates a single inheritance edge with routing points from metadata.
     *
     * @param inheritance The inheritance definition
     * @param sourceClass The source class AST node
     * @param targetClass The target class AST node
     * @param validatedMetadata The validated metadata containing edge information
     * @param idRegistry The ID registry for AST node ID generation
     * @returns The created edge
     */
    private createInheritanceEdge(
        inheritance: PartialClassExtension,
        sourceClass: PartialClass,
        targetClass: PartialClass,
        validatedMetadata: GraphMetadata,
        idRegistry: ModelIdRegistry
    ): InheritanceEdge {
        const edgeId = idRegistry.getId(inheritance);
        const sourceId = idRegistry.getId(sourceClass);
        const targetId = idRegistry.getId(targetClass);
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
        associations: PartialAssociation[],
        idRegistry: ModelIdRegistry
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const assoc of associations) {
            if (assoc == undefined || assoc.start == undefined || assoc.target == undefined) {
                continue;
            }

            const startClassRef = assoc.start.class?.ref;
            const targetClassRef = assoc.target.class?.ref;

            if (startClassRef == undefined || targetClassRef == undefined) {
                continue;
            }

            const edge = this.createAssociationEdge(
                assoc,
                startClassRef,
                targetClassRef,
                validatedMetadata,
                idRegistry
            );
            graph.children.push(edge);
        }
    }

    /**
     * Creates a single association edge with all properties.
     *
     * @param assoc The association definition
     * @param sourceClass The source class AST node
     * @param targetClass The target class AST node
     * @param validatedMetadata The validated metadata containing edge information
     * @param idRegistry The ID registry for AST node ID generation
     * @returns The created edge
     */
    private createAssociationEdge(
        assoc: PartialAssociation,
        sourceClass: PartialClass,
        targetClass: PartialClass,
        validatedMetadata: GraphMetadata,
        idRegistry: ModelIdRegistry
    ): AssociationEdge {
        const edgeId = idRegistry.getId(assoc);
        const sourceId = idRegistry.getId(sourceClass);
        const targetId = idRegistry.getId(targetClass);
        const metadata = validatedMetadata.edges[edgeId];
        const edge = AssociationEdge.builder()
            .id(edgeId)
            .sourceId(sourceId)
            .targetId(targetId)
            .operator(assoc.operator ?? "--")
            .build();

        this.applyRoutingPoints(edge, metadata);

        if (assoc.start != undefined) {
            const startLabel = this.createAssociationEndLabel(
                `${edgeId}#start`,
                assoc.start.property,
                assoc.start.multiplicity,
                validatedMetadata,
                0.2
            );
            if (startLabel != undefined) {
                edge.children.push(startLabel);
            }
        }

        if (assoc.target != undefined) {
            const targetLabel = this.createAssociationEndLabel(
                `${edgeId}#target`,
                assoc.target.property,
                assoc.target.multiplicity,
                validatedMetadata,
                0.8
            );
            if (targetLabel != undefined) {
                edge.children.push(targetLabel);
            }
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
     * @returns The created label or undefined if no property or multiplicity
     */
    private createAssociationEndLabel(
        labelId: string,
        property: string | undefined,
        multiplicity: PartialMultiplicity | undefined,
        validatedMetadata: GraphMetadata,
        defaultPosition: number
    ): AssociationEndLabel | undefined {
        const multiplicityStr = this.formatMultiplicity(multiplicity);

        if (property == undefined && multiplicityStr === "") {
            return undefined;
        }

        const text = property && multiplicityStr ? `${property} ${multiplicityStr}` : property || multiplicityStr;

        const label = AssociationEndLabel.builder().id(labelId).text(text).build();

        const labelMeta = validatedMetadata.nodes[labelId];
        if (
            labelMeta != undefined &&
            labelMeta.meta != undefined &&
            EdgePlacementMetadataUtil.isValid(labelMeta.meta)
        ) {
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
        if (metadata != undefined && metadata.meta != undefined && EdgeVisualMetadataUtil.isValid(metadata.meta)) {
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
    private formatMultiplicity(multiplicity: PartialMultiplicity | undefined): string {
        if (multiplicity == undefined) {
            return "";
        }

        if (multiplicity.$type === "SingleMultiplicity") {
            const single = multiplicity as PartialAstNode<SingleMultiplicityType>;
            if (single.value != undefined) {
                return `[${single.value}]`;
            }
            if (single.numericValue !== undefined && single.numericValue !== null) {
                return `[${single.numericValue}]`;
            }
        } else if (multiplicity.$type === "RangeMultiplicity") {
            const range = multiplicity as PartialAstNode<RangeMultiplicityType>;
            const lower = range.lower ?? 0;
            const upper = range.upper ?? range.upperNumeric ?? "*";
            return `[${lower}..${upper}]`;
        }

        return "";
    }
}

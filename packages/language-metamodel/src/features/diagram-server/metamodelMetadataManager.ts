import {
    sharedImport,
    MetadataManager,
    type GraphMetadata,
    type NodeMetadata,
    type EdgeMetadata,
    NodeLayoutMetadata,
    EdgeVisualMetadata,
    EdgePlacementMetadata,
    ModelIdRegistry,
    ModelIdProvider,
    type ModelIdProvider as ModelIdProviderType
} from "@mdeo/language-shared";
import type { NodeAttributes, EdgeAttributes } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import type { MetaModelType, ClassType, AssociationType } from "../../grammar/metamodelTypes.js";
import { MetamodelElementType } from "./model/elementTypes.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Manages metadata validation and synchronization for metamodel diagrams.
 * Implements cost calculations based on semantic similarity between model elements.
 */
@injectable()
export class MetamodelMetadataManager extends MetadataManager<MetaModelType> {
    @inject(ModelIdProvider)
    protected modelIdProvider!: ModelIdProviderType;

    /**
     * Verifies the metadata for a given model element.
     * Corrects invalid metadata or returns undefined if valid.
     */
    protected verifyMetadata(model: NodeMetadata | EdgeMetadata): object | undefined {
        if (model.type === MetamodelElementType.NODE_CLASS) {
            return NodeLayoutMetadata.verify(model.meta, 0, 0);
        }

        if (model.type === MetamodelElementType.LABEL_ASSOCIATION_END) {
            return EdgePlacementMetadata.verify(model.meta, 0.5);
        }

        if (
            model.type === MetamodelElementType.EDGE_INHERITANCE ||
            model.type === MetamodelElementType.EDGE_ASSOCIATION
        ) {
            const edgeModel = model as EdgeMetadata;
            return EdgeVisualMetadata.verify(edgeModel.meta);
        }

        return undefined;
    }

    /**
     * Calculate the cost of transforming one node to another.
     * Insertion/deletion: cost = 1
     * Substitution: cost = 1 + similarity (0-1)
     */
    protected calculateNodeCost(node1: NodeAttributes | undefined, node2: NodeAttributes | undefined): number {
        if (node1 === undefined || node2 === undefined) {
            return 1;
        }

        const type1 = node1.type as string;
        const type2 = node2.type as string;

        if (type1 !== type2) {
            return 2;
        }

        if (type1 === MetamodelElementType.NODE_CLASS) {
            const similarity = this.calculateClassSimilarity(node1, node2);
            return 1 + (1 - similarity);
        }

        if (type1.startsWith("label:")) {
            return 1;
        }

        return 1;
    }

    /**
     * Calculate the cost of transforming one edge to another.
     * Insertion/deletion: cost = 1
     * Substitution: cost = 1 + similarity (0-1)
     */
    protected calculateEdgeCost(edge1: EdgeAttributes | undefined, edge2: EdgeAttributes | undefined): number {
        if (edge1 === undefined || edge2 === undefined) {
            return 1;
        }

        const type1 = edge1.type as string;
        const type2 = edge2.type as string;

        if (type1 !== type2) {
            return 2;
        }

        if (type1 === MetamodelElementType.EDGE_INHERITANCE) {
            return 1;
        }

        if (type1 === MetamodelElementType.EDGE_ASSOCIATION) {
            const similarity = this.calculateAssociationSimilarity(edge1, edge2);
            return 1 + (1 - similarity);
        }

        return 1;
    }

    /**
     * Extracts graph metadata from the metamodel source model.
     */
    protected extractGraphMetadata(sourceModel: MetaModelType): GraphMetadata {
        const nodes: Record<string, NodeMetadata> = {};
        const edges: Record<string, EdgeMetadata> = {};

        const idRegistry = new ModelIdRegistry(sourceModel, this.modelIdProvider);
        const { classes, associations } = this.extractClassesAndAssociations(sourceModel);

        this.extractClassMetadata(classes, idRegistry, nodes);
        this.extractInheritanceMetadata(classes, idRegistry, edges);
        this.extractAssociationMetadata(associations, idRegistry, nodes, edges);

        return { nodes, edges };
    }

    /**
     * Extracts metadata for all classes.
     */
    private extractClassMetadata(
        classes: ClassType[],
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>
    ): void {
        for (const cls of classes) {
            const nodeId = idRegistry.getId(cls);
            if (nodeId) {
                nodes[nodeId] = {
                    type: MetamodelElementType.NODE_CLASS,
                    attrs: this.createClassAttributes(cls)
                };
            }
        }
    }

    /**
     * Extracts metadata for all inheritance relationships.
     */
    private extractInheritanceMetadata(
        classes: ClassType[],
        idRegistry: ModelIdRegistry,
        edges: Record<string, EdgeMetadata>
    ): void {
        let inheritanceIndex = 0;

        for (const cls of classes) {
            for (const superClassRef of cls.extends) {
                const superClass = superClassRef.ref;
                if (superClass && superClass.$type === "Class") {
                    const sourceId = idRegistry.getId(cls);
                    const targetId = idRegistry.getId(superClass as ClassType);

                    if (sourceId && targetId) {
                        const edgeId = `inheritance_${inheritanceIndex++}`;
                        edges[edgeId] = {
                            type: MetamodelElementType.EDGE_INHERITANCE,
                            from: sourceId,
                            to: targetId,
                            attrs: {}
                        };
                    }
                }
            }
        }
    }

    /**
     * Extracts metadata for all associations.
     */
    private extractAssociationMetadata(
        associations: AssociationType[],
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): void {
        let associationIndex = 0;

        for (const assoc of associations) {
            const startClass = assoc.start.class.ref;
            const targetClass = assoc.target.class.ref;

            if (startClass && targetClass) {
                const startId = this.getClassId(startClass, idRegistry);
                const targetId = this.getClassId(targetClass, idRegistry);

                if (startId && targetId) {
                    const edgeId = `association_${associationIndex++}`;
                    edges[edgeId] = {
                        type: MetamodelElementType.EDGE_ASSOCIATION,
                        from: startId,
                        to: targetId,
                        attrs: this.createAssociationAttributes(assoc)
                    };

                    this.extractAssociationLabelMetadata(assoc, edgeId, nodes);
                }
            }
        }
    }

    /**
     * Extracts metadata for association end labels.
     */
    private extractAssociationLabelMetadata(
        assoc: AssociationType,
        edgeId: string,
        nodes: Record<string, NodeMetadata>
    ): void {
        if (assoc.start.property || assoc.start.multiplicity) {
            nodes[`${edgeId}_start`] = {
                type: MetamodelElementType.LABEL_ASSOCIATION_END,
                attrs: {}
            };
        }

        if (assoc.target.property || assoc.target.multiplicity) {
            nodes[`${edgeId}_target`] = {
                type: MetamodelElementType.LABEL_ASSOCIATION_END,
                attrs: {}
            };
        }
    }

    /**
     * Extracts classes and associations from the metamodel.
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
     * Gets the ID for a class (handles both Class and ClassImport).
     */
    private getClassId(classNode: AstNode, idRegistry: ModelIdRegistry): string | undefined {
        if (classNode.$type === "Class") {
            return idRegistry.getId(classNode);
        }

        if (classNode.$type === "ClassImport") {
            const importNode = classNode as any;
            const referencedClass = importNode.element?.ref;
            if (referencedClass) {
                return idRegistry.getId(referencedClass);
            }
        }

        return undefined;
    }

    /**
     * Creates node attributes for a class.
     * Includes property names for similarity comparison.
     */
    private createClassAttributes(cls: ClassType): NodeAttributes {
        return {
            type: MetamodelElementType.NODE_CLASS,
            name: cls.name,
            isAbstract: cls.isAbstract,
            properties: cls.properties.map((p) => p.name)
        };
    }

    /**
     * Creates edge attributes for an association.
     * Includes property names for similarity comparison.
     */
    private createAssociationAttributes(assoc: AssociationType): EdgeAttributes {
        return {
            type: MetamodelElementType.EDGE_ASSOCIATION,
            operator: assoc.operator,
            startProperty: assoc.start.property || "",
            targetProperty: assoc.target.property || ""
        };
    }

    /**
     * Calculates similarity between two classes based on shared properties.
     * Returns a value between 0 (no similarity) and 1 (identical).
     * This function is symmetric.
     */
    private calculateClassSimilarity(node1: NodeAttributes, node2: NodeAttributes): number {
        const props1 = (node1.properties as string[]) || [];
        const props2 = (node2.properties as string[]) || [];

        if (props1.length === 0 && props2.length === 0) {
            return 1;
        }

        const maxProps = Math.max(props1.length, props2.length);
        if (maxProps === 0) {
            return 1;
        }

        const props1Set = new Set(props1);
        const sharedCount = props2.filter((p) => props1Set.has(p)).length;

        return sharedCount / maxProps;
    }

    /**
     * Calculates similarity between two associations based on property names.
     * Returns a value between 0 (no similarity) and 1 (identical).
     * This function is symmetric.
     */
    private calculateAssociationSimilarity(edge1: EdgeAttributes, edge2: EdgeAttributes): number {
        const start1 = (edge1.startProperty as string) || "";
        const target1 = (edge1.targetProperty as string) || "";
        const start2 = (edge2.startProperty as string) || "";
        const target2 = (edge2.targetProperty as string) || "";

        let matches = 0;
        let total = 0;

        if (start1 || start2) {
            total++;
            if (start1 === start2) {
                matches++;
            }
        }

        if (target1 || target2) {
            total++;
            if (target1 === target2) {
                matches++;
            }
        }

        if (total === 0) {
            return 0.5;
        }

        return matches / total;
    }
}

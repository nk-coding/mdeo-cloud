import {
    sharedImport,
    MetadataManager,
    type GraphMetadata,
    type NodeMetadata,
    type EdgeMetadata,
    ModelIdRegistry,
    ModelIdProvider,
    type ModelIdProvider as ModelIdProviderType
} from "@mdeo/language-shared";
import type { NodeAttributes, EdgeAttributes, Attributes } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import { MetamodelElementType } from "./model/elementTypes.js";
import type {
    PartialClassImport,
    PartialMetaModel,
    PartialClass,
    PartialAssociation
} from "../../grammar/metamodelPartialTypes.js";
import { Association, Class, ClassImport } from "../../grammar/metamodelTypes.js";
import { EdgePlacementMetadataUtil, EdgeVisualMetadataUtil, NodeLayoutMetadataUtil } from "./metadataTypes.js";

const { injectable, inject } = sharedImport("inversify");

/**
 * Manages metadata validation and synchronization for metamodel diagrams.
 * Implements cost calculations based on semantic similarity between model elements.
 */
@injectable()
export class MetamodelMetadataManager extends MetadataManager<PartialMetaModel> {
    @inject(ModelIdProvider)
    protected modelIdProvider!: ModelIdProviderType;

    protected override verifyMetadata(model: NodeMetadata | EdgeMetadata): object | undefined {
        if (model.type === MetamodelElementType.NODE_CLASS) {
            return NodeLayoutMetadataUtil.verify(model.meta, 250);
        }

        if (model.type === MetamodelElementType.LABEL_ASSOCIATION_END) {
            return EdgePlacementMetadataUtil.verify(model.meta, 0.5);
        }

        if (
            model.type === MetamodelElementType.EDGE_INHERITANCE ||
            model.type === MetamodelElementType.EDGE_ASSOCIATION
        ) {
            const edgeModel = model as EdgeMetadata;
            return EdgeVisualMetadataUtil.verify(edgeModel.meta);
        }

        return undefined;
    }

    /**
     * Calculate the cost of transforming one node to another.
     * Insertion/deletion: cost = 1
     * Substitution: cost = 1 + similarity (0-1)
     *
     * @param node1 first NodeAttributes
     * @param node2 second NodeAttributes
     * @returns cost of transformation
     */
    protected calculateNodeCost(node1: NodeAttributes | undefined, node2: NodeAttributes | undefined): number {
        if (node1 == undefined || node2 == undefined) {
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
     *
     * @param edge1 first EdgeAttributes
     * @param edge2 second EdgeAttributes
     * @returns cost of transformation
     */
    protected calculateEdgeCost(edge1: EdgeAttributes | undefined, edge2: EdgeAttributes | undefined): number {
        if (edge1 == undefined || edge2 == undefined) {
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
     *
     * @param sourceModel the metamodel source model
     * @returns extracted graph metadata
     */
    protected extractGraphMetadata(sourceModel: PartialMetaModel): GraphMetadata {
        const nodes: Record<string, NodeMetadata> = {};
        const edges: Record<string, EdgeMetadata> = {};

        const idRegistry = new ModelIdRegistry(sourceModel, this.modelIdProvider);
        const { classes, associations, imports } = this.extractClassesAndAssociations(sourceModel);

        this.extractClassMetadata(classes, idRegistry, nodes);
        this.extractClassImportMetadata(imports, idRegistry, nodes);
        this.extractInheritanceMetadata(classes, idRegistry, edges);
        this.extractAssociationMetadata(associations, idRegistry, nodes, edges);

        return { nodes, edges };
    }

    /**
     * Extracts metadata for all classes.
     *
     * @param classes list of classes in the metamodel
     * @param idRegistry model ID registry
     * @param nodes record to populate with node metadata
     */
    private extractClassMetadata(
        classes: PartialClass[],
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
     * Extracts metadata for all imported classes.
     *
     * @param imports list of class imports in the metamodel
     * @param idRegistry model ID registry
     * @param nodes record to populate with node metadata
     */
    private extractClassImportMetadata(
        imports: PartialClassImport[],
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>
    ): void {
        for (const classImport of imports) {
            const importedClass = classImport.entity?.ref;
            if (!importedClass) {
                continue;
            }

            const nodeId = idRegistry.getId(classImport);
            if (nodeId) {
                nodes[nodeId] = {
                    type: MetamodelElementType.NODE_CLASS,
                    attrs: this.createClassAttributes(importedClass as PartialClass)
                };
            }
        }
    }

    /**
     * Extracts metadata for all inheritance relationships.
     *
     * @param classes list of classes in the metamodel
     * @param idRegistry model ID registry
     * @param edges record to populate with edge metadata
     */
    private extractInheritanceMetadata(
        classes: PartialClass[],
        idRegistry: ModelIdRegistry,
        edges: Record<string, EdgeMetadata>
    ): void {
        for (const cls of classes) {
            if (!cls.extends) {
                continue;
            }

            for (const extendsDef of cls.extends) {
                if (extendsDef?.class?.ref == undefined) {
                    continue;
                }

                const superClass = extendsDef.class.ref;
                if (this.reflection.isInstance(superClass, Class)) {
                    const sourceId = idRegistry.getId(cls);
                    const targetId = idRegistry.getId(superClass as PartialClass);

                    if (sourceId && targetId) {
                        const edgeId = idRegistry.getId(extendsDef);
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
     *
     * @param associations list of associations in the metamodel
     * @param idRegistry model ID registry
     * @param nodes record to populate with node metadata
     * @param edges record to populate with edge metadata
     */
    private extractAssociationMetadata(
        associations: PartialAssociation[],
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>,
        edges: Record<string, EdgeMetadata>
    ): void {
        for (const assoc of associations) {
            if (assoc.start == undefined || assoc.target == undefined) {
                continue;
            }

            const startClass = assoc.start.class?.ref;
            const targetClass = assoc.target.class?.ref;

            if (startClass && targetClass) {
                const startId = this.getClassId(startClass, idRegistry);
                const targetId = this.getClassId(targetClass, idRegistry);

                if (startId && targetId) {
                    const edgeId = idRegistry.getId(assoc);
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
     *
     * @param assoc the association definition
     * @param edgeId the ID of the association edge
     * @param nodes record to populate with node metadata
     */
    private extractAssociationLabelMetadata(
        assoc: PartialAssociation,
        edgeId: string,
        nodes: Record<string, NodeMetadata>
    ): void {
        if (assoc.start != undefined && (assoc.start.property != undefined || assoc.start.multiplicity != undefined)) {
            nodes[`${edgeId}_start`] = {
                type: MetamodelElementType.LABEL_ASSOCIATION_END,
                attrs: {}
            };
        }

        if (
            assoc.target != undefined &&
            (assoc.target.property != undefined || assoc.target.multiplicity != undefined)
        ) {
            nodes[`${edgeId}_target`] = {
                type: MetamodelElementType.LABEL_ASSOCIATION_END,
                attrs: {}
            };
        }
    }

    /**
     * Extracts classes, associations, and imports from the metamodel.
     *
     * @param metamodel the metamodel source model
     * @returns extracted classes, associations, and imports
     */
    private extractClassesAndAssociations(metamodel: PartialMetaModel): {
        classes: PartialClass[];
        associations: PartialAssociation[];
        imports: PartialClassImport[];
    } {
        const classes: PartialClass[] = [];
        const associations: PartialAssociation[] = [];
        const imports: PartialClassImport[] = [];

        if (!metamodel.classesAndAssociations) {
            return { classes, associations, imports };
        }

        for (const item of metamodel.classesAndAssociations) {
            if (!item) {
                continue;
            }

            if (this.reflection.isInstance(item, Class)) {
                classes.push(item);
            } else if (this.reflection.isInstance(item, Association)) {
                associations.push(item);
            }
        }

        // Extract imports from file imports
        const fileImports = metamodel.imports ?? [];
        for (const fileImport of fileImports) {
            const classImports = fileImport?.imports ?? [];
            for (const classImport of classImports) {
                if (classImport) {
                    imports.push(classImport as PartialClassImport);
                }
            }
        }

        return { classes, associations, imports };
    }

    /**
     * Gets the ID for a class (handles both Class and ClassImport).
     *
     * @param classNode the class AST node
     * @param idRegistry model ID registry
     * @returns the class ID or undefined
     */
    private getClassId(classNode: AstNode, idRegistry: ModelIdRegistry): string | undefined {
        if (!classNode) {
            return undefined;
        }

        if (this.reflection.isInstance(classNode, Class)) {
            return idRegistry.getId(classNode);
        }

        if (this.reflection.isInstance(classNode, ClassImport)) {
            const importNode = classNode as PartialClassImport;
            const referencedClass = importNode.entity?.ref;
            if (referencedClass) {
                return idRegistry.getId(referencedClass);
            }
        }

        return undefined;
    }

    /**
     * Creates node attributes for a class.
     * Includes property names for similarity comparison.
     *
     * @param cls the class definition
     * @returns node attributes
     */
    private createClassAttributes(cls: PartialClass): Attributes {
        const properties = cls.properties?.filter((p) => p?.name).map((p) => p.name!) ?? [];

        return {
            name: cls.name ?? "",
            isAbstract: cls.isAbstract ?? false,
            properties
        };
    }

    /**
     * Creates edge attributes for an association.
     * Includes property names for similarity comparison.
     *
     * @param assoc the association definition
     * @returns edge attributes
     */
    private createAssociationAttributes(assoc: PartialAssociation): Attributes {
        return {
            operator: assoc.operator ?? "",
            startProperty: assoc.start?.property ?? "",
            targetProperty: assoc.target?.property ?? ""
        };
    }

    /**
     * Calculates similarity between two classes based on shared properties.
     * Returns a value between 0 (no similarity) and 1 (identical).
     * This function is symmetric.
     *
     * @param node1 first NodeAttributes
     * @param node2 second NodeAttributes
     * @returns similarity score
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
     *
     * @param edge1 first EdgeAttributes
     * @param edge2 second EdgeAttributes
     * @returns similarity score
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

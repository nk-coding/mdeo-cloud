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
    PartialMetaModel,
    PartialClass,
    PartialAssociation,
    PartialClassOrEnumImport,
    PartialEnum
} from "../../grammar/metamodelPartialTypes.js";
import { Association, Class, ClassOrEnumImport, Enum } from "../../grammar/metamodelTypes.js";
import { EdgeLayoutMetadataUtil, NodeLayoutMetadataUtil } from "./metadataTypes.js";

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

        if (
            model.type === MetamodelElementType.NODE_ASSOCIATION_PROPERTY ||
            model.type === MetamodelElementType.NODE_ASSOCIATION_MULTIPLICITY
        ) {
            return NodeLayoutMetadataUtil.verify(model.meta);
        }

        if (
            model.type === MetamodelElementType.EDGE_INHERITANCE ||
            model.type === MetamodelElementType.EDGE_ASSOCIATION
        ) {
            const edgeModel = model as EdgeMetadata;
            return EdgeLayoutMetadataUtil.verify(edgeModel.meta);
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
        const { classes, enums, associations, imports } = this.extractClassesAndAssociations(sourceModel);

        this.extractClassMetadata(classes, idRegistry, nodes);
        this.extractEnumMetadata(enums, idRegistry, nodes);
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
     * Extracts metadata for all enums.
     *
     * @param enums list of enums in the metamodel
     * @param idRegistry model ID registry
     * @param nodes record to populate with node metadata
     */
    private extractEnumMetadata(
        enums: PartialEnum[],
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>
    ): void {
        for (const enumeration of enums) {
            const nodeId = idRegistry.getId(enumeration);
            if (nodeId) {
                nodes[nodeId] = {
                    type: MetamodelElementType.NODE_ENUM,
                    attrs: this.createEnumAttributes(enumeration)
                };
            }
        }
    }

    /**
     * Extracts metadata for all imported classes or enums.
     *
     * @param imports list of class or enum imports in the metamodel
     * @param idRegistry model ID registry
     * @param nodes record to populate with node metadata
     */
    private extractClassImportMetadata(
        imports: PartialClassOrEnumImport[],
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
            const extensions = cls.extensions?.extensions ?? [];
            if (extensions.length === 0) {
                continue;
            }

            for (const extendsDef of extensions) {
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
            if (assoc.source == undefined || assoc.target == undefined) {
                continue;
            }

            const startClass = assoc.source.class?.ref;
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

                    this.extractAssociationLabelMetadata(assoc, idRegistry, nodes);
                }
            }
        }
    }

    /**
     * Extracts metadata for association end labels.
     *
     * @param assoc the association definition
     * @param idRegistry model ID registry
     * @param nodes record to populate with node metadata
     */
    private extractAssociationLabelMetadata(
        assoc: PartialAssociation,
        idRegistry: ModelIdRegistry,
        nodes: Record<string, NodeMetadata>
    ): void {
        if (assoc.source != undefined) {
            const startId = idRegistry.getId(assoc.source);
            if (assoc.source.name != undefined) {
                nodes[`${startId}#property`] = {
                    type: MetamodelElementType.NODE_ASSOCIATION_PROPERTY,
                    attrs: {}
                };
            }
            if (assoc.source.multiplicity != undefined) {
                nodes[`${startId}#multiplicity`] = {
                    type: MetamodelElementType.NODE_ASSOCIATION_MULTIPLICITY,
                    attrs: {}
                };
            }
        }

        if (assoc.target != undefined) {
            const targetId = idRegistry.getId(assoc.target);
            if (assoc.target.name != undefined) {
                nodes[`${targetId}#property`] = {
                    type: MetamodelElementType.NODE_ASSOCIATION_PROPERTY,
                    attrs: {}
                };
            }
            if (assoc.target.multiplicity != undefined) {
                nodes[`${targetId}#multiplicity`] = {
                    type: MetamodelElementType.NODE_ASSOCIATION_MULTIPLICITY,
                    attrs: {}
                };
            }
        }
    }

    /**
     * Extracts classes, associations, and imports from the metamodel.
     *
     * @param metamodel the metamodel source model
     * @returns extracted classes, enums,  associations, and imports
     */
    private extractClassesAndAssociations(metamodel: PartialMetaModel): {
        classes: PartialClass[];
        enums: PartialEnum[];
        associations: PartialAssociation[];
        imports: PartialClassOrEnumImport[];
    } {
        const classes: PartialClass[] = [];
        const enums: PartialEnum[] = [];
        const associations: PartialAssociation[] = [];
        const imports: PartialClassOrEnumImport[] = [];

        if (!metamodel.elements) {
            return { classes, enums, associations, imports };
        }

        for (const item of metamodel.elements) {
            if (!item) {
                continue;
            }

            if (this.reflection.isInstance(item, Class)) {
                classes.push(item);
            } else if (this.reflection.isInstance(item, Enum)) {
                enums.push(item);
            } else if (this.reflection.isInstance(item, Association)) {
                associations.push(item);
            }
        }

        const fileImports = metamodel.imports ?? [];
        for (const fileImport of fileImports) {
            for (const imp of fileImport?.imports ?? []) {
                if (imp) {
                    imports.push(imp as PartialClassOrEnumImport);
                }
            }
        }

        return { classes, enums, associations, imports };
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

        if (this.reflection.isInstance(classNode, ClassOrEnumImport)) {
            const importNode = classNode as PartialClassOrEnumImport;
            const referencedClass = importNode.entity?.ref;
            if (this.reflection.isInstance(referencedClass, Class)) {
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
     * Creates enum attributes for an enumeration.
     * Includes entry names for similarity comparison.
     *
     * @param enumeration the enum definition
     * @returns enum attributes
     */
    private createEnumAttributes(enumeration: PartialEnum): Attributes {
        const entries = enumeration.entries?.filter((e) => e?.name).map((e) => e.name!) ?? [];

        return {
            name: enumeration.name ?? "",
            entries
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
            startProperty: assoc.source?.name ?? "",
            targetProperty: assoc.target?.name ?? ""
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

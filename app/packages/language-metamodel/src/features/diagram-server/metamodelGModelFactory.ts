import type { GModelElement, GModelRoot } from "@eclipse-glsp/server";
import { sharedImport, BaseGModelFactory, GCompartment, GHorizontalDivider } from "@mdeo/language-shared";
import type { GraphMetadata, ModelIdRegistry } from "@mdeo/language-shared";
import type { NodeLayoutMetadata } from "@mdeo/editor-protocol";
import { ID, type PartialAstNode } from "@mdeo/language-common";
import type { SingleMultiplicityType, RangeMultiplicityType } from "../../grammar/metamodelTypes.js";
import { Class, MetamodelAssociationOperators } from "../../grammar/metamodelTypes.js";
import type {
    PartialMetaModel,
    PartialClass,
    PartialAssociation,
    PartialMultiplicity,
    PartialClassOrEnumImport,
    PartialClassExtension,
    PartialAssociationEnd,
    PartialEnum
} from "../../grammar/metamodelPartialTypes.js";
import { GClassNode } from "./model/classNode.js";
import { GEnumNode } from "./model/enumNode.js";
import { GInheritanceEdge } from "./model/inheritanceEdge.js";
import { GAssociationEdge } from "./model/associationEdge.js";
import { GClassLabel } from "./model/classLabel.js";
import { GEnumLabel } from "./model/enumLabel.js";
import { GEnumEntryLabel } from "./model/enumEntryLabel.js";
import { GPropertyLabel } from "./model/propertyLabel.js";
import { GAssociationPropertyNode } from "./model/associationPropertyNode.js";
import { GAssociationMultiplicityNode } from "./model/associationMultiplicityNode.js";
import { GAssociationPropertyLabel } from "./model/associationPropertyLabel.js";
import { GAssociationMultiplicityLabel } from "./model/associationMultiplicityLabel.js";
import { GEnumTitleCompartment } from "./model/enumTitleCompartment.js";
import { EdgeLayoutMetadataUtil, NodeLayoutMetadataUtil } from "./metadataTypes.js";
import { AssociationEndKind, MetamodelElementType } from "./model/elementTypes.js";

const { injectable } = sharedImport("inversify");
const { GGraph } = sharedImport("@eclipse-glsp/server");

type GGraphType = ReturnType<typeof GGraph.builder>["proxy"];

/**
 * Factory for creating GLSP graph models from metamodel AST.
 * Transforms the metamodel source model into a visual graph representation
 * with nodes for classes, enums, and edges for relationships.
 */
@injectable()
export class MetamodelGModelFactory extends BaseGModelFactory<PartialMetaModel> {
    override createModelInternal(sourceModel: PartialMetaModel, idRegistry: ModelIdRegistry): GModelRoot {
        const graph = GGraph.builder().id("metamodel-graph").addCssClass("editor-metamodel").build();

        const extracted = this.extractElements(sourceModel);
        this.createClassNodes(graph, extracted.classes, idRegistry);
        this.createClassNodesFromImports(graph, extracted.imports, idRegistry);
        this.createEnumNodes(graph, extracted.enums, idRegistry);
        this.createInheritanceEdges(graph, extracted.classes, idRegistry);
        this.createAssociationEdges(graph, extracted.associations, idRegistry);

        return graph;
    }

    /**
     * Extracts and separates classes, enums, associations, and imports from the metamodel.
     *
     * @param metamodel The metamodel containing elements and imports
     * @returns An object containing separate arrays of each element type
     */
    private extractElements(metamodel: PartialMetaModel): {
        classes: PartialClass[];
        enums: PartialEnum[];
        associations: PartialAssociation[];
        imports: PartialClassOrEnumImport[];
    } {
        const classes: PartialClass[] = [];
        const enums: PartialEnum[] = [];
        const associations: PartialAssociation[] = [];
        const imports: PartialClassOrEnumImport[] = [];
        const items = metamodel.elements ?? [];
        for (const item of items) {
            if (item?.$type === "Class") {
                classes.push(item as PartialClass);
            } else if (item?.$type === "Enum") {
                enums.push(item as PartialEnum);
            } else if (item?.$type === "Association") {
                associations.push(item as PartialAssociation);
            }
        }

        const fileImports = metamodel.imports ?? [];
        for (const fileImport of fileImports) {
            const classOrEnumImports = fileImport?.imports ?? [];
            for (const classOrEnumImport of classOrEnumImports) {
                if (classOrEnumImport != undefined) {
                    imports.push(classOrEnumImport as PartialClassOrEnumImport);
                }
            }
        }

        return { classes, enums, associations, imports };
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

            const node = GClassNode.builder()
                .id(nodeId)
                .name(displayName)
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
        imports: PartialClassOrEnumImport[],
        idRegistry: ModelIdRegistry
    ): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const classOrEnumImport of imports) {
            if (classOrEnumImport == undefined) {
                continue;
            }

            const importedClassOrEnum = classOrEnumImport.entity?.ref;
            if (importedClassOrEnum == undefined) {
                continue;
            }

            const nodeId = idRegistry.getId(classOrEnumImport);
            const metadata = validatedMetadata.nodes[nodeId].meta as NodeLayoutMetadata;
            const displayName = classOrEnumImport.name ?? importedClassOrEnum.name ?? "Unnamed";

            if (this.modelState.languageServices.shared.AstReflection.isInstance(importedClassOrEnum, Class)) {
                const node = GClassNode.builder()
                    .id(nodeId)
                    .name(displayName)
                    .isAbstract(importedClassOrEnum.isAbstract ?? false)
                    .meta(metadata)
                    .build();

                node.children.push(
                    ...this.createClassTitle(nodeId, displayName, classOrEnumImport.name == undefined),
                    ...this.createClassProperties(nodeId, importedClassOrEnum, idRegistry, true)
                );

                graph.children.push(node);
            } else {
                const node = GEnumNode.builder().id(nodeId).name(displayName).meta(metadata).build();

                node.children.push(
                    ...this.createEnumTitle(nodeId, displayName, classOrEnumImport.name == undefined),
                    ...this.createEnumEntries(nodeId, importedClassOrEnum, idRegistry, true)
                );

                graph.children.push(node);
            }
        }
    }

    /**
     * Creates visual nodes for all enums with their entries.
     *
     * @param graph The graph to add nodes to
     * @param enums Array of enums to create nodes for
     * @param idRegistry The ID registry for AST node ID generation
     */
    private createEnumNodes(graph: GGraphType, enums: PartialEnum[], idRegistry: ModelIdRegistry): void {
        const validatedMetadata = this.modelState.getValidatedMetadata();

        for (const enumDef of enums) {
            if (enumDef == undefined) {
                continue;
            }

            const nodeId = idRegistry.getId(enumDef);
            const metadata = validatedMetadata.nodes[nodeId].meta as NodeLayoutMetadata;
            const displayName = enumDef.name ?? "Unnamed";

            const node = GEnumNode.builder().id(nodeId).name(displayName).meta(metadata).build();

            node.children.push(
                ...this.createEnumTitle(nodeId, displayName, false),
                ...this.createEnumEntries(nodeId, enumDef, idRegistry, false)
            );

            graph.children.push(node);
        }
    }

    /**
     * Creates the title compartment containing the enum name label.
     *
     * @param nodeId The ID of the enum node
     * @param name The name of the enum
     * @param readonly Whether the label should be readonly
     * @returns An array with the title compartment GModelElement
     */
    private createEnumTitle(nodeId: string, name: string | undefined, readonly: boolean): GModelElement[] {
        const titleCompartment = GEnumTitleCompartment.builder().id(`${nodeId}#title-compartment`).build();

        const nameLabel = GEnumLabel.builder()
            .id(`${nodeId}#name`)
            .text(name ?? "Unnamed")
            .readonly(readonly)
            .build();

        titleCompartment.children.push(nameLabel);
        return [titleCompartment];
    }

    /**
     * Creates the entries compartment (and divider) for an enum node.
     *
     * @param nodeId The ID of the enum node
     * @param enumDef The enum containing the entries
     * @param idRegistry The ID registry for AST node ID generation
     * @param readonly Whether the entry labels should be readonly
     * @returns An array of GModelElements representing the entries compartment
     */
    private createEnumEntries(
        nodeId: string,
        enumDef: PartialEnum,
        idRegistry: ModelIdRegistry,
        readonly: boolean
    ): GModelElement[] {
        const children: GModelElement[] = [];
        const entries = enumDef.entries ?? [];
        if (entries.length === 0) {
            return children;
        }

        const divider = GHorizontalDivider.builder().type(MetamodelElementType.DIVIDER).id(`${nodeId}#divider`).build();
        children.push(divider);

        const entriesCompartment = GCompartment.builder()
            .type(MetamodelElementType.COMPARTMENT)
            .id(`${nodeId}#entries-compartment`)
            .build();

        for (const entry of entries) {
            if (entry == undefined) {
                continue;
            }

            const entryId = readonly ? idRegistry.getIdOrUnresolved(entry) : idRegistry.getId(entry);
            const entryName = entry.name ?? "unnamed";

            const entryLabel = GEnumEntryLabel.builder()
                .id(`${entryId}#label`)
                .text(entryName)
                .readonly(readonly)
                .build();
            entriesCompartment.children.push(entryLabel);
        }

        children.push(entriesCompartment);
        return children;
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
        const titleCompartment = GCompartment.builder()
            .type(MetamodelElementType.COMPARTMENT)
            .id(`${nodeId}#title-compartment`)
            .build();

        const nameLabel = GClassLabel.builder()
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

        const divider = GHorizontalDivider.builder().type(MetamodelElementType.DIVIDER).id(`${nodeId}#divider`).build();
        children.push(divider);

        const propertiesCompartment = GCompartment.builder()
            .type(MetamodelElementType.COMPARTMENT)
            .id(`${nodeId}#properties-compartment`)
            .build();

        for (const prop of properties) {
            if (prop == undefined) {
                continue;
            }

            const propId = readonly ? idRegistry.getIdOrUnresolved(prop) : idRegistry.getId(prop);

            const multiplicityStr =
                prop.multiplicity != undefined ? `[${this.formatMultiplicity(prop.multiplicity)}]` : "";
            const propName = this.modelState.languageServices.AstSerializer.serializePrimitive(
                { value: prop.name ?? "unnamed" },
                ID
            );

            const typeName = this.getPropertyTypeName(prop.type);
            const propText = `${propName}: ${typeName}${multiplicityStr}`;

            const propLabel = GPropertyLabel.builder().id(`${propId}#label`).text(propText).readonly(readonly).build();
            propertiesCompartment.children.push(propLabel);
        }

        children.push(propertiesCompartment);
        return children;
    }

    /**
     * Gets the display name for a property type.
     *
     * @param propertyType The property type value (PrimitiveType or EnumTypeReference)
     * @returns The type name string
     */
    private getPropertyTypeName(propertyType: unknown): string {
        if (propertyType == undefined || typeof propertyType !== "object") {
            return "unknown";
        }
        const pt = propertyType as { $type?: string; name?: string; enum?: { ref?: { name?: string } } };
        if (pt.$type === "PrimitiveType") {
            return pt.name ?? "unknown";
        }
        if (pt.$type === "EnumTypeReference") {
            return pt.enum?.ref?.name ?? "unknown";
        }
        return "unknown";
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

            const extendsRefs = cls.extensions?.extensions ?? [];
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
    ): GInheritanceEdge {
        const edgeId = idRegistry.getId(inheritance);
        const sourceId = idRegistry.getId(sourceClass);
        const targetId = idRegistry.getId(targetClass);
        const metadata = validatedMetadata.edges[edgeId];
        const edge = GInheritanceEdge.builder().id(edgeId).sourceId(sourceId).targetId(targetId).build();

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
            if (assoc == undefined || assoc.source == undefined || assoc.target == undefined) {
                continue;
            }

            const startClassRef = assoc.source.class?.ref;
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
    ): GAssociationEdge {
        const edgeId = idRegistry.getId(assoc);
        const sourceId = idRegistry.getId(sourceClass);
        const targetId = idRegistry.getId(targetClass);
        const metadata = validatedMetadata.edges[edgeId];
        const { sourceKind, targetKind } = this.getAssociationEndKinds(assoc.operator);
        const edge = GAssociationEdge.builder()
            .id(edgeId)
            .sourceId(sourceId)
            .targetId(targetId)
            .operator(assoc.operator ?? "--")
            .sourceKind(sourceKind)
            .targetKind(targetKind)
            .build();

        this.applyRoutingPoints(edge, metadata);

        if (assoc.source != undefined) {
            const startNodes = this.createAssociationEndNodes(idRegistry, assoc.source, "source", validatedMetadata);
            edge.children.push(...startNodes);
        }

        if (assoc.target != undefined) {
            const targetNodes = this.createAssociationEndNodes(idRegistry, assoc.target, "target", validatedMetadata);
            edge.children.push(...targetNodes);
        }

        return edge;
    }

    /**
     * Creates nodes for an association endpoint with property name and/or multiplicity.
     *
     * @param idRegistry The ID registry for AST node ID generation
     * @param associationEnd The association end definition
     * @param target Whether this is at "source" or "target" of the association
     * @param validatedMetadata The validated metadata containing node placement
     * @returns An array of created nodes (property and/or multiplicity)
     */
    private createAssociationEndNodes(
        idRegistry: ModelIdRegistry,
        associationEnd: PartialAssociationEnd,
        target: "source" | "target",
        validatedMetadata: GraphMetadata
    ): GModelElement[] {
        const nodes: GModelElement[] = [];
        const property = associationEnd.name;
        const multiplicity = associationEnd.multiplicity;

        if (property != undefined) {
            const propertyId = idRegistry.getId(associationEnd);
            const propertyMeta = validatedMetadata.nodes[propertyId];
            const metadata =
                propertyMeta?.meta != undefined && NodeLayoutMetadataUtil.isValid(propertyMeta.meta)
                    ? propertyMeta.meta
                    : NodeLayoutMetadataUtil.create(0, 0);

            const propertyNode = GAssociationPropertyNode.builder().id(propertyId).end(target).meta(metadata).build();

            const propertyLabel = GAssociationPropertyLabel.builder().id(`${propertyId}-label`).text(property).build();

            propertyNode.children.push(propertyLabel);
            nodes.push(propertyNode);
        }

        if (multiplicity != undefined) {
            const multiplicityId = idRegistry.getId(multiplicity);
            const multiplicityMeta = validatedMetadata.nodes[multiplicityId];
            const metadata =
                multiplicityMeta?.meta != undefined && NodeLayoutMetadataUtil.isValid(multiplicityMeta.meta)
                    ? multiplicityMeta.meta
                    : NodeLayoutMetadataUtil.create(0, 0);

            const multiplicityNode = GAssociationMultiplicityNode.builder()
                .id(multiplicityId)
                .end(target)
                .meta(metadata)
                .build();

            const multiplicityLabel = GAssociationMultiplicityLabel.builder()
                .id(`${multiplicityId}-label`)
                .text(this.formatMultiplicity(multiplicity))
                .build();

            multiplicityNode.children.push(multiplicityLabel);
            nodes.push(multiplicityNode);
        }

        return nodes;
    }

    /**
     * Applies routing points to an edge from metadata if available.
     *
     * @param edge The edge to apply routing points to
     * @param metadata Metadata containing routing points
     */
    private applyRoutingPoints(
        edge: GInheritanceEdge | GAssociationEdge,
        metadata: { meta?: object } | undefined
    ): void {
        if (metadata != undefined && metadata.meta != undefined && EdgeLayoutMetadataUtil.isValid(metadata.meta)) {
            edge.meta = metadata.meta;
        }
    }

    /**
     * Formats multiplicity for display in labels.
     * Handles both single multiplicities (*, +, ?, or numeric) and range multiplicities.
     *
     * @param multiplicity The multiplicity to format
     * @returns Formatted string like "*", "1", "0..1"
     */
    private formatMultiplicity(multiplicity: PartialMultiplicity): string {
        if (multiplicity.$type === "SingleMultiplicity") {
            const single = multiplicity as PartialAstNode<SingleMultiplicityType>;
            if (single.value != undefined) {
                return single.value;
            }
            if (single.numericValue !== undefined && single.numericValue !== null) {
                return single.numericValue.toString();
            }
        } else if (multiplicity.$type === "RangeMultiplicity") {
            const range = multiplicity as PartialAstNode<RangeMultiplicityType>;
            const lower = range.lower ?? 0;
            const upper = range.upper ?? range.upperNumeric ?? "*";
            return `${lower}..${upper}`;
        }

        return "";
    }

    /**
     * Determines the source and target end kinds based on the association operator.
     *
     * @param operator The association operator string
     * @returns Object with sourceKind and targetKind
     */
    private getAssociationEndKinds(operator: string | undefined): {
        sourceKind: AssociationEndKind;
        targetKind: AssociationEndKind;
    } {
        let sourceKind = AssociationEndKind.NONE;
        let targetKind = AssociationEndKind.NONE;

        switch (operator) {
            case MetamodelAssociationOperators.NAVIGABLE_TO_TARGET:
                targetKind = AssociationEndKind.ARROW;
                break;
            case MetamodelAssociationOperators.NAVIGABLE_TO_SOURCE:
                sourceKind = AssociationEndKind.ARROW;
                break;
            case MetamodelAssociationOperators.BIDIRECTIONAL:
                sourceKind = AssociationEndKind.ARROW;
                targetKind = AssociationEndKind.ARROW;
                break;
            case MetamodelAssociationOperators.COMPOSITION_SOURCE_NAVIGABLE_TARGET:
                sourceKind = AssociationEndKind.COMPOSITION;
                targetKind = AssociationEndKind.ARROW;
                break;
            case MetamodelAssociationOperators.COMPOSITION_SOURCE:
                sourceKind = AssociationEndKind.COMPOSITION;
                break;
            case MetamodelAssociationOperators.COMPOSITION_TARGET_NAVIGABLE_SOURCE:
                sourceKind = AssociationEndKind.ARROW;
                targetKind = AssociationEndKind.COMPOSITION;
                break;
            case MetamodelAssociationOperators.COMPOSITION_TARGET:
                targetKind = AssociationEndKind.COMPOSITION;
                break;
        }

        return { sourceKind, targetKind };
    }
}

import { sharedImport } from "@mdeo/language-shared";

const { GNode, GNodeBuilder, GEdge, GEdgeBuilder, GLabel, GLabelBuilder } = sharedImport("@eclipse-glsp/server");

/**
 * Type constants for metamodel diagram elements.
 */
export enum MetamodelElementType {
    NODE_METACLASS = "node:metaclass",
    LABEL_METACLASS_NAME = "label:metaclass-name",
    LABEL_PROPERTY = "label:property",
    LABEL_ASSOCIATION_END = "label:association-end",
    EDGE_INHERITANCE = "edge:inheritance",
    EDGE_ASSOCIATION = "edge:association"
}

/**
 * Node representing a MetaClass in the diagram.
 */
export class MetaClassNode extends GNode {
    name?: string;
    isAbstract?: boolean;

    static override builder(): MetaClassNodeBuilder {
        return new MetaClassNodeBuilder(MetaClassNode).type(MetamodelElementType.NODE_METACLASS);
    }
}

export class MetaClassNodeBuilder<T extends MetaClassNode = MetaClassNode> extends GNodeBuilder<T> {
    name(name: string): this {
        this.proxy.name = name;
        return this;
    }

    isAbstract(isAbstract: boolean): this {
        this.proxy.isAbstract = isAbstract;
        return this;
    }
}

/**
 * Label for the MetaClass name.
 */
export class MetaClassLabel extends GLabel {
    static override builder(): MetaClassLabelBuilder {
        return new MetaClassLabelBuilder(MetaClassLabel).type(MetamodelElementType.LABEL_METACLASS_NAME);
    }
}

export class MetaClassLabelBuilder<T extends MetaClassLabel = MetaClassLabel> extends GLabelBuilder<T> {}

/**
 * Label for property names and types.
 */
export class PropertyLabel extends GLabel {
    static override builder(): PropertyLabelBuilder {
        return new PropertyLabelBuilder(PropertyLabel).type(MetamodelElementType.LABEL_PROPERTY);
    }
}

export class PropertyLabelBuilder<T extends PropertyLabel = PropertyLabel> extends GLabelBuilder<T> {}

/**
 * Label for association endpoint information (property name and multiplicity).
 */
export class AssociationEndLabel extends GLabel {
    static override builder(): AssociationEndLabelBuilder {
        return new AssociationEndLabelBuilder(AssociationEndLabel).type(MetamodelElementType.LABEL_ASSOCIATION_END);
    }
}

export class AssociationEndLabelBuilder<T extends AssociationEndLabel = AssociationEndLabel> extends GLabelBuilder<T> {}

/**
 * Edge representing an inheritance relationship (extends).
 */
export class InheritanceEdge extends GEdge {
    static override builder(): InheritanceEdgeBuilder {
        return new InheritanceEdgeBuilder(InheritanceEdge).type(MetamodelElementType.EDGE_INHERITANCE);
    }
}

export class InheritanceEdgeBuilder<E extends InheritanceEdge = InheritanceEdge> extends GEdgeBuilder<E> {}

/**
 * Edge representing an association relationship.
 */
export class AssociationEdge extends GEdge {
    operator?: string;

    static override builder(): AssociationEdgeBuilder {
        return new AssociationEdgeBuilder(AssociationEdge).type(MetamodelElementType.EDGE_ASSOCIATION);
    }
}

export class AssociationEdgeBuilder<E extends AssociationEdge = AssociationEdge> extends GEdgeBuilder<E> {
    operator(operator: string): this {
        this.proxy.operator = operator;
        return this;
    }
}

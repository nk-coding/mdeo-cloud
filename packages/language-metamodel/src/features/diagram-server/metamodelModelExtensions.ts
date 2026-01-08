import { sharedImport } from "@mdeo/language-shared";

const {
    GNode,
    GNodeBuilder,
    GEdge,
    GEdgeBuilder,
    GLabel,
    GLabelBuilder,
    GCompartment,
    GCompartmentBuilder,
    GModelElement,
    GModelElementBuilder
} = sharedImport("@eclipse-glsp/server");

/**
 * Type constants for metamodel diagram elements.
 */
export enum MetamodelElementType {
    NODE_CLASS = "node:class",
    LABEL_CLASS_NAME = "label:class-name",
    LABEL_PROPERTY = "label:property",
    LABEL_ASSOCIATION_END = "label:association-end",
    EDGE_INHERITANCE = "edge:inheritance",
    EDGE_ASSOCIATION = "edge:association",
    COMPARTMENT = "comp:compartment",
    DIVIDER = "divider:horizontal"
}

/**
 * Node representing a Class in the diagram.
 */
export class ClassNode extends GNode {
    name?: string;
    isAbstract?: boolean;

    static override builder(): ClassNodeBuilder {
        return new ClassNodeBuilder(ClassNode).type(MetamodelElementType.NODE_CLASS);
    }
}

export class ClassNodeBuilder<T extends ClassNode = ClassNode> extends GNodeBuilder<T> {
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
 * Label for the Class name.
 */
export class ClassLabel extends GLabel {
    static override builder(): ClassLabelBuilder {
        return new ClassLabelBuilder(ClassLabel).type(MetamodelElementType.LABEL_CLASS_NAME);
    }
}

export class ClassLabelBuilder<T extends ClassLabel = ClassLabel> extends GLabelBuilder<T> {}

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

/**
 * Compartment for grouping labels within a class node.
 */
export class ClassCompartment extends GCompartment {
    static override builder(): ClassCompartmentBuilder {
        return new ClassCompartmentBuilder(ClassCompartment).type(MetamodelElementType.COMPARTMENT).layout("vbox");
    }
}

export class ClassCompartmentBuilder<T extends ClassCompartment = ClassCompartment> extends GCompartmentBuilder<T> {}

/**
 * Divider for separating compartments.
 */
export class ClassDivider extends GModelElement {
    static builder(): ClassDividerBuilder {
        return new ClassDividerBuilder(ClassDivider).type(MetamodelElementType.DIVIDER);
    }
}

export class ClassDividerBuilder<T extends ClassDivider = ClassDivider> extends GModelElementBuilder<T> {}

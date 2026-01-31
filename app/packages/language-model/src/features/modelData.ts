/**
 * Represents a property value in a model instance.
 * Can be a primitive value, an enum reference, or a list of values.
 */
export type ModelDataPropertyValue = string | number | boolean | { enum: string } | null ;

/**
 * Represents an object instance in a model.
 * Contains the class name and all property assignments.
 */
export interface ModelDataInstance {
    /**
     * Unique name/identifier of this instance within the model.
     */
    name: string;

    /**
     * Fully qualified name of the class this instance belongs to.
     */
    className: string;

    /**
     * All property assignments for this instance.
     * Includes all properties defined in the class, with null for unset optional properties.
     */
    properties: Record<string, ModelDataPropertyValue | ModelDataPropertyValue[]>;
}

/**
 * Represents a link between two model instances.
 * Both ends define the property name for easier handling.
 */
export interface ModelDataLink {
    /**
     * Name of the source instance.
     */
    sourceName: string;

    /**
     * Property name on the source side of the link.
     * Null if the association end has no property name.
     */
    sourceProperty: string | null;

    /**
     * Name of the target instance.
     */
    targetName: string;

    /**
     * Property name on the target side of the link.
     * Null if the association end has no property name.
     */
    targetProperty: string | null;
}

/**
 * Root container for a model in the ModelData format.
 * Contains the metamodel reference, all instances, and all links.
 */
export interface ModelData {
    /**
     * URI of the imported metamodel file.
     */
    metamodelUri: string;

    /**
     * All object instances in the model.
     */
    instances: ModelDataInstance[];

    /**
     * All links between instances in the model.
     */
    links: ModelDataLink[];
}

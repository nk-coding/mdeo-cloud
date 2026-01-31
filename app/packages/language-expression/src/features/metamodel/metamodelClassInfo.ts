import type { ValueType } from "../../typir-extensions/config/type.js";

/**
 * Represents property information extracted from a metamodel class.
 * This is an intermediate representation used before generating TypedProperty or ClassType members.
 */
export interface MetamodelPropertyInfo {
    /**
     * The name of the property.
     */
    name: string;

    /**
     * The value type of the property (for Typir type system).
     */
    valueType: ValueType;
}

/**
 * Represents relation (association) information extracted from a metamodel.
 * This is an intermediate representation used before generating TypedRelation or ClassType members.
 */
export interface MetamodelRelationInfo {
    /**
     * The property name that defines this relation on the class.
     */
    property: string;

    /**
     * The property name at the opposite end of the relation, or undefined if unnamed.
     */
    oppositeProperty?: string;

    /**
     * The fully qualified name of the class at the opposite end of the relation.
     */
    oppositeClassName: string;

    /**
     * True if this is an outgoing relation, false if incoming.
     */
    isOutgoing: boolean;

    /**
     * The value type of the relation (for Typir type system).
     */
    valueType: ValueType;
}

/**
 * Represents class information extracted from a metamodel.
 * This is an intermediate representation that can be converted to:
 * - TypedClass for the TypedAst
 * - ClassType for the Typir type system
 */
export interface MetamodelClassInfo {
    /**
     * The fully qualified name of the class (e.g., "metamodel.Person").
     */
    name: string;

    /**
     * The package name where the class is defined.
     */
    package: string;

    /**
     * Fully qualified names of direct superclasses.
     */
    superClasses: string[];

    /**
     * Properties defined on this class (not inherited).
     */
    properties: MetamodelPropertyInfo[];

    /**
     * Relations defined on this class (not inherited).
     */
    relations: MetamodelRelationInfo[];
}

import { createInterface, Optional, Ref, type ASTType } from "@mdeo/language-common";
import { ClassOrImport, Property } from "@mdeo/language-metamodel";

/**
 * Literal value for property assignments.
 * Supports string, number, and boolean literals.
 */
export const LiteralValue = createInterface("LiteralValue").attrs({
    stringValue: Optional(String),
    numberValue: Optional(Number),
    booleanValue: Optional(Boolean)
});

/**
 * Type representing a LiteralValue AST node.
 */
export type LiteralValueType = ASTType<typeof LiteralValue>;

/**
 * Property assignment in an object instance.
 * References a property from the class and assigns it a value.
 */
export const PropertyAssignment = createInterface("PropertyAssignment").attrs({
    property: Ref(() => Property),
    value: LiteralValue
});

/**
 * Type representing a PropertyAssignment AST node.
 */
export type PropertyAssignmentType = ASTType<typeof PropertyAssignment>;

/**
 * Object instance definition.
 * Represents an instance of a class from the metamodel.
 */
export const ObjectInstance = createInterface("ObjectInstance").attrs({
    name: String,
    class: Ref(() => ClassOrImport),
    properties: [PropertyAssignment]
});

/**
 * Type representing an ObjectInstance AST node.
 */
export type ObjectInstanceType = ASTType<typeof ObjectInstance>;

/**
 * Metamodel file import.
 * Simple import statement for referencing metamodel files.
 */
export const MetamodelFileImport = createInterface("MetamodelFileImport").attrs({
    file: String
});

/**
 * Type representing MetamodelFileImport AST node.
 */
export type MetamodelFileImportType = ASTType<typeof MetamodelFileImport>;

/**
 * Model root interface.
 * Contains metamodel imports and object instances.
 */
export const Model = createInterface("Model").attrs({
    import: MetamodelFileImport,
    objects: [ObjectInstance]
});

/**
 * Model AST type.
 */
export type ModelType = ASTType<typeof Model>;

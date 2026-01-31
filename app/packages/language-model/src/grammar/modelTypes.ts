import { createInterface, createType, Optional, Ref, type ASTType, type BaseType } from "@mdeo/language-common";
import { Class, EnumEntry, Property } from "@mdeo/language-metamodel";
import type { AstNode } from "langium";

/**
 * Simple value for property assignments.
 * Supports string, number, and boolean literals.
 */
export const SimpleValue = createInterface("SimpleValue").attrs({
    stringValue: Optional(String),
    numberValue: Optional(Number),
    booleanValue: Optional(Boolean)
});

/**
 * Type representing a SimpleValue AST node.
 */
export type SimpleValueType = ASTType<typeof SimpleValue>;

/**
 * Enum value for property assignments.
 * References an enum entry from the metamodel.
 */
export const EnumValue = createInterface("EnumValue").attrs({
    value: Ref(() => EnumEntry)
});

/**
 * Type representing an EnumValue AST node.
 */
export type EnumValueType = ASTType<typeof EnumValue>;

/**
 * Single value type union.
 * Represents either a simple value or an enum value.
 */
export const SingleValue: BaseType<AstNode> = createType("SingleValue").types(SimpleValue, EnumValue);

/**
 * Type representing a SingleValue AST node.
 */
export type SingleValueType = ASTType<typeof SingleValue>;

/**
 * List value for property assignments with multiplicity > 1.
 * Contains a comma-separated list of values in square brackets.
 */
export const ListValue = createInterface("ListValue").attrs({
    values: [SingleValue]
});

/**
 * Type representing a ListValue AST node.
 */
export type ListValueType = ASTType<typeof ListValue>;

/**
 * Literal value type union for property assignments.
 * Can be a single value (simple or enum) or a list value.
 */
export const LiteralValue: BaseType<AstNode> = createType("LiteralValue").types(SimpleValue, EnumValue, ListValue);

/**
 * Type representing a LiteralValue AST node.
 */
export type LiteralValueType = ASTType<typeof LiteralValue>;

/**
 * Property assignment in an object instance.
 * References a property from the class and assigns it a value.
 */
export const PropertyAssignment = createInterface("PropertyAssignment").attrs({
    name: Ref(() => Property),
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
    class: Ref(() => Class),
    properties: [PropertyAssignment]
});

/**
 * Type representing an ObjectInstance AST node.
 */
export type ObjectInstanceType = ASTType<typeof ObjectInstance>;

/**
 * Link end representing one side of a link.
 * References an object instance with an optional property specification.
 */
export const LinkEnd = createInterface("LinkEnd").attrs({
    object: Ref(() => ObjectInstance),
    property: Optional(Ref(() => Property))
});

/**
 * Type representing a LinkEnd AST node.
 */
export type LinkEndType = ASTType<typeof LinkEnd>;

/**
 * Link definition between two object instances.
 * Format: objectId1[.property] -- objectId2[.property]
 */
export const Link = createInterface("Link").attrs({
    source: LinkEnd,
    target: LinkEnd
});

/**
 * Type representing a Link AST node.
 */
export type LinkType = ASTType<typeof Link>;

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
 * Contains metamodel imports, object instances, and links.
 */
export const Model = createInterface("Model").attrs({
    import: MetamodelFileImport,
    objects: [ObjectInstance],
    links: [Link]
});

/**
 * Model AST type.
 */
export type ModelType = ASTType<typeof Model>;

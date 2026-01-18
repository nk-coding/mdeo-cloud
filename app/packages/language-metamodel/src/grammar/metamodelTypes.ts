import { createInterface, createType, Optional, Ref, Union } from "@mdeo/language-common";
import type { ASTType, BaseType } from "@mdeo/language-common";
import { FileScopingConfig, generateImportTypes } from "@mdeo/language-shared";
import type { AstNode } from "langium";

/**
 * Enumeration of primitive types supported in the metamodel.
 */
export enum MetamodelPrimitiveTypes {
    INT = "int",
    STRING = "string",
    BOOLEAN = "boolean",
    LONG = "long",
    DOUBLE = "double",
    FLOAT = "float"
}

/**
 * Primitive type for metamodel properties.
 * Represents basic data types that can be used in metamodel class properties.
 */
export const PrimitiveType = createInterface("PrimitiveType").attrs({
    name: Union(
        MetamodelPrimitiveTypes.INT,
        MetamodelPrimitiveTypes.STRING,
        MetamodelPrimitiveTypes.BOOLEAN,
        MetamodelPrimitiveTypes.LONG,
        MetamodelPrimitiveTypes.DOUBLE,
        MetamodelPrimitiveTypes.FLOAT
    )
});

/**
 * Type representing a PrimitiveType AST node.
 */
export type PrimitiveTypeType = ASTType<typeof PrimitiveType>;

/**
 * Single multiplicity specification.
 * Can be a numeric value or one of the special symbols: * (any), + (one or more), ? (optional).
 */
export const SingleMultiplicity = createInterface("SingleMultiplicity").attrs({
    value: Optional(Union("*", "+", "?")),
    numericValue: Optional(Number)
});

/**
 * Type representing a SingleMultiplicity AST node.
 */
export type SingleMultiplicityType = ASTType<typeof SingleMultiplicity>;

/**
 * Range multiplicity specification.
 * Defines a lower bound and upper bound (can be * for unlimited).
 */
export const RangeMultiplicity = createInterface("RangeMultiplicity").attrs({
    lower: Number,
    upper: Union("*"),
    upperNumeric: Optional(Number)
});

/**
 * Type representing a RangeMultiplicity AST node.
 */
export type RangeMultiplicityType = ASTType<typeof RangeMultiplicity>;

/**
 * Union type for multiplicity specifications.
 * Can be either single or range multiplicity.
 */
export const Multiplicity = createType("Multiplicity").types(SingleMultiplicity, RangeMultiplicity);

/**
 * Type representing a Multiplicity AST node.
 */
export type MultiplicityType = ASTType<typeof Multiplicity>;

/**
 * Property definition for metamodel classes.
 * Represents an attribute with a name, primitive type, and optional multiplicity.
 */
export const Property = createInterface("Property").attrs({
    name: String,
    type: PrimitiveType,
    multiplicity: Optional(Multiplicity)
});

/**
 * Type representing a Property AST node.
 */
export type PropertyType = ASTType<typeof Property>;

/**
 * Class extension definition.
 * Represents a reference to a parent class in an inheritance relationship.
 */
export const ClassExtension = createInterface("ClassExtension").attrs({
    class: Ref(() => ClassOrImport)
});

/**
 * Type representing a ClassExtension AST node.
 */
export type ClassExtensionType = ASTType<typeof ClassExtension>;

/**
 * Class definition.
 * Represents a class in the metamodel with properties and inheritance relationships.
 */
export const Class = createInterface("Class").attrs({
    name: String,
    isAbstract: Boolean,
    extends: [ClassExtension],
    properties: [Property]
});

/**
 * Type representing a Class AST node.
 */
export type ClassType = ASTType<typeof Class>;

/**
 * File scoping configuration for classes.
 * Enables cross-file references and imports for classes.
 */
export const metamodelFileScopingConfig = new FileScopingConfig<ClassType>("Class", Class);

/**
 * Import types for classes.
 * Generated types for importing classes from other files.
 */
export const { importType: ClassImport, fileImportType: ClassFileImport } =
    generateImportTypes(metamodelFileScopingConfig);

/**
 * Union type for Class or ClassImport.
 * Used for references that can point to either a locally defined or imported class.
 */
export const ClassOrImport: BaseType<AstNode> = createType("ClassOrImport").types(Class, ClassImport);

/**
 * Type representing ClassImport AST node.
 */
export type ClassImportType = ASTType<typeof ClassImport>;

/**
 * Type representing ClassOrImport AST node.
 */
export type ClassOrImportType = ASTType<typeof ClassOrImport>;

/**
 * Association end definition.
 * Represents one end of an association or composition relationship.
 */
export const AssociationEnd = createInterface("AssociationEnd").attrs({
    class: Ref(() => ClassOrImport),
    name: Optional(String),
    multiplicity: Optional(Multiplicity)
});

/**
 * Type representing an AssociationEnd AST node.
 */
export type AssociationEndType = ASTType<typeof AssociationEnd>;

/**
 * Association definition.
 * Represents a relationship between two classes.
 * Can be a regular association (--), composition from start (*--), or composition from target (--*).
 */
export const Association = createInterface("Association").attrs({
    source: AssociationEnd,
    operator: Union("--", "*--", "--*"),
    target: AssociationEnd
});

/**
 * Type representing an Association AST node.
 */
export type AssociationType = ASTType<typeof Association>;

/**
 * Union type for Class or Association.
 */
export const ClassOrAssociation = createType("ClassOrAssociation").types(Class, Association);

/**
 * The root MetaModel type.
 * Contains imports, class definitions, and associations.
 */
export const MetaModel = createInterface("MetaModel").attrs({
    imports: [ClassFileImport],
    classesAndAssociations: [ClassOrAssociation]
});

/**
 * Type representing the MetaModel AST node.
 */
export type MetaModelType = ASTType<typeof MetaModel>;

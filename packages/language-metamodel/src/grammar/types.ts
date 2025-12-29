import {
    createInterface,
    createType,
    FileScopingConfig,
    generateImportTypes,
    Optional,
    Ref,
    Union
} from "@mdeo/language-common";
import type { ASTType, BaseType } from "@mdeo/language-common";
import type { AstNode } from "langium";

/**
 * Primitive type for metamodel properties.
 * Represents basic data types that can be used in metamodel class properties.
 */
export const PrimitiveType = createInterface("PrimitiveType").attrs({
    name: Union("int", "string", "boolean", "long", "double", "float")
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
    value: Union("*", "+", "?"),
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
 * MetaClass definition.
 * Represents a class in the metamodel with properties and inheritance relationships.
 */
export const MetaClass = createInterface("MetaClass").attrs({
    name: String,
    isAbstract: Boolean,
    extends: [Ref(() => MetaClassOrImport)],
    properties: [Property]
});

/**
 * Type representing a MetaClass AST node.
 */
export type MetaClassType = ASTType<typeof MetaClass>;

/**
 * File scoping configuration for metaclasses.
 * Enables cross-file references and imports for metaclasses.
 */
export const metamodelFileScopingConfig = new FileScopingConfig<MetaClassType>("MetaClass", MetaClass);

/**
 * Import types for metaclasses.
 * Generated types for importing metaclasses from other files.
 */
export const { importType: MetaClassImport, fileImportType: MetaClassFileImport } =
    generateImportTypes(metamodelFileScopingConfig);

/**
 * Union type for MetaClass or MetaClassImport.
 * Used for references that can point to either a locally defined or imported metaclass.
 */
export const MetaClassOrImport: BaseType<AstNode> = createType("MetaClassOrImport").types(MetaClass, MetaClassImport);

/**
 * Type representing MetaClassOrImport AST node.
 */
export type MetaClassOrImportType = ASTType<typeof MetaClassOrImport>;

/**
 * Association end definition.
 * Represents one end of an association or composition relationship.
 */
export const AssociationEnd = createInterface("AssociationEnd").attrs({
    class: Ref(() => MetaClassOrImport),
    property: Optional(String),
    multiplicity: Optional(Multiplicity)
});

/**
 * Type representing an AssociationEnd AST node.
 */
export type AssociationEndType = ASTType<typeof AssociationEnd>;

/**
 * Association definition.
 * Represents a relationship between two metaclasses.
 * Can be a regular association (--), composition from start (*--), or composition from target (--*).
 */
export const Association = createInterface("Association").attrs({
    start: AssociationEnd,
    operator: Union("--", "*--", "--*"),
    target: AssociationEnd
});

/**
 * Type representing an Association AST node.
 */
export type AssociationType = ASTType<typeof Association>;

/**
 * Union type for MetaClass or Association.
 */
export const ClassOrAssociation = createType("ClassOrAssociation").types(MetaClass, Association);

/**
 * The root MetaModel type.
 * Contains imports, class definitions, and associations.
 */
export const MetaModel = createInterface("MetaModel").attrs({
    imports: [MetaClassFileImport],
    classesAndAssociations: [ClassOrAssociation]
});

/**
 * Type representing the MetaModel AST node.
 */
export type MetaModelType = ASTType<typeof MetaModel>;

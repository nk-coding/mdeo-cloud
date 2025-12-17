import {
    createInterface,
    createType,
    FileScopingConfig,
    generateImportTypes,
    Optional,
    Ref,
    Union
} from "@mdeo/language-common";
import type { ASTType, BaseType, Type, UnionTypes } from "@mdeo/language-common";
import type { AstNode } from "langium";

export const PrimitiveType = createInterface("PrimitiveType").attrs({
    name: Union("int", "string", "boolean", "long", "double", "float")
});

export type PrimitiveTypeType = ASTType<typeof PrimitiveType>;

export const SingleMultiplicity = createInterface("SingleMultiplicity").attrs({
    value: Union("*", "+", "?"),
    numericValue: Optional(Number)
});

export type SingleMultiplicityType = ASTType<typeof SingleMultiplicity>;

export const RangeMultiplicity = createInterface("RangeMultiplicity").attrs({
    lower: Number,
    upper: Union("*"),
    upperNumeric: Optional(Number)
});

export type RangeMultiplicityType = ASTType<typeof RangeMultiplicity>;

export const Multiplicity = createType("Multiplicity").types(SingleMultiplicity, RangeMultiplicity);

export type MultiplicityType = ASTType<typeof Multiplicity>;

export const Property = createInterface("Property").attrs({
    name: String,
    type: PrimitiveType,
    multiplicity: Optional(Multiplicity)
});

export type PropertyType = ASTType<typeof Property>;

export const MetaClass = createInterface("MetaClass").attrs({
    name: String,
    isAbstract: Boolean,
    extends: [Ref(() => MetaClassOrImport)],
    properties: [Property]
});

export type MetaClassType = ASTType<typeof MetaClass>;

export const metamodelFileScopingConfig = new FileScopingConfig<MetaClassType>("MetaClass", MetaClass);

export const { importType: MetaClassImport, fileImportType: MetaClassFileImport } =
    generateImportTypes(metamodelFileScopingConfig);

export const MetaClassOrImport: BaseType<AstNode> = createType("MetaClassOrImport").types(MetaClass, MetaClassImport);

export type MetaClassOrImportType = ASTType<typeof MetaClassOrImport>;

export const AssociationEnd = createInterface("AssociationEnd").attrs({
    class: Ref(() => MetaClassOrImport),
    property: Optional(String),
    multiplicity: Optional(Multiplicity)
});

export type AssociationEndType = ASTType<typeof AssociationEnd>;

export const Association = createInterface("Association").attrs({
    start: AssociationEnd,
    operator: Union("--", "*--", "--*"),
    target: AssociationEnd
});

export type AssociationType = ASTType<typeof Association>;

export const MetaModel = createInterface("MetaModel").attrs({
    imports: [MetaClassFileImport],
    classes: [MetaClass],
    associations: [Association]
});

export type MetaModelType = ASTType<typeof MetaModel>;

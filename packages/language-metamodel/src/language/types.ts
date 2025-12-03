import {
    createInterface,
    createType,
    Optional,
    Ref,
    Union
} from "@mdeo/language-common";
import type { ASTType } from "@mdeo/language-common";

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

export const Multiplicity = createType("Multiplicity").types(
    SingleMultiplicity,
    RangeMultiplicity
);

export type MultiplicityType = ASTType<typeof Multiplicity>;

export const Property = createInterface("Property").attrs({
    name: String,
    type: PrimitiveType,
    multiplicity: Optional(Multiplicity)
});

export type PropertyType = ASTType<typeof Property>;

export const MetaClass = createInterface("MetaClass").attrs({
    name: String,
    isAbstract: Optional(Boolean),
    extends: [Ref(() => MetaClass)],
    properties: [Property]
});

export type MetaClassType = ASTType<typeof MetaClass>;

export const AssociationEnd = createInterface("AssociationEnd").attrs({
    class: Ref(() => MetaClass),
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
    name: String,
    classes: [MetaClass],
    associations: [Association]
});

export type MetaModelType = ASTType<typeof MetaModel>;
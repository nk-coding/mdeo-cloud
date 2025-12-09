import { createRule, or, optional, many, ref } from "@mdeo/language-common";
import {
    PrimitiveType,
    SingleMultiplicity,
    RangeMultiplicity,
    Multiplicity,
    Property,
    MetaClass,
    AssociationEnd,
    Association,
    MetaModel
} from "./types.js";
import { ID, INT } from "./terminals.js";

export const PrimitiveTypeRule = createRule("PrimitiveTypeRule")
    .returns(PrimitiveType)
    .as(({ set }) => [set("name", "int", "string", "boolean", "long", "double", "float")]);

export const SingleMultiplicityRule = createRule("SingleMultiplicityRule")
    .returns(SingleMultiplicity)
    .as(({ set }) => [or(set("numericValue", INT), set("value", "*", "+", "?"))]);

export const RangeMultiplicityRule = createRule("RangeMultiplicityRule")
    .returns(RangeMultiplicity)
    .as(({ set }) => [set("lower", INT), "..", or(set("upper", "*"), set("upperNumeric", INT))]);

export const MultiplicityRule = createRule("MultiplicityRule")
    .returns(Multiplicity)
    .as(() => ["[", or(SingleMultiplicityRule, RangeMultiplicityRule), "]"]);

export const PropertyRule = createRule("PropertyRule")
    .returns(Property)
    .as(({ set }) => [set("name", ID), ":", set("type", PrimitiveTypeRule), optional("[", MultiplicityRule, "]")]);

export const MetaClassRule = createRule("MetaClassRule")
    .returns(MetaClass)
    .as(({ set, add, flag }) => [
        "class",
        flag("isAbstract", "abstract"),
        set("name", ID),
        "{",
        many(add("properties", PropertyRule)),
        "}"
    ]);

export const AssociationEndWithPropertyRule = createRule("AssociationEndWithPropertyRule")
    .returns(AssociationEnd)
    .as(({ set }) => [
        set("class", ref(MetaClass, ID)),
        ".",
        set("property", ID),
        optional("[", MultiplicityRule, "]")
    ]);

export const AssociationEndWithoutPropertyRule = createRule("AssociationEndWithoutPropertyRule")
    .returns(AssociationEnd)
    .as(({ set }) => [set("class", ref(MetaClass, ID)), optional("[", MultiplicityRule, "]")]);

export const AssociationEndRule = createRule("AssociationEndRule")
    .returns(AssociationEnd)
    .as(() => [or(AssociationEndWithPropertyRule, AssociationEndWithoutPropertyRule)]);

export const CompositionEndRule = createRule("CompositionEndRule")
    .returns(AssociationEnd)
    .as(({ set }) => [
        set("class", ref(MetaClass, ID)),
        ".",
        set("property", ID),
        optional("[", MultiplicityRule, "]")
    ]);

export const RegularAssociationStartWithPropertyRule = createRule("RegularAssociationStartWithPropertyRule")
    .returns(Association)
    .as(({ set }) => [
        set("start", AssociationEndWithPropertyRule),
        set("operator", "--"),
        set("target", AssociationEndRule)
    ]);

export const RegularAssociationTargetWithPropertyRule = createRule("RegularAssociationTargetWithPropertyRule")
    .returns(Association)
    .as(({ set }) => [
        set("start", AssociationEndWithoutPropertyRule),
        set("operator", "--"),
        set("target", AssociationEndWithPropertyRule)
    ]);

export const RegularAssociationRule = createRule("RegularAssociationRule")
    .returns(Association)
    .as(() => [or(RegularAssociationStartWithPropertyRule, RegularAssociationTargetWithPropertyRule)]);

export const CompositionFromStartRule = createRule("CompositionFromStartRule")
    .returns(Association)
    .as(({ set }) => [set("start", CompositionEndRule), set("operator", "*--"), set("target", AssociationEndRule)]);

export const CompositionFromTargetRule = createRule("CompositionFromTargetRule")
    .returns(Association)
    .as(({ set }) => [set("start", AssociationEndRule), set("operator", "--*"), set("target", CompositionEndRule)]);

export const AssociationRule = createRule("AssociationRule")
    .returns(Association)
    .as(() => [or(RegularAssociationRule, CompositionFromStartRule, CompositionFromTargetRule)]);

export const MetaModelRule = createRule("MetaModelRule")
    .returns(MetaModel)
    .as(({ add }) => [many(add("classes", MetaClassRule)), many(add("associations", AssociationRule))]);

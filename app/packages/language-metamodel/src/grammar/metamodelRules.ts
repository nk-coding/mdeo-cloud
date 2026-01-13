import { createRule, or, optional, many, ref, ID, INT, NEWLINE } from "@mdeo/language-common";
import { generateImportRules } from "@mdeo/language-shared";
import {
    PrimitiveType,
    SingleMultiplicity,
    RangeMultiplicity,
    Multiplicity,
    Property,
    ClassExtension,
    Class,
    AssociationEnd,
    Association,
    MetaModel,
    ClassOrImport,
    ClassImport,
    ClassFileImport
} from "./metamodelTypes.js";
import { metamodelFileScopingConfig } from "./metamodelTypes.js";

/**
 * Primitive type rule.
 * Matches one of the supported primitive types: int, string, boolean, long, double, float.
 */
export const PrimitiveTypeRule = createRule("PrimitiveTypeRule")
    .returns(PrimitiveType)
    .as(({ set }) => [set("name", "int", "string", "boolean", "long", "double", "float")]);

/**
 * Single multiplicity rule.
 * Matches either a numeric value or one of the special multiplicity symbols: *, +, ?
 */
export const SingleMultiplicityRule = createRule("SingleMultiplicityRule")
    .returns(SingleMultiplicity)
    .as(({ set }) => [or(set("numericValue", INT), set("value", "*", "+", "?"))]);

/**
 * Range multiplicity rule.
 * Matches a range specification like "1..*" or "0..5".
 */
export const RangeMultiplicityRule = createRule("RangeMultiplicityRule")
    .returns(RangeMultiplicity)
    .as(({ set }) => [set("lower", INT), "..", or(set("upper", "*"), set("upperNumeric", INT))]);

/**
 * Multiplicity rule.
 * Matches multiplicity specifications in brackets, either single or range.
 */
export const MultiplicityRule = createRule("MultiplicityRule")
    .returns(Multiplicity)
    .as(() => ["[", or(SingleMultiplicityRule, RangeMultiplicityRule), "]"]);

/**
 * Property rule.
 * Matches a property definition with name, type, and optional multiplicity.
 */
export const PropertyRule = createRule("PropertyRule")
    .returns(Property)
    .as(({ set }) => [set("name", ID), ":", set("type", PrimitiveTypeRule), optional("[", MultiplicityRule, "]")]);

/**
 * Class extension rule.
 * Matches a reference to a parent class in an extends clause.
 */
export const ClassExtensionRule = createRule("ClassExtensionRule")
    .returns(ClassExtension)
    .as(({ set }) => [set("class", ref(ClassOrImport, ID))]);

/**
 * Class rule.
 * Matches a class definition with optional abstract modifier, name, inheritance, and properties.
 */
export const ClassRule = createRule("ClassRule")
    .returns(Class)
    .as(({ set, add, flag }) => [
        optional(flag("isAbstract", "abstract")),
        "class",
        set("name", ID),
        optional("extends", add("extends", ClassExtensionRule), many(",", add("extends", ClassExtensionRule))),
        "{",
        many(or(add("properties", PropertyRule), NEWLINE)),
        "}"
    ]);

/**
 * Association end rule with property.
 * Matches an association end that specifies a property name (e.g., "Class.property[*]").
 */
export const AssociationEndWithPropertyRule = createRule("AssociationEndWithPropertyRule")
    .returns(AssociationEnd)
    .as(({ set }) => [
        set("class", ref(ClassOrImport, ID)),
        ".",
        set("property", ID),
        optional(set("multiplicity", MultiplicityRule))
    ]);

/**
 * Association end rule without property.
 * Matches an association end that only specifies a class (e.g., "Class[*]").
 */
export const AssociationEndWithoutPropertyRule = createRule("AssociationEndWithoutPropertyRule")
    .returns(AssociationEnd)
    .as(({ set }) => [set("class", ref(ClassOrImport, ID)), optional(set("multiplicity", MultiplicityRule))]);

/**
 * Association end rule.
 * Matches an association end, either with or without a property specification.
 */
export const AssociationEndRule = createRule("AssociationEndRule")
    .returns(AssociationEnd)
    .as(() => [or(AssociationEndWithPropertyRule, AssociationEndWithoutPropertyRule)]);

/**
 * Composition end rule.
 * Matches the composite end of a composition relationship (must have a property).
 */
export const CompositionEndRule = createRule("CompositionEndRule")
    .returns(AssociationEnd)
    .as(({ set }) => [
        set("class", ref(ClassOrImport, ID)),
        ".",
        set("property", ID),
        optional(set("multiplicity", MultiplicityRule))
    ]);

/**
 * Regular association rule with property on start.
 * Matches an association where the start end specifies a property.
 */
export const RegularAssociationStartWithPropertyRule = createRule("RegularAssociationStartWithPropertyRule")
    .returns(Association)
    .as(({ set }) => [
        set("start", AssociationEndWithPropertyRule),
        set("operator", "--"),
        set("target", AssociationEndRule)
    ]);

/**
 * Regular association rule with property on target.
 * Matches an association where the target end specifies a property.
 */
export const RegularAssociationTargetWithPropertyRule = createRule("RegularAssociationTargetWithPropertyRule")
    .returns(Association)
    .as(({ set }) => [
        set("start", AssociationEndWithoutPropertyRule),
        set("operator", "--"),
        set("target", AssociationEndWithPropertyRule)
    ]);

/**
 * Regular association rule.
 * Matches a regular bidirectional association (not a composition).
 */
export const RegularAssociationRule = createRule("RegularAssociationRule")
    .returns(Association)
    .as(() => [or(RegularAssociationStartWithPropertyRule, RegularAssociationTargetWithPropertyRule)]);

/**
 * Composition from start rule.
 * Matches a composition relationship where the start is the composite (*--).
 */
export const CompositionFromStartRule = createRule("CompositionFromStartRule")
    .returns(Association)
    .as(({ set }) => [set("start", CompositionEndRule), set("operator", "*--"), set("target", AssociationEndRule)]);

/**
 * Composition from target rule.
 * Matches a composition relationship where the target is the composite (--*).
 */
export const CompositionFromTargetRule = createRule("CompositionFromTargetRule")
    .returns(Association)
    .as(({ set }) => [set("start", AssociationEndRule), set("operator", "--*"), set("target", CompositionEndRule)]);

/**
 * Association rule.
 * Matches any type of association: regular, composition from start, or composition from target.
 */
export const AssociationRule = createRule("AssociationRule")
    .returns(Association)
    .as(() => [or(RegularAssociationRule, CompositionFromStartRule, CompositionFromTargetRule)]);

/**
 * Import rules for classes.
 */
export const { importRule: ClassImportRule, fileImportRule: ClassFileImportRule } = generateImportRules(
    metamodelFileScopingConfig,
    ClassImport,
    ClassFileImport,
    ID
);

/**
 * The MetaModel entry rule.
 * Defines the structure: imports first, then classes and associations.
 */
export const MetaModelRule = createRule("MetaModelRule")
    .returns(MetaModel)
    .as(({ add }) => [
        many(
            or(
                add("imports", ClassFileImportRule),
                add("classesAndAssociations", ClassRule),
                add("classesAndAssociations", AssociationRule),
                NEWLINE
            )
        )
    ]);

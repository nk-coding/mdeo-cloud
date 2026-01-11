import type { PartialAstNode } from "@mdeo/language-common";
import type {
    MetaModelType,
    ClassType,
    PropertyType,
    AssociationType,
    AssociationEndType,
    MultiplicityType,
    SingleMultiplicityType,
    RangeMultiplicityType
} from "./metamodelTypes.js";

/** Partial metamodel type with optional domain properties. */
export type PartialMetaModel = PartialAstNode<MetaModelType>;

/** Partial class type with optional domain properties. */
export type PartialClass = PartialAstNode<ClassType>;

/** Partial property type with optional domain properties. */
export type PartialProperty = PartialAstNode<PropertyType>;

/** Partial association type with optional domain properties. */
export type PartialAssociation = PartialAstNode<AssociationType>;

/** Partial association end type with optional domain properties. */
export type PartialAssociationEnd = PartialAstNode<AssociationEndType>;

/** Partial multiplicity type with optional domain properties. */
export type PartialMultiplicity = PartialAstNode<MultiplicityType>;

/** Partial single multiplicity type with optional domain properties. */
export type PartialSingleMultiplicity = PartialAstNode<SingleMultiplicityType>;

/** Partial range multiplicity type with optional domain properties. */
export type PartialRangeMultiplicity = PartialAstNode<RangeMultiplicityType>;

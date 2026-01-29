import type { PartialAstNode } from "@mdeo/language-common";
import type {
    ModelType,
    ObjectInstanceType,
    LinkType,
    LinkEndType,
    PropertyAssignmentType,
    SimpleValueType,
    EnumValueType,
    ListValueType,
    MetamodelFileImportType
} from "./modelTypes.js";

/**
 * Partial model type with optional domain properties.
 */
export type PartialModel = PartialAstNode<ModelType>;

/**
 * Partial object instance type with optional domain properties.
 */
export type PartialObjectInstance = PartialAstNode<ObjectInstanceType>;

/**
 * Partial link type with optional domain properties.
 */
export type PartialLink = PartialAstNode<LinkType>;

/**
 * Partial link end type with optional domain properties.
 */
export type PartialLinkEnd = PartialAstNode<LinkEndType>;

/**
 * Partial property assignment type with optional domain properties.
 */
export type PartialPropertyAssignment = PartialAstNode<PropertyAssignmentType>;

/**
 * Partial simple value type with optional domain properties.
 */
export type PartialSimpleValue = PartialAstNode<SimpleValueType>;

/**
 * Partial enum value type with optional domain properties.
 */
export type PartialEnumValue = PartialAstNode<EnumValueType>;

/**
 * Partial list value type with optional domain properties.
 */
export type PartialListValue = PartialAstNode<ListValueType>;

/**
 * Partial metamodel file import type with optional domain properties.
 */
export type PartialMetamodelFileImport = PartialAstNode<MetamodelFileImportType>;

import {
    createRule,
    or,
    many,
    ref,
    ID,
    STRING,
    INT,
    FLOAT,
    NEWLINE,
    WS,
    ML_COMMENT,
    SL_COMMENT,
    HIDDEN_NEWLINE,
    optional,
    group
} from "@mdeo/language-common";
import { ClassOrImport, EnumEntry, Property } from "@mdeo/language-metamodel";
import {
    SimpleValue,
    EnumValue,
    SingleValue,
    ListValue,
    LiteralValue,
    PropertyAssignment,
    ObjectInstance,
    Model,
    MetamodelFileImport,
    LinkEnd,
    Link
} from "./modelTypes.js";

/**
 * Boolean literal rule.
 * Matches "true" or "false".
 */
export const BOOLEAN = createRule("BOOLEAN")
    .returns(Boolean)
    .as(() => [or("true", "false")]);

/**
 * Simple value rule.
 * Matches string, number, or boolean literals.
 */
export const SimpleValueRule = createRule("SimpleValueRule")
    .returns(SimpleValue)
    .as(({ set }) => [
        or(set("stringValue", STRING), set("numberValue", FLOAT), set("numberValue", INT), set("booleanValue", BOOLEAN))
    ]);

/**
 * Enum value rule.
 * Matches a reference to an enum entry identifier.
 */
export const EnumValueRule = createRule("EnumValueRule")
    .returns(EnumValue)
    .as(({ set }) => [set("value", ref(EnumEntry, ID))]);

/**
 * Single value rule.
 * Matches either a simple value or an enum value.
 */
export const SingleValueRule = createRule("SingleValueRule")
    .returns(SingleValue)
    .as(() => [or(SimpleValueRule, EnumValueRule)]);

/**
 * List value rule.
 * Matches values in square brackets with comma separation.
 */
export const ListValueRule = createRule("ListValueRule")
    .returns(ListValue)
    .as(({ add }) => [
        "[",
        optional(group(add("values", SingleValueRule), many(",", add("values", SingleValueRule)))),
        "]"
    ]);

/**
 * Literal value rule.
 * Matches any value type: simple, enum, or list.
 */
export const LiteralValueRule = createRule("LiteralValueRule")
    .returns(LiteralValue)
    .as(() => [or(ListValueRule, SimpleValueRule, EnumValueRule)]);

/**
 * Property assignment rule.
 * Matches property assignments like "a = 100".
 */
export const PropertyAssignmentRule = createRule("PropertyAssignmentRule")
    .returns(PropertyAssignment)
    .as(({ set }) => [set("name", ref(Property, ID)), "=", set("value", LiteralValueRule)]);

/**
 * Object instance rule.
 * Matches object definitions like "test : ClassName { ... }".
 */
export const ObjectInstanceRule = createRule("ObjectInstanceRule")
    .returns(ObjectInstance)
    .as(({ set, add }) => [
        set("name", ID),
        ":",
        set("class", ref(ClassOrImport, ID)),
        "{",
        many(or(add("properties", PropertyAssignmentRule), NEWLINE)),
        "}"
    ]);

/**
 * Link end rule.
 * Matches an object reference with optional property specification.
 */
export const LinkEndRule = createRule("LinkEndRule")
    .returns(LinkEnd)
    .as(({ set }) => [
        set("object", ref(ObjectInstance, ID)),
        optional(group(".", set("property", ref(Property, ID))))
    ]);

/**
 * Link rule.
 * Matches links between object instances: objectId1[.property] -- objectId2[.property]
 */
export const LinkRule = createRule("LinkRule")
    .returns(Link)
    .as(({ set }) => [set("source", LinkEndRule), "--", set("target", LinkEndRule)]);

/**
 * Metamodel file import rule.
 * Matches "using" statements for importing metamodel files.
 */
export const MetamodelFileImportRule = createRule("MetamodelFileImportRule")
    .returns(MetamodelFileImport)
    .as(({ set }) => ["using", set("file", STRING)]);

/**
 * Model root rule.
 * Matches the complete model file structure.
 */
export const ModelRule = createRule("ModelRule")
    .returns(Model)
    .as(({ add, set }) => [
        many(NEWLINE),
        set("import", MetamodelFileImportRule),
        many(or(add("objects", ObjectInstanceRule), add("links", LinkRule), NEWLINE))
    ]);

/**
 * Additional terminals for the Model language.
 */
export const ModelTerminals = [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT];

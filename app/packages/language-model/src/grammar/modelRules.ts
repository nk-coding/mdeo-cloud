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
    HIDDEN_NEWLINE
} from "@mdeo/language-common";
import { ClassOrImport, Property } from "@mdeo/language-metamodel";
import { LiteralValue, PropertyAssignment, ObjectInstance, Model, MetamodelFileImport } from "./modelTypes.js";

/**
 * Boolean literal rule.
 * Matches "true" or "false".
 */
const BOOLEAN = createRule("BOOLEAN")
    .returns(Boolean)
    .as(() => [or("true", "false")]);

/**
 * Literal value rule.
 * Matches string, number, or boolean literals.
 */
export const LiteralValueRule = createRule("LiteralValueRule")
    .returns(LiteralValue)
    .as(({ set }) => [
        or(set("stringValue", STRING), set("numberValue", FLOAT), set("numberValue", INT), set("booleanValue", BOOLEAN))
    ]);

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
        many(or(add("objects", ObjectInstanceRule), NEWLINE))
    ]);

/**
 * Additional terminals for the Model language.
 */
export const ModelTerminals = [WS, HIDDEN_NEWLINE, ML_COMMENT, SL_COMMENT];

import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-common";
import { ID, STRING, FLOAT, INT } from "@mdeo/language-common";
import { serializeNewlineSep, sharedImport } from "@mdeo/language-shared";
import {
    SimpleValue,
    EnumValue,
    ListValue,
    PropertyAssignment,
    ObjectInstance,
    LinkEnd,
    Link,
    MetamodelFileImport,
    Model,
    type SimpleValueType,
    type EnumValueType,
    type ListValueType,
    type PropertyAssignmentType,
    type ObjectInstanceType,
    type LinkEndType,
    type LinkType,
    type MetamodelFileImportType,
    type ModelType
} from "../grammar/modelTypes.js";

const { doc } = sharedImport("prettier");
const { indent, hardline, group, join } = doc.builders;

/**
 * Registers all model serializers for pretty-printing model AST nodes.
 *
 * @param services The Langium core services with AST serializer
 */
export function registerModelSerializers(services: LangiumCoreServices & AstSerializerAdditionalServices): void {
    const { AstSerializer } = services;

    AstSerializer.registerNodeSerializer(SimpleValue, (ctx) => printSimpleValue(ctx));
    AstSerializer.registerNodeSerializer(EnumValue, (ctx) => printEnumValue(ctx));
    AstSerializer.registerNodeSerializer(ListValue, (ctx) => printListValue(ctx));
    AstSerializer.registerNodeSerializer(PropertyAssignment, (ctx) => printPropertyAssignment(ctx));
    AstSerializer.registerNodeSerializer(ObjectInstance, (ctx) => printObjectInstance(ctx));
    AstSerializer.registerNodeSerializer(LinkEnd, (ctx) => printLinkEnd(ctx));
    AstSerializer.registerNodeSerializer(Link, (ctx) => printLink(ctx));
    AstSerializer.registerNodeSerializer(MetamodelFileImport, (ctx) => printMetamodelFileImport(ctx));
    AstSerializer.registerNodeSerializer(Model, (ctx) => printModel(ctx));
}

/**
 * Prints a simple value node (string, number, or boolean).
 *
 * @param context The print context
 * @returns The formatted simple value
 */
function printSimpleValue(context: PrintContext<SimpleValueType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;

    if (ctx.stringValue != undefined) {
        return printPrimitive(getPrimitive(ctx, "stringValue"), STRING);
    } else if (ctx.numberValue != undefined) {
        const numPrimitive = getPrimitive(ctx, "numberValue");
        const isFloat = numPrimitive.cstNode?.text?.includes(".") ?? false;
        return printPrimitive(numPrimitive, isFloat ? FLOAT : INT);
    } else if (ctx.booleanValue != undefined) {
        return ctx.booleanValue ? "true" : "false";
    }
    return "";
}

/**
 * Prints an enum value node using EnumName.Entry syntax.
 *
 * @param context The print context
 * @returns The formatted enum value
 */
function printEnumValue(context: PrintContext<EnumValueType>): Doc {
    const { path, printReference } = context;
    const enumRef = path.call((ref) => printReference(ref, ID), "enumRef");
    const entry = path.call((ref) => printReference(ref, ID), "value");
    return [enumRef, ".", entry];
}

/**
 * Prints a list value node with square brackets and comma separation.
 *
 * @param context The print context
 * @returns The formatted list value
 */
function printListValue(context: PrintContext<ListValueType>): Doc {
    const { path, print } = context;
    const values = path.map(print, "values");
    return ["[", join(", ", values), "]"];
}

/**
 * Prints a property assignment node.
 *
 * @param context The print context
 * @returns The formatted property assignment
 */
function printPropertyAssignment(context: PrintContext<PropertyAssignmentType>): Doc {
    const { path, print, printReference } = context;
    const name = path.call((ref) => printReference(ref, ID), "name");
    const value = path.call(print, "value");
    return [name, " = ", value];
}

/**
 * Prints an object instance node.
 *
 * @param context The print context
 * @returns The formatted object instance
 */
function printObjectInstance(context: PrintContext<ObjectInstanceType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, printReference } = context;
    const docs: Doc[] = [];

    docs.push(printPrimitive(getPrimitive(ctx, "name"), ID));
    docs.push(": ");
    docs.push(path.call((ref) => printReference(ref, ID), "class"));
    docs.push(" {");

    const content = serializeNewlineSep(context, ["properties"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]), hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints a link end node.
 *
 * @param context The print context
 * @returns The formatted link end
 */
function printLinkEnd(context: PrintContext<LinkEndType>): Doc {
    const { ctx, path, printReference } = context;
    const docs: Doc[] = [];

    docs.push(path.call((ref) => printReference(ref, ID), "object"));

    if (ctx.property != undefined && ctx.property.$refText) {
        docs.push(".");
        docs.push(ctx.property.$refText);
    }

    return docs;
}

/**
 * Prints a link node.
 *
 * @param context The print context
 * @returns The formatted link
 */
function printLink(context: PrintContext<LinkType>): Doc {
    const { path, print } = context;
    const source = path.call(print, "source");
    const target = path.call(print, "target");
    return [source, " -- ", target];
}

/**
 * Prints a metamodel file import node.
 *
 * @param context The print context
 * @returns The formatted import
 */
function printMetamodelFileImport(context: PrintContext<MetamodelFileImportType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return ["using ", printPrimitive(getPrimitive(ctx, "file"), STRING)];
}

/**
 * Prints the root model node.
 *
 * @param context The print context
 * @returns The formatted model
 */
function printModel(context: PrintContext<ModelType>): Doc {
    const { path, print } = context;
    const docs: Doc[] = [];

    docs.push(path.call(print, "import"));
    docs.push(hardline);

    const content = serializeNewlineSep(context, ["objects", "links"], doc.builders);
    if (content.length > 0) {
        docs.push(hardline);
        docs.push(content);
    }

    return docs;
}

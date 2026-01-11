import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-shared";
import { ID, INT } from "@mdeo/language-common";
import { serializeNewlineSep, registerImportSerializers, sharedImport } from "@mdeo/language-shared";
import {
    PrimitiveType,
    SingleMultiplicity,
    RangeMultiplicity,
    Property,
    Class,
    AssociationEnd,
    Association,
    MetaModel,
    ClassImport,
    ClassFileImport,
    type PrimitiveTypeType,
    type SingleMultiplicityType,
    type RangeMultiplicityType,
    type PropertyType,
    type ClassType,
    type AssociationEndType,
    type AssociationType,
    type MetaModelType
} from "../grammar/metamodelTypes.js";

const { doc } = sharedImport("prettier");
const { indent, hardline, group, join } = doc.builders;

/**
 * Registers all metamodel serializers for pretty-printing metamodel AST nodes.
 *
 * @param services The Langium core services with AST serializer
 */
export function registerMetamodelSerializers(services: LangiumCoreServices & AstSerializerAdditionalServices): void {
    const { AstSerializer } = services;

    AstSerializer.registerPrimitiveSerializer(ID, ({ value, cstNode }) => {
        if (cstNode != undefined) {
            return cstNode.text;
        }
        return value;
    });

    AstSerializer.registerPrimitiveSerializer(INT, ({ value, cstNode }) => {
        return cstNode?.text ?? value.toString();
    });

    AstSerializer.registerNodeSerializer(PrimitiveType, (ctx) => printPrimitiveType(ctx));
    AstSerializer.registerNodeSerializer(SingleMultiplicity, (ctx) => printSingleMultiplicity(ctx));
    AstSerializer.registerNodeSerializer(RangeMultiplicity, (ctx) => printRangeMultiplicity(ctx));
    AstSerializer.registerNodeSerializer(Property, (ctx) => printProperty(ctx));
    AstSerializer.registerNodeSerializer(Class, (ctx) => printClass(ctx));
    AstSerializer.registerNodeSerializer(AssociationEnd, (ctx) => printAssociationEnd(ctx));
    AstSerializer.registerNodeSerializer(Association, (ctx) => printAssociation(ctx));
    registerImportSerializers(services, doc.builders, ClassImport, ClassFileImport);
    AstSerializer.registerNodeSerializer(MetaModel, (ctx) => printMetaModel(ctx));
}

/**
 * Prints a primitive type node.
 *
 * @param context The print context
 * @returns The formatted primitive type
 */
function printPrimitiveType(context: PrintContext<PrimitiveTypeType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return printPrimitive(getPrimitive(ctx, "name"), ID);
}

/**
 * Prints a single multiplicity node.
 *
 * @param context The print context
 * @returns The formatted single multiplicity
 */
function printSingleMultiplicity(context: PrintContext<SingleMultiplicityType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    if (ctx.numericValue != undefined) {
        return printPrimitive(getPrimitive(ctx, "numericValue"), INT);
    } else {
        return printPrimitive(getPrimitive(ctx, "value"), ID);
    }
}

/**
 * Prints a range multiplicity node.
 *
 * @param context The print context
 * @returns The formatted range multiplicity
 */
function printRangeMultiplicity(context: PrintContext<RangeMultiplicityType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    const lower = printPrimitive(getPrimitive(ctx, "lower"), INT);
    const upper =
        ctx.upper != undefined
            ? printPrimitive(getPrimitive(ctx, "upper"), ID)
            : printPrimitive(getPrimitive(ctx, "upperNumeric"), INT);
    return [lower, "..", upper];
}

/**
 * Prints a property node.
 *
 * @param context The print context
 * @returns The formatted property
 */
function printProperty(context: PrintContext<PropertyType>): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print } = context;
    const name = printPrimitive(getPrimitive(ctx, "name"), ID);
    const type = path.call(print, "type");
    const docs: Doc[] = [name, ": ", type];

    if (ctx.multiplicity != undefined) {
        docs.push("[", path.call(print, "multiplicity"), "]");
    }

    return docs;
}

/**
 * Prints a class node.
 *
 * @param context The print context
 * @returns The formatted class
 */
function printClass(context: PrintContext<ClassType>): Doc {
    const { ctx, printPrimitive, printReference, getPrimitive, path } = context;
    const docs: Doc[] = [];

    if (ctx.isAbstract) {
        docs.push("abstract ");
    }

    docs.push("class ");
    docs.push(printPrimitive(getPrimitive(ctx, "name"), ID));

    if (ctx.extends.length > 0) {
        docs.push(" extends ");
        const extendsRefs = path.map((ref) => printReference(ref, ID), "extends");
        docs.push(join(", ", extendsRefs));
    }

    docs.push(" {");

    const content = serializeNewlineSep(context, ["properties"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]), hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints an association end node.
 *
 * @param context The print context
 * @returns The formatted association end
 */
function printAssociationEnd(context: PrintContext<AssociationEndType>): Doc {
    const { ctx, path, print, printPrimitive, getPrimitive } = context;
    const docs: Doc[] = [path.call(print, "class", "ref")];

    if (ctx.property != undefined) {
        docs.push(".");
        docs.push(printPrimitive(getPrimitive(ctx, "property"), ID));
    }

    if (ctx.multiplicity != undefined) {
        docs.push("[", path.call(print, "multiplicity"), "]");
    }

    return docs;
}

/**
 * Prints an association node.
 *
 * @param context The print context
 * @returns The formatted association
 */
function printAssociation(context: PrintContext<AssociationType>): Doc {
    const { ctx, path, print, printPrimitive, getPrimitive } = context;
    const start = path.call(print, "start");
    const operator = printPrimitive(getPrimitive(ctx, "operator"), ID);
    const target = path.call(print, "target");

    return [start, " ", operator, " ", target];
}

/**
 * Prints the root metamodel node.
 *
 * @param context The print context
 * @returns The formatted metamodel
 */
function printMetaModel(context: PrintContext<MetaModelType>): Doc {
    return serializeNewlineSep(context, ["imports", "classesAndAssociations"], doc.builders);
}

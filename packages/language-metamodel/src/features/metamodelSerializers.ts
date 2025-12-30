import type { Doc, doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext, PluginContext } from "@mdeo/language-common";
import { ID, INT, printDanglingComments, serializeNewlineSep } from "@mdeo/language-common";
import {
    PrimitiveType,
    SingleMultiplicity,
    RangeMultiplicity,
    Property,
    MetaClass,
    AssociationEnd,
    Association,
    MetaModel,
    MetaClassFileImport,
    type PrimitiveTypeType,
    type SingleMultiplicityType,
    type RangeMultiplicityType,
    type PropertyType,
    type MetaClassType,
    type AssociationEndType,
    type AssociationType,
    type MetaModelType
} from "../grammar/types.js";

/**
 * Registers all metamodel serializers for pretty-printing metamodel AST nodes.
 *
 * @param context The plugin context
 * @param services The Langium core services with AST serializer
 */
export function registerMetamodelSerializers(
    { prettier }: PluginContext,
    services: LangiumCoreServices & AstSerializerAdditionalServices
): void {
    const { AstSerializer } = services;
    const builders = prettier.doc.builders;

    AstSerializer.registerPrimitiveSerializer(ID, ({ value, cstNode }) => {
        if (cstNode != undefined) {
            return cstNode.text;
        }
        return value;
    });

    AstSerializer.registerPrimitiveSerializer(INT, ({ value, cstNode }) => {
        return cstNode?.text ?? value.toString();
    });

    AstSerializer.registerNodeSerializer(PrimitiveType, (ctx) => printPrimitiveType(ctx, builders));
    AstSerializer.registerNodeSerializer(SingleMultiplicity, (ctx) => printSingleMultiplicity(ctx, builders));
    AstSerializer.registerNodeSerializer(RangeMultiplicity, (ctx) => printRangeMultiplicity(ctx, builders));
    AstSerializer.registerNodeSerializer(Property, (ctx) => printProperty(ctx, builders));
    AstSerializer.registerNodeSerializer(MetaClass, (ctx) => printMetaClass(ctx, builders));
    AstSerializer.registerNodeSerializer(AssociationEnd, (ctx) => printAssociationEnd(ctx, builders));
    AstSerializer.registerNodeSerializer(Association, (ctx) => printAssociation(ctx, builders));
    AstSerializer.registerNodeSerializer(MetaClassFileImport, (ctx) => printMetaClassFileImport(ctx, builders));
    AstSerializer.registerNodeSerializer(MetaModel, (ctx) => printMetaModel(ctx, builders));
}

/**
 * Prints a primitive type node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted primitive type
 */
function printPrimitiveType(context: PrintContext<PrimitiveTypeType>, builders: typeof doc.builders): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return printPrimitive(getPrimitive(ctx, "name"), ID);
}

/**
 * Prints a single multiplicity node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted single multiplicity
 */
function printSingleMultiplicity(context: PrintContext<SingleMultiplicityType>, builders: typeof doc.builders): Doc {
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
 * @param builders Prettier doc builders
 * @returns The formatted range multiplicity
 */
function printRangeMultiplicity(context: PrintContext<RangeMultiplicityType>, builders: typeof doc.builders): Doc {
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
 * @param builders Prettier doc builders
 * @returns The formatted property
 */
function printProperty(context: PrintContext<PropertyType>, builders: typeof doc.builders): Doc {
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
 * Prints a metaclass node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted metaclass
 */
function printMetaClass(context: PrintContext<MetaClassType>, builders: typeof doc.builders): Doc {
    const { ctx, printPrimitive, getPrimitive, path, print, options } = context;
    const { join, indent, group, hardline } = builders;
    const docs: Doc[] = [];

    if (ctx.isAbstract) {
        docs.push("abstract ");
    }

    docs.push("class ");
    docs.push(printPrimitive(getPrimitive(ctx, "name"), ID));

    if (ctx.extends.length > 0) {
        docs.push(" extends ");
        const extendsRefs = path.map(print, "extends");
        docs.push(join(", ", extendsRefs));
    }

    docs.push(" {");

    const content = serializeNewlineSep(context, ["properties"], builders);
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
 * @param builders Prettier doc builders
 * @returns The formatted association end
 */
function printAssociationEnd(context: PrintContext<AssociationEndType>, builders: typeof doc.builders): Doc {
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
 * @param builders Prettier doc builders
 * @returns The formatted association
 */
function printAssociation(context: PrintContext<AssociationType>, builders: typeof doc.builders): Doc {
    const { ctx, path, print, printPrimitive, getPrimitive } = context;
    const start = path.call(print, "start");
    const operator = printPrimitive(getPrimitive(ctx, "operator"), ID);
    const target = path.call(print, "target");

    return [start, " ", operator, " ", target];
}

/**
 * Prints a metaclass file import node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted file import
 */
function printMetaClassFileImport(context: PrintContext<any>, builders: typeof doc.builders): Doc {
    const { ctx, path, print, printPrimitive, getPrimitive } = context;
    const { join } = builders;
    const docs: Doc[] = ["import "];

    if (ctx.imports && ctx.imports.length > 0) {
        docs.push("{");
        const imports = path.map(print, "imports");
        docs.push(join(", ", imports));
        docs.push("} from ");
    } else {
        docs.push("* from ");
    }

    docs.push(printPrimitive(getPrimitive(ctx, "uri"), ID));

    return docs;
}

/**
 * Prints the root metamodel node.
 *
 * @param context The print context
 * @param builders Prettier doc builders
 * @returns The formatted metamodel
 */
function printMetaModel(context: PrintContext<MetaModelType>, builders: typeof doc.builders): Doc {
    return serializeNewlineSep(context, ["imports", "classesAndAssociations"], builders)
}

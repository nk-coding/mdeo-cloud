import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-common";
import { ID, STRING, INT } from "@mdeo/language-common";
import { sharedImport, serializeNewlineSep, registerImportSerializers } from "@mdeo/language-shared";
import {
    FunctionImport,
    FunctionFileImport,
    SingleMultiplicity,
    RangeMultiplicity,
    ProblemSection,
    GoalSection,
    ConstraintReference,
    Objective,
    Refinement,
    type SingleMultiplicityType,
    type RangeMultiplicityType,
    type ProblemSectionType,
    type GoalSectionType,
    type ConstraintReferenceType,
    type ObjectiveType,
    type RefinementType
} from "../grammar/optimizationTypes.js";

const { doc } = sharedImport("prettier");
const { indent, hardline, group } = doc.builders;

/**
 * Registers all optimization serializers for pretty-printing AST nodes.
 *
 * @param services The Langium core services with AST serializer
 */
export function registerOptimizationSerializers(services: LangiumCoreServices & AstSerializerAdditionalServices): void {
    const { AstSerializer } = services;

    registerImportSerializers(services, doc.builders, FunctionImport, FunctionFileImport);

    AstSerializer.registerNodeSerializer(SingleMultiplicity, (ctx) => printSingleMultiplicity(ctx));
    AstSerializer.registerNodeSerializer(RangeMultiplicity, (ctx) => printRangeMultiplicity(ctx));

    AstSerializer.registerNodeSerializer(ProblemSection, (ctx) => printProblemSection(ctx));
    AstSerializer.registerNodeSerializer(GoalSection, (ctx) => printGoalSection(ctx));

    AstSerializer.registerNodeSerializer(ConstraintReference, (ctx) => printConstraintReference(ctx));
    AstSerializer.registerNodeSerializer(Objective, (ctx) => printObjective(ctx));
    AstSerializer.registerNodeSerializer(Refinement, (ctx) => printRefinement(ctx));
}

/**
 * Prints a single multiplicity specification.
 *
 * @param context The print context
 * @returns The formatted multiplicity
 */
function printSingleMultiplicity(context: PrintContext<SingleMultiplicityType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;

    if (ctx.value != undefined) {
        return ctx.value;
    }
    if (ctx.numericValue != undefined) {
        return printPrimitive(getPrimitive(ctx, "numericValue"), INT);
    }
    return "";
}

/**
 * Prints a range multiplicity specification.
 *
 * @param context The print context
 * @returns The formatted multiplicity
 */
function printRangeMultiplicity(context: PrintContext<RangeMultiplicityType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    const docs: Doc[] = [];

    docs.push(printPrimitive(getPrimitive(ctx, "lower"), INT));
    docs.push("..");

    if (ctx.upper != undefined) {
        docs.push("*");
    } else if (ctx.upperNumeric != undefined) {
        docs.push(printPrimitive(getPrimitive(ctx, "upperNumeric"), INT));
    }

    return docs;
}

/**
 * Prints a problem section content (without keyword).
 * The keyword is added by the wrapper rule in language-config.
 *
 * @param context The print context
 * @returns The formatted problem section content
 */
function printProblemSection(context: PrintContext<ProblemSectionType>): Doc {
    const { ctx, printPrimitive } = context;
    const docs: Doc[] = [];
    const contentDocs: Doc[] = [];

    if (ctx.metamodel.length > 0) {
        contentDocs.push(["metamodel = ", printPrimitive({ value: ctx.metamodel[0] } as any, STRING)]);
    }
    if (ctx.model.length > 0) {
        contentDocs.push(["model = ", printPrimitive({ value: ctx.model[0] } as any, STRING)]);
    }

    docs.push("{");
    if (contentDocs.length > 0) {
        docs.push(indent([hardline, doc.builders.join(hardline, contentDocs)]));
        docs.push(hardline);
    }
    docs.push("}");

    return group(docs);
}

/**
 * Prints a goal section content (without keyword).
 * The keyword is added by the wrapper rule in language-config.
 *
 * @param context The print context
 * @returns The formatted goal section content
 */
function printGoalSection(context: PrintContext<GoalSectionType>): Doc {
    const docs: Doc[] = [];

    docs.push("{");

    const content = serializeNewlineSep(context, ["imports", "constraints", "objectives", "refinements"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]));
        docs.push(hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints a constraint reference.
 *
 * @param context The print context
 * @returns The formatted constraint reference
 */
function printConstraintReference(context: PrintContext<ConstraintReferenceType>): Doc {
    const { path, printReference } = context;
    return ["constraint ", path.call((ref) => printReference(ref, ID), "constraint")];
}

/**
 * Prints an objective declaration.
 *
 * @param context The print context
 * @returns The formatted objective
 */
function printObjective(context: PrintContext<ObjectiveType>): Doc {
    const { ctx, path, printReference } = context;
    return [ctx.type, " ", path.call((ref) => printReference(ref, ID), "objective")];
}

/**
 * Prints a refinement declaration.
 *
 * @param context The print context
 * @returns The formatted refinement
 */
function printRefinement(context: PrintContext<RefinementType>): Doc {
    const { path, print, printReference } = context;
    const docs: Doc[] = [];

    docs.push("refine ");
    docs.push(path.call((ref) => printReference(ref, ID), "class"));
    docs.push(".");
    docs.push(path.call((ref) => printReference(ref, ID), "field"));
    docs.push("[");
    docs.push(path.call(print, "multiplicity"));
    docs.push("]");

    return docs;
}

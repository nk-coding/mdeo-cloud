import type { Doc } from "prettier";
import type { LangiumCoreServices } from "langium";
import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-common";
import { ID, STRING, INT } from "@mdeo/language-common";
import { sharedImport, serializeNewlineSep } from "@mdeo/language-shared";
import {
    SearchSection,
    SolverSection,
    MutationsBlock,
    UsingPath,
    ClassMutation,
    EdgeMutation,
    MutationStepNumeric,
    MutationStepFixed,
    MutationStepFixedN,
    MutationStepInterval,
    MutationBlock,
    ArchiveBlock,
    AlgorithmParameters,
    TerminationBlock,
    type SearchSectionType,
    type SolverSectionType,
    type MutationsBlockType,
    type UsingPathType,
    type ClassMutationType,
    type EdgeMutationType,
    type MutationStepNumericType,
    type MutationStepFixedNType,
    type MutationStepIntervalType,
    type MutationBlockType,
    type ArchiveBlockType,
    type AlgorithmParametersType,
    type TerminationBlockType
} from "../grammar/mdeoTypes.js";

const { doc } = sharedImport("prettier");
const { indent, hardline, group } = doc.builders;

/**
 * Registers all MDEO serializers for pretty-printing AST nodes.
 *
 * @param services The Langium core services with AST serializer
 */
export function registerMdeoSerializers(services: LangiumCoreServices & AstSerializerAdditionalServices): void {
    const { AstSerializer } = services;

    AstSerializer.registerNodeSerializer(SearchSection, (ctx) => printSearchSection(ctx));
    AstSerializer.registerNodeSerializer(MutationsBlock, (ctx) => printMutationsBlock(ctx));
    AstSerializer.registerNodeSerializer(UsingPath, (ctx) => printUsingPath(ctx));
    AstSerializer.registerNodeSerializer(ClassMutation, (ctx) => printClassMutation(ctx));
    AstSerializer.registerNodeSerializer(EdgeMutation, (ctx) => printEdgeMutation(ctx));
    AstSerializer.registerNodeSerializer(SolverSection, (ctx) => printSolverSection(ctx));
    AstSerializer.registerNodeSerializer(AlgorithmParameters, (ctx) => printAlgorithmParameters(ctx));
    AstSerializer.registerNodeSerializer(TerminationBlock, (ctx) => printTerminationBlock(ctx));
    AstSerializer.registerNodeSerializer(MutationBlock, (ctx) => printMutationBlock(ctx));
    AstSerializer.registerNodeSerializer(ArchiveBlock, (ctx) => printArchiveBlock(ctx));
    AstSerializer.registerNodeSerializer(MutationStepNumeric, (ctx) => printMutationStepNumeric(ctx));
    AstSerializer.registerNodeSerializer(MutationStepFixed, () => "fixed");
    AstSerializer.registerNodeSerializer(MutationStepFixedN, (ctx) => printMutationStepFixedN(ctx));
    AstSerializer.registerNodeSerializer(MutationStepInterval, (ctx) => printMutationStepInterval(ctx));
}

/**
 * Prints a search section content.
 *
 * @param context The print context
 * @returns The formatted search section content
 */
function printSearchSection(context: PrintContext<SearchSectionType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];

    docs.push("{");

    if (ctx.mutations.length > 0) {
        docs.push(indent([hardline, path.call(print, "mutations", 0)]));
        docs.push(hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints the solver section content.
 */
function printSolverSection(context: PrintContext<SolverSectionType>): Doc {
    const { ctx, path, print, printPrimitive } = context;
    const docs: Doc[] = [];
    docs.push("{");

    const contentDocs: Doc[] = [];
    if (ctx.algorithm.length > 0) {
        contentDocs.push(["algorithm = ", ctx.algorithm[0]]);
    }
    if (ctx.parameters.length > 0) {
        contentDocs.push(path.call(print, "parameters", 0));
    }
    if (ctx.termination.length > 0) {
        contentDocs.push(path.call(print, "termination", 0));
    }
    if (ctx.batches.length > 0) {
        contentDocs.push(["batches = ", printPrimitive({ value: ctx.batches[0] }, INT)]);
    }

    if (contentDocs.length > 0) {
        docs.push(indent([hardline, doc.builders.join(hardline, contentDocs)]));
        docs.push(hardline);
    }
    docs.push("}");
    return group(docs);
}

/**
 * Prints the parameters block.
 */
function printAlgorithmParameters(context: PrintContext<AlgorithmParametersType>): Doc {
    const { ctx, path, print, printPrimitive } = context;
    const docs: Doc[] = [];
    docs.push("parameters {");

    const contentDocs: Doc[] = [];
    if (ctx.population.length > 0)
        contentDocs.push(["population = ", printPrimitive({ value: ctx.population[0] }, INT)]);
    if (ctx.variation.length > 0) contentDocs.push(["variation = ", ctx.variation[0]]);
    if (ctx.mutation.length > 0) contentDocs.push(path.call(print, "mutation", 0));
    if (ctx.bisections.length > 0)
        contentDocs.push(["bisections = ", printPrimitive({ value: ctx.bisections[0] }, INT)]);
    if (ctx.archive.length > 0) contentDocs.push(path.call(print, "archive", 0));

    if (contentDocs.length > 0) {
        docs.push(indent([hardline, doc.builders.join(hardline, contentDocs)]));
        docs.push(hardline);
    }
    docs.push("}");
    return group(docs);
}

/**
 * Prints the termination block.
 */
function printTerminationBlock(context: PrintContext<TerminationBlockType>): Doc {
    const { ctx, printPrimitive } = context;
    const docs: Doc[] = [];
    docs.push("termination {");

    const contentDocs: Doc[] = [];
    if (ctx.evolutions.length > 0)
        contentDocs.push(["evolutions = ", printPrimitive({ value: ctx.evolutions[0] }, INT)]);
    if (ctx.time.length > 0) contentDocs.push(["time = ", printPrimitive({ value: ctx.time[0] }, INT)]);
    if (ctx.delta.length > 0) contentDocs.push(["delta = ", printPrimitive({ value: ctx.delta[0] }, INT)]);
    if (ctx.iterations.length > 0)
        contentDocs.push(["iterations = ", printPrimitive({ value: ctx.iterations[0] }, INT)]);

    if (contentDocs.length > 0) {
        docs.push(indent([hardline, doc.builders.join(hardline, contentDocs)]));
        docs.push(hardline);
    }
    docs.push("}");
    return group(docs);
}

/**
 * Prints the mutation sub-block.
 */
function printMutationBlock(context: PrintContext<MutationBlockType>): Doc {
    const { ctx, path, print } = context;
    const docs: Doc[] = [];
    docs.push("mutation {");

    const contentDocs: Doc[] = [];
    if (ctx.step.length > 0) contentDocs.push(["step = ", path.call(print, "step", 0)]);
    if (ctx.strategy.length > 0) contentDocs.push(["strategy = ", ctx.strategy[0]]);
    if (ctx.selection.length > 0) contentDocs.push(["selection = ", ctx.selection[0]]);
    if (ctx.application.length > 0) contentDocs.push(["application = ", ctx.application[0]]);
    if (ctx.credit.length > 0) contentDocs.push(["credit = ", ctx.credit[0]]);
    if (ctx.repair.length > 0) contentDocs.push(["repair = ", ctx.repair[0]]);

    if (contentDocs.length > 0) {
        docs.push(indent([hardline, doc.builders.join(hardline, contentDocs)]));
        docs.push(hardline);
    }
    docs.push("}");
    return group(docs);
}

/**
 * Prints the archive sub-block (PESA2/PAES only).
 */
function printArchiveBlock(context: PrintContext<ArchiveBlockType>): Doc {
    const { ctx, printPrimitive } = context;
    const docs: Doc[] = [];
    docs.push("archive {");

    if (ctx.size.length > 0) {
        docs.push(indent([hardline, ["size = ", printPrimitive({ value: ctx.size[0] }, INT)]]));
        docs.push(hardline);
    }
    docs.push("}");
    return group(docs);
}

/**
 * Prints a numeric step size.
 * Example: 3
 */
function printMutationStepNumeric(context: PrintContext<MutationStepNumericType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return printPrimitive(getPrimitive(ctx, "value"), INT);
}

/**
 * Prints a fixed(N) step size.
 * Example: fixed(3)
 */
function printMutationStepFixedN(context: PrintContext<MutationStepFixedNType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return ["fixed(", printPrimitive(getPrimitive(ctx, "n"), INT), ")"];
}

/**
 * Prints an interval(lower, upper) step size.
 * Example: interval(1, 5)
 */
function printMutationStepInterval(context: PrintContext<MutationStepIntervalType>): Doc {
    const { ctx, printPrimitive, getPrimitive } = context;
    return [
        "interval(",
        printPrimitive(getPrimitive(ctx, "lower"), INT),
        ", ",
        printPrimitive(getPrimitive(ctx, "upper"), INT),
        ")"
    ];
}

/**
 * Prints a mutations block.
 *
 * @param context The print context
 * @returns The formatted mutations block
 */
function printMutationsBlock(context: PrintContext<MutationsBlockType>): Doc {
    const docs: Doc[] = [];

    docs.push("mutations {");

    const content = serializeNewlineSep(context, ["usingPaths", "classMutations", "edgeMutations"], doc.builders);
    if (content.length > 0) {
        docs.push(indent([hardline, content]));
        docs.push(hardline);
    }

    docs.push("}");

    return group(docs);
}

/**
 * Prints a using path declaration.
 *
 * @param context The print context
 * @returns The formatted using path
 */
function printUsingPath(context: PrintContext<UsingPathType>): Doc {
    const { printPrimitive, getPrimitive, ctx } = context;
    return ["using ", printPrimitive(getPrimitive(ctx, "path"), STRING)];
}

/**
 * Prints a class mutation declaration.
 *
 * @param context The print context
 * @returns The formatted class mutation
 */
function printClassMutation(context: PrintContext<ClassMutationType>): Doc {
    const { ctx, path, printReference } = context;
    return [ctx.operator, " ", path.call((ref) => printReference(ref, ID), "class")];
}

/**
 * Prints an edge mutation declaration.
 *
 * @param context The print context
 * @returns The formatted edge mutation
 */
function printEdgeMutation(context: PrintContext<EdgeMutationType>): Doc {
    const { ctx, path, printReference } = context;
    return [
        ctx.operator,
        " ",
        path.call((ref) => printReference(ref, ID), "class"),
        ".",
        path.call((ref) => printReference(ref, ID), "edge")
    ];
}

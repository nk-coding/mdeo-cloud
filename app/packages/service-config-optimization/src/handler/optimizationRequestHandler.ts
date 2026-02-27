import type { RequestHandler } from "@mdeo/service-common";
import { hasErrors } from "@mdeo/service-common";
import type { ExternalReferenceAdditionalServices } from "@mdeo/language-common";
import { getWrapperInterfaceName } from "@mdeo/language-config";
import type { ConfigPluginRequestBody } from "@mdeo/language-config";
import { CONFIG_OPTIMIZATION_LANGUAGE_KEY, OPTIMIZATION_PLUGIN_NAME } from "@mdeo/language-config-optimization";
import type {
    ProblemSectionType,
    GoalSectionType,
    SingleMultiplicityType,
    RangeMultiplicityType
} from "@mdeo/language-config-optimization";
import { TextDocument } from "langium";
import type { LangiumDocument } from "langium";
import type {
    ProblemSectionData,
    GoalSectionData,
    OptimizationRequestResponse,
    ConstraintData,
    ObjectiveData,
    RefinementData
} from "./optimizationRequestTypes.js";
import { resolveRelativePath } from "@mdeo/language-shared";

/**
 * Key used for the optimization plugin request handler.
 * Must match the key used by the config file-data handler to call this plugin.
 */
export const OPTIMIZATION_REQUEST_KEY = "config";

function extractProblemData(section: ProblemSectionType, document: LangiumDocument): ProblemSectionData {
    const metamodel = section.metamodel[0];
    const model = section.model[0];
    return {
        metamodelPath: metamodel ? resolveRelativePath(document, metamodel).fsPath : "",
        modelPath: model ? resolveRelativePath(document, model).fsPath : ""
    };
}

function extractMultiplicityBounds(mult: any): { lower: number; upper: number } {
    if (mult == undefined) {
        return { lower: 1, upper: 1 };
    }
    if ((mult.$type as string | undefined)?.includes("Range")) {
        const range = mult as RangeMultiplicityType;
        const upper = range.upper === "*" ? -1 : (range.upperNumeric ?? 0);
        return { lower: range.lower ?? 0, upper };
    }
    const single = mult as SingleMultiplicityType;
    if (single.value === "*") return { lower: 0, upper: -1 };
    if (single.value === "+") return { lower: 1, upper: -1 };
    if (single.value === "?") return { lower: 0, upper: 1 };
    const n = single.numericValue;
    if (n != undefined) return { lower: n, upper: n };
    return { lower: 1, upper: 1 };
}

/**
 * Builds a lookup map from the textual reference name used in the config source
 * (may be an alias) to the absolute path of the providing script file and the
 * original function name declared in that file.
 */
function buildFunctionLookup(
    section: GoalSectionType,
    document: LangiumDocument
): Map<string, { path: string; functionName: string }> {
    const lookup = new Map<string, { path: string; functionName: string }>();
    for (const fileImport of (section.imports ?? []) as any[]) {
        const relativePath: string = fileImport.file ?? "";
        const absolutePath = resolveRelativePath(document, relativePath).fsPath;
        for (const imp of (fileImport.imports ?? []) as any[]) {
            const originalName: string = imp.entity?.$refText ?? "";
            const refName: string = imp.name != undefined ? imp.name : originalName;
            lookup.set(refName, { path: absolutePath, functionName: originalName });
        }
    }
    return lookup;
}

function extractGoalData(section: GoalSectionType, document: LangiumDocument): GoalSectionData {
    const functionLookup = buildFunctionLookup(section, document);

    const constraints: ConstraintData[] = (section.constraints ?? []).map((c: any) => {
        const refText: string = c.constraint?.$refText ?? "";
        const resolved = functionLookup.get(refText);
        return {
            path: resolved?.path ?? "",
            functionName: resolved?.functionName ?? refText
        };
    });

    const objectives: ObjectiveData[] = (section.objectives ?? []).map((o: any) => {
        const refText: string = o.objective?.$refText ?? "";
        const resolved = functionLookup.get(refText);
        return {
            type: (o.type === "maximize" ? "MAXIMIZE" : "MINIMIZE") as "MAXIMIZE" | "MINIMIZE",
            path: resolved?.path ?? "",
            functionName: resolved?.functionName ?? refText
        };
    });

    const refinements: RefinementData[] = (section.refinements ?? []).map((r: any) => {
        const bounds = extractMultiplicityBounds(r.multiplicity);
        return {
            className: r.class?.$refText ?? "",
            fieldName: r.field?.$refText ?? "",
            lower: bounds.lower,
            upper: bounds.upper
        };
    });

    return { constraints, objectives, refinements };
}

/**
 * Request handler for the optimization contribution plugin.
 * Receives a partial config text (containing only optimization sections) and
 * dependency data from plugins this plugin depends on, parses the text using the
 * optimization language services, and extracts structured data for each section.
 *
 * Returns `null` when the text contains lexer or parser errors, mirroring the
 * null-return behaviour of the file-data handlers.
 *
 * The handler is invoked by the config service's file-data handler via the backend
 * plugin request proxy.
 *
 * Response format: `{ problem?: ProblemSectionData, goal?: GoalSectionData }` or `null`
 */
export const optimizationRequestHandler: RequestHandler<
    OptimizationRequestResponse,
    ExternalReferenceAdditionalServices
> = async (context): Promise<OptimizationRequestResponse | null> => {
    const requestBody = context.body as ConfigPluginRequestBody;
    const text = requestBody?.text ?? "";

    if (!text.trim()) {
        return {};
    }

    const textDocument = TextDocument.create(requestBody.configFileUri, CONFIG_OPTIMIZATION_LANGUAGE_KEY, 0, text);
    context.services.shared.workspace.TextDocuments.set(textDocument);
    const document = context.services.shared.workspace.LangiumDocumentFactory.fromTextDocument(textDocument);
    await context.services.shared.workspace.DocumentBuilder.build([document], { validation: true });
    if (hasErrors(document)) {
        return null;
    }

    const root = document.parseResult?.value as { sections?: any[] } | undefined;
    if (root == undefined || !Array.isArray(root.sections)) {
        return {};
    }

    const response: OptimizationRequestResponse = {};

    const problemWrapperType = getWrapperInterfaceName("problem", OPTIMIZATION_PLUGIN_NAME);
    const goalWrapperType = getWrapperInterfaceName("goal", OPTIMIZATION_PLUGIN_NAME);

    for (const section of root.sections) {
        if (section.$type === problemWrapperType) {
            response.problem = extractProblemData(section.content as ProblemSectionType, document);
        } else if (section.$type === goalWrapperType) {
            response.goal = extractGoalData(section.content as GoalSectionType, document);
        }
    }

    return response;
};

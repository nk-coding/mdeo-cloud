import type { RequestHandler } from "@mdeo/service-common";
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
import type {
    ProblemSectionData,
    GoalSectionData,
    OptimizationRequestResponse,
    MultiplicityData,
    FunctionFileImportData,
    FunctionImportData,
    ConstraintData,
    ObjectiveData,
    RefinementData
} from "./optimizationRequestTypes.js";

/**
 * Key used for the optimization plugin request handler.
 * Must match the key used by the config file-data handler to call this plugin.
 */
export const OPTIMIZATION_REQUEST_KEY = "config";

function extractProblemData(section: ProblemSectionType): ProblemSectionData {
    return {
        metamodel: section.metamodel ?? "",
        model: section.model ?? ""
    };
}

function extractMultiplicity(mult: any): MultiplicityData {
    if (mult == undefined) {
        return { kind: "single", value: 1 };
    }
    if (mult.$type?.includes("Range")) {
        const range = mult as RangeMultiplicityType;
        const upper: "*" | number = range.upper != undefined ? "*" : (range.upperNumeric ?? 0);
        return { kind: "range", lower: range.lower ?? 0, upper };
    }
    const single = mult as SingleMultiplicityType;
    return {
        kind: "single",
        value: single.value != undefined ? single.value : (single.numericValue ?? 1)
    };
}

function extractGoalData(section: GoalSectionType): GoalSectionData {
    const imports: FunctionFileImportData[] = (section.imports ?? []).map((fileImport: any) => ({
        file: fileImport.file ?? "",
        imports: (fileImport.imports ?? []).map((imp: any) => {
            const entry: FunctionImportData = { entity: imp.entity?.$refText ?? "" };
            if (imp.name != undefined) entry.alias = imp.name;
            return entry;
        })
    }));

    const constraints: ConstraintData[] = (section.constraints ?? []).map((c: any) => ({
        functionName: c.constraint?.$refText ?? ""
    }));

    const objectives: ObjectiveData[] = (section.objectives ?? []).map((o: any) => ({
        type: (o.type === "maximize" ? "maximize" : "minimize") as "maximize" | "minimize",
        functionName: o.objective?.$refText ?? ""
    }));

    const refinements: RefinementData[] = (section.refinements ?? []).map((r: any) => ({
        className: r.class?.$refText ?? "",
        fieldName: r.field?.$refText ?? "",
        multiplicity: extractMultiplicity(r.multiplicity)
    }));

    return { imports, constraints, objectives, refinements };
}

/**
 * Request handler for the optimization contribution plugin.
 * Receives a partial config text (containing only optimization sections) and
 * dependency data from plugins this plugin depends on, parses the text using the
 * optimization language services, and extracts structured data for each section.
 *
 * The handler is invoked by the config service's file-data handler via the backend
 * plugin request proxy.
 *
 * Response format: `{ problem?: ProblemSectionData, goal?: GoalSectionData }`
 */
export const optimizationRequestHandler: RequestHandler<
    OptimizationRequestResponse,
    ExternalReferenceAdditionalServices
> = async (context): Promise<OptimizationRequestResponse> => {
    const requestBody = context.body as ConfigPluginRequestBody;
    const text = requestBody?.text ?? "";

    if (!text.trim()) {
        return {};
    }

    const textDocument = TextDocument.create(requestBody.configFileUri, CONFIG_OPTIMIZATION_LANGUAGE_KEY, 0, text);
    context.services.shared.workspace.TextDocuments.set(textDocument)
    const document = context.services.shared.workspace.LangiumDocumentFactory.fromTextDocument(textDocument);

    const root = document.parseResult?.value as { sections?: any[] } | undefined;
    if (root == undefined || !Array.isArray(root.sections)) {
        return {};
    }

    const response: OptimizationRequestResponse = {};

    const problemWrapperType = getWrapperInterfaceName("problem", OPTIMIZATION_PLUGIN_NAME);
    const goalWrapperType = getWrapperInterfaceName("goal", OPTIMIZATION_PLUGIN_NAME);

    for (const section of root.sections) {
        if (section.$type === problemWrapperType) {
            response.problem = extractProblemData(section.content as ProblemSectionType);
        } else if (section.$type === goalWrapperType) {
            response.goal = extractGoalData(section.content as GoalSectionType);
        }
    }

    return response;
};

import type { RequestHandler } from "@mdeo/service-common";
import { hasErrors } from "@mdeo/service-common";
import { getWrapperInterfaceName } from "@mdeo/language-config";
import type { ConfigPluginRequestBody } from "@mdeo/language-config";
import { CONFIG_MDEO_LANGUAGE_KEY, MDEO_PLUGIN_NAME } from "@mdeo/language-config-mdeo";
import type {
    SearchSectionType,
    SolverSectionType,
    MutationsBlockType,
    UsingPathType,
    ClassMutationType,
    EdgeMutationType,
    MutationStepValueType,
    MutationBlockType,
    ArchiveBlockType,
    AlgorithmParametersType,
    TerminationBlockType
} from "@mdeo/language-config-mdeo";
import { TextDocument, URI } from "langium";
import type { LangiumDocument } from "langium";
import { resolveRelativePath } from "@mdeo/language-shared";
import type {
    SearchSectionData,
    SolverSectionData,
    MdeoRequestResponse,
    MutationsBlockData,
    UsingPathData,
    ClassMutationData,
    EdgeMutationData,
    MutationStepValueData,
    MutationBlockData,
    ArchiveBlockData,
    AlgorithmParametersData,
    TerminationBlockData
} from "./mdeoRequestTypes.js";
import type { MdeoServices } from "@mdeo/language-config-mdeo";
import type { ServiceMdeoMetamodelResolver } from "../serviceMdeoMetamodelResolver.js";

/**
 * Key used for the MDEO plugin request handler.
 * Must match the key used by the config file-data handler to call this plugin.
 */
export const MDEO_REQUEST_KEY = "config";

/**
 * Extracts using path data.
 *
 * @param usingPath The using path node
 * @param document The config Langium document
 * @returns The extracted data
 */
function extractUsingPath(usingPath: UsingPathType, document: LangiumDocument): UsingPathData {
    const relativePath = usingPath.path ?? "";
    return {
        path: resolveRelativePath(document, relativePath).fsPath
    };
}

/**
 * Extracts class mutation data.
 *
 * @param mutation The class mutation node
 * @returns The extracted data
 */
function extractClassMutation(mutation: ClassMutationType): ClassMutationData {
    return {
        operator: mutation.operator as "create" | "delete" | "mutate",
        className: mutation.class?.$refText ?? ""
    };
}

/**
 * Extracts edge mutation data.
 *
 * @param mutation The edge mutation node
 * @returns The extracted data
 */
function extractEdgeMutation(mutation: EdgeMutationType): EdgeMutationData {
    return {
        operator: mutation.operator as "add" | "remove" | "mutate",
        className: mutation.class?.$refText ?? "",
        edgeName: mutation.edge?.$refText ?? ""
    };
}

/**
 * Extracts mutations block data.
 *
 * @param mutations The mutations block node
 * @param document The config Langium document
 * @returns The extracted data
 */
function extractMutationsBlock(mutations: MutationsBlockType, document: LangiumDocument): MutationsBlockData {
    return {
        usingPaths: (mutations.usingPaths ?? []).map((up) => extractUsingPath(up as UsingPathType, document)),
        classMutations: (mutations.classMutations ?? []).map((cm) => extractClassMutation(cm as ClassMutationType)),
        edgeMutations: (mutations.edgeMutations ?? []).map((em) => extractEdgeMutation(em as EdgeMutationType))
    };
}

/**
 * Extracts search section data.
 *
 * @param section The search section node
 * @param document The config Langium document
 * @returns The extracted data
 */
function extractSearchData(section: SearchSectionType, document: LangiumDocument): SearchSectionData {
    const mutations = section.mutations[0];
    return {
        mutations: mutations ? extractMutationsBlock(mutations as MutationsBlockType, document) : undefined
    };
}

/**
 * Extracts mutation step value data from the AST union node.
 */
function extractMutationStepValue(step: MutationStepValueType): MutationStepValueData {
    const typed = step as unknown as { $type: string; value?: number; n?: number; lower?: number; upper?: number };
    switch (typed.$type) {
        case "ConfigMdeoMutationStepNumeric":
            return { kind: "numeric", value: typed.value ?? 0 };
        case "ConfigMdeoMutationStepFixed":
            return { kind: "fixed" };
        case "ConfigMdeoMutationStepFixedN":
            return { kind: "fixedN", n: typed.n ?? 0 };
        case "ConfigMdeoMutationStepInterval":
            return { kind: "interval", lower: typed.lower ?? 0, upper: typed.upper ?? 0 };
        default:
            return { kind: "fixed" };
    }
}

/**
 * Extracts mutation block data.
 */
function extractMutationBlock(mutation: MutationBlockType): MutationBlockData {
    const step = mutation.step[0];
    return {
        step: step ? extractMutationStepValue(step as MutationStepValueType) : undefined,
        strategy: mutation.strategy[0] as MutationBlockData["strategy"],
        selection: mutation.selection[0] as MutationBlockData["selection"],
        application: mutation.application[0] as MutationBlockData["application"],
        credit: mutation.credit[0] as MutationBlockData["credit"],
        repair: mutation.repair[0] as MutationBlockData["repair"]
    };
}

/**
 * Extracts archive block data.
 */
function extractArchiveBlock(archive: ArchiveBlockType): ArchiveBlockData {
    return { size: archive.size[0] };
}

/**
 * Extracts algorithm parameters block data.
 */
function extractAlgorithmParameters(params: AlgorithmParametersType): AlgorithmParametersData {
    const mutation = params.mutation[0];
    const archive = params.archive[0];
    return {
        population: params.population[0],
        variation: params.variation[0] as AlgorithmParametersData["variation"],
        mutation: mutation ? extractMutationBlock(mutation as MutationBlockType) : undefined,
        bisections: params.bisections[0],
        archive: archive ? extractArchiveBlock(archive as ArchiveBlockType) : undefined
    };
}

/**
 * Extracts termination block data.
 */
function extractTerminationBlock(termination: TerminationBlockType): TerminationBlockData {
    return {
        evolutions: termination.evolutions[0],
        time: termination.time[0],
        delta: termination.delta[0],
        iterations: termination.iterations[0]
    };
}

/**
 * Extracts solver section data.
 *
 * @param section The solver section node
 * @returns The extracted data
 */
function extractSolverData(section: SolverSectionType): SolverSectionData {
    const parameters = section.parameters[0];
    const termination = section.termination[0];
    return {
        provider: section.provider[0] as SolverSectionData["provider"],
        algorithm: section.algorithm[0] as SolverSectionData["algorithm"],
        parameters: parameters ? extractAlgorithmParameters(parameters as AlgorithmParametersType) : undefined,
        termination: termination ? extractTerminationBlock(termination as TerminationBlockType) : undefined,
        batches: section.batches[0]
    };
}

/**
 * Request handler for the MDEO contribution plugin.
 * Receives a partial config text (containing only MDEO sections) and
 * parses the text using the MDEO language services, extracting structured data.
 *
 * Returns `null` when the text contains lexer or parser errors.
 */
export const mdeoRequestHandler: RequestHandler<MdeoRequestResponse, MdeoServices> = async (
    context
): Promise<MdeoRequestResponse | null> => {
    const requestBody = context.body as ConfigPluginRequestBody;
    const text = requestBody?.text ?? "";

    if (!text.trim()) {
        return {};
    }

    const resolver = context.services.MdeoMetamodelResolver as ServiceMdeoMetamodelResolver;
    const problemData = (requestBody.dependencyData?.["optimization"]?.["problem"] ?? {}) as {
        metamodel?: string;
    };
    if (problemData.metamodel) {
        resolver.setMetamodelData(URI.file(problemData.metamodel));
    }

    const textDocument = TextDocument.create(requestBody.configFileUri, CONFIG_MDEO_LANGUAGE_KEY, 0, text);
    context.services.shared.workspace.TextDocuments.set(textDocument);
    const document = context.services.shared.workspace.LangiumDocumentFactory.fromTextDocument(textDocument);
    try {
        await context.services.shared.workspace.DocumentBuilder.build([document], { validation: true });
        if (hasErrors(document)) {
            return null;
        }
    } finally {
        resolver.clearMetamodelData();
    }

    const root = document.parseResult?.value as { sections?: any[] } | undefined;
    if (root == undefined || !Array.isArray(root.sections)) {
        return {};
    }

    const response: MdeoRequestResponse = {};

    const searchWrapperType = getWrapperInterfaceName("search", MDEO_PLUGIN_NAME);
    const solverWrapperType = getWrapperInterfaceName("solver", MDEO_PLUGIN_NAME);

    for (const section of root.sections) {
        if (section.$type === searchWrapperType) {
            response.search = extractSearchData(section.content as SearchSectionType, document);
        } else if (section.$type === solverWrapperType) {
            response.solver = extractSolverData(section.content as SolverSectionType);
        }
    }

    return response;
};

import { URI, type LangiumDocument } from "langium";
import type { FileDataHandler, FileDataResult } from "./types.js";
import { type AstData } from "../langium/jsonAstSerializer.js";
import type { ExternalReferenceAdditionalServices, LanguageServices } from "@mdeo/language-common";
import type { TrackedRequests } from "../service/serverApi.js";
import type { ServiceAdditionalServices } from "../langium/types.js";

/**
 * Key for the AST handler
 */
export const AST_HANDLER_KEY = "ast";

/**
 * Handler for computing the AST of a metamodel file.
 * Parses the document, builds it, and serializes the AST to JSON.
 *
 * @param context The file data context with path, content, and services
 * @returns Promise resolving to the file data result with serialized AST
 */
export const astHandler: FileDataHandler<AstData, ExternalReferenceAdditionalServices> = async (context) => {
    const { uri, instance, services, serverApi } = context;

    const document = await instance.buildDocument(uri);

    const trackedRequests = serverApi.getTrackedRequests();

    for (const dependency of trackedRequests.dataDependencies) {
        if (dependency.key !== AST_HANDLER_KEY) {
            throw new Error(`Unexpected data dependency key: ${dependency.key}`);
        }
    }

    const trackedRequestsLookup: TrackedRequestsLookup = {
        fileDependencies: new Map(trackedRequests.fileDependencies.map((dependency) => [dependency.path, dependency])),
        dataDependencies: new Map(trackedRequests.dataDependencies.map((dependency) => [dependency.path, dependency]))
    };

    const additionalDocuments = trackedRequests.fileDependencies
        .map((dependency) => services.shared.workspace.LangiumDocuments.getDocument(URI.file(dependency.path)))
        .filter((doc) => doc != undefined);

    return {
        ...serializeDocument(document, services, trackedRequestsLookup),
        additionalFileData: additionalDocuments.map((doc) => ({
            ...serializeDocument(doc, services, trackedRequestsLookup),
            path: doc.uri.path,
            key: AST_HANDLER_KEY,
            sourceVersion: trackedRequestsLookup.fileDependencies.get(doc.uri.path)?.version ?? -1
        }))
    };
};

/**
 * Serializes a Langium document into AST data, including tracking dependencies.
 *
 * @param document the Langium document to serialize
 * @param services the language services
 * @param trackedRequests the tracked requests lookup
 * @returns the file data result containing the serialized AST and dependencies
 */
function serializeDocument(
    document: LangiumDocument,
    services: LanguageServices & ServiceAdditionalServices & ExternalReferenceAdditionalServices,
    trackedRequests: TrackedRequestsLookup
): Omit<FileDataResult<AstData>, "additionalFileData"> {
    const externalReferenceCollector = services.references.ExternalReferenceCollector;
    const serializer = services.shared.serializer.JsonAstSerializer;

    const serializedAst = serializer.serialize(document);
    const references = externalReferenceCollector.findExternalReferences([document]);

    const astData: AstData = {
        hasErrors:
            document.parseResult.lexerErrors.length > 0 ||
            document.parseResult.parserErrors.length > 0 ||
            (document.diagnostics != undefined && document.diagnostics.length > 0),
        ast: serializedAst,
        path: document.uri.path,
        linkDependencies: [...references.local.map((uri) => uri.path), ...references.external.map((uri) => uri.path)],
        exportedElements: serializer.serializeExportedElements(document)
    };

    const result: Omit<FileDataResult<AstData>, "additionalFileData"> = {
        data: astData,
        fileDependencies: [],
        dataDependencies: []
    };

    for (const ref of references.local) {
        const fileDep = trackedRequests.fileDependencies.get(ref.path);
        if (fileDep != undefined) {
            result.fileDependencies.push(fileDep);
        } else {
            throw new Error(`Missing tracked request for local reference: ${ref.path}`);
        }
    }

    for (const ref of references.external) {
        const dataDep = trackedRequests.dataDependencies.get(ref.path);
        if (dataDep != undefined) {
            result.dataDependencies.push(dataDep);
        } else {
            throw new Error(`Missing tracked request for external reference: ${ref.path}`);
        }
    }

    return result;
}

/**
 * Tracked requests lookup data structure
 */
type TrackedRequestsLookup = {
    [Property in keyof TrackedRequests]: Map<string, TrackedRequests[Property][number]>;
};

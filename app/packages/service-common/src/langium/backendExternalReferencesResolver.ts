import type { ExternalReferenceResolver } from "@mdeo/language-common";
import { DocumentState, type LangiumDocuments, type LangiumSharedCoreServices } from "langium";
import type { URI } from "vscode-uri";
import type {  ServiceAdditionalSharedServices } from "./types.js";
import type { ServerApi } from "../service/serverApi.js";
import type { CancellationTokenSource } from "vscode-jsonrpc";

/**
 * External reference resolver that utilizes backend services to resolve references
 */
export class BackendExternalReferencesResolver implements ExternalReferenceResolver {

    /**
     * The server API for backend communication
     */
    protected readonly serverApi: ServerApi;

    /**
     * The Langium documents collection
     */
    protected readonly langiumDocuments: LangiumDocuments;

    /**
     * The cancellation token source for managing request cancellation
     * Does a cancel when loading external documents
     */
    private cancelationTokenSource: CancellationTokenSource | undefined;

    /**
     * Creates a new backend external references resolver
     * 
     * @param services The combined Langium core and service additional services
     */
    constructor(services: LangiumSharedCoreServices & ServiceAdditionalSharedServices) {
        this.serverApi = services.ServerApi;
        this.langiumDocuments = services.workspace.LangiumDocuments;
    }

    async loadExternalDocument(uri: URI): Promise<void> {
        const source = await this.serverApi.readFile(uri.fsPath)
        this.langiumDocuments.createDocument(uri, source.content)
        this.cancelationTokenSource?.cancel();
    }

    /**
     * Sets the cancellation token source for managing request cancellation
     * 
     * @param source The cancellation token source
     */
    setCancellationTokenSource(source: CancellationTokenSource | undefined): void {
        this.cancelationTokenSource = source;
    }

}
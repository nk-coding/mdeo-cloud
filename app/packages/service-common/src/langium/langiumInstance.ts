import type { LanguageServices } from "@mdeo/language-common";
import type { ContributionPluginKey } from "../service/types.js";
import type { ServiceAdditionalServices } from "./types.js";
import type { URI } from "vscode-uri";
import { isOperationCancelled, type LangiumDocument } from "langium";
import { CancellationTokenSource } from "vscode-jsonrpc";
import type { BackendExternalReferencesResolver } from "./backendExternalReferencesResolver.js";

/**
 * A managed Langium instance with its services and state
 *
 * @template T Additional services provided by the service layer
 */
export class LangiumInstance<T> {
    /**
     * Flag if the instance is currently busy processing a request
     */
    busy: boolean = false;

    /**
     * Timestamp of last use (for LRU eviction)
     */
    lastUsed: number = Date.now();

    /**
     * Creates a new Langium instance
     *
     * @param id the unique identifier for the instance
     * @param services The Langium services for this instance
     * @param contributionPluginKey Key identifying the contribution plugin configuration
     */
    constructor(
        readonly id: string,
        readonly services: LanguageServices & ServiceAdditionalServices & T,
        readonly contributionPluginKey: ContributionPluginKey
    ) {}

    /**
     * Configures the instance for a new request
     * Must be called before using the instance
     * Cannot be called if the instance is already busy
     *
     * @param jwt the JWT for authentication
     * @param project the project context
     */
    configure(jwt: string, project: string): void {
        if (this.busy) {
            throw new Error(`Langium instance ${this.id} is already busy`);
        }
        this.services.shared.ServerApi.setContext(jwt, project);
        this.busy = true;
    }

    /**
     * Resets the instance after request processing
     * Clears the JWT and project context
     * Deletes all loaded documents to free memory
     * Cannot be called if the instance is not busy
     */
    reset(): void {
        if (!this.busy) {
            throw new Error(`Langium instance ${this.id} is not busy`);
        }
        this.services.shared.ServerApi.clearContext();
        this.services.shared.ServerApi.resetTrackedRequests();
        const documents = this.services.shared.workspace.LangiumDocuments.all.toArray();
        for (const doc of documents) {
            this.services.shared.workspace.LangiumDocuments.deleteDocument(doc.uri);
        }

        this.busy = false;
    }

    /**
     * Builds the document at the given URI, ensuring all references are resolved.
     * Handles external references via the BackendExternalReferencesResolver.
     *
     * @param uri The URI of the document to build
     * @returns The built Langium document
     */
    async buildDocument(uri: URI): Promise<LangiumDocument> {
        let hasLoadedDocuments = true;
        const builder = this.services.shared.workspace.DocumentBuilder;
        const documents = this.services.shared.workspace.LangiumDocuments;
        const externalReferenceResolver = this.services.shared.references
            .ExternalReferenceResolver as BackendExternalReferencesResolver;
        while (hasLoadedDocuments) {
            const tokenSource = new CancellationTokenSource();
            externalReferenceResolver.setCancellationTokenSource(tokenSource);
            try {
                await builder.build(documents.all.toArray(), { validation: true }, tokenSource.token);
            } catch (e) {
                if (!isOperationCancelled(e)) {
                    throw e;
                }
            } finally {
                hasLoadedDocuments = tokenSource.token.isCancellationRequested;
                tokenSource.dispose();
            }
        }
        const document = documents.getDocument(uri);
        if (!document) {
            throw new Error(`Document not found after build: ${uri.toString()}`);
        }
        return document;
    }
}

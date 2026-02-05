import type { URI } from "langium";
import { DefaultLangiumDocuments, type LangiumDocument, UriTrie } from "langium";
import type { CancellationToken } from "vscode-languageserver";

/**
 * Extension to langium documents that allows for concurrent getOrCreateDocument calls for the same URI.
 * Prevents race conditions when multiple concurrent calls try to create the same document.
 */
export class ConcurrentLangiumDocuments extends DefaultLangiumDocuments {
    /**
     * Tracks in-flight document creation promises to prevent duplicate creation attempts.
     */
    protected readonly promiseTrie = new UriTrie<Promise<LangiumDocument>>();

    override async getOrCreateDocument(uri: URI, cancellationToken?: CancellationToken): Promise<LangiumDocument> {
        const document = this.getDocument(uri);
        if (document) {
            return document;
        }

        const uriString = uri.toString();

        const existingPromise = this.promiseTrie.find(uriString);
        if (existingPromise) {
            return existingPromise;
        }

        const creationPromise = (async () => {
            try {
                const newDocument = await this.langiumDocumentFactory.fromUri(uri, cancellationToken);
                this.addDocument(newDocument);
                return newDocument;
            } finally {
                this.promiseTrie.delete(uriString);
            }
        })();

        this.promiseTrie.insert(uriString, creationPromise);
        return creationPromise;
    }
}

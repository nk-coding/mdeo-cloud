import { DefaultIndexManager, type AstNodeDescription, type LangiumDocument } from "langium";

/**
 * An extended index manager that allows updating exported elements for external documents directly bypassing the regular updateContent method
 */
export class ExtendedIndexManager extends DefaultIndexManager {
    /**
     * Updates the exported elements for an external document
     * Directly accesses the symbol index and ignores the regular scope computation, which cannot be performed due to the services missing.
     *
     * @param document the external document
     * @param astNodeDescriptions the AST node descriptions to register
     */
    updateExternalContent(document: LangiumDocument, astNodeDescriptions: AstNodeDescription[]): void {
        const uri = document.uri.toString();
        this.symbolIndex.set(uri, astNodeDescriptions);
        this.symbolByTypeIndex.clear();
    }
}

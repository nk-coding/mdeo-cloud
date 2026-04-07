import type { AstReflection, AstSerializerAdditionalServices, ExtendedLangiumServices } from "@mdeo/language-common";
import {
    BaseCompletionProvider,
    computeRelativePathCompletions,
    acceptRelativePathCompletions,
    sharedImport
} from "@mdeo/language-shared";
import type { CompletionAcceptor, CompletionContext, CompletionProviderOptions, NextFeature } from "langium/lsp";
import type { LangiumDocuments, MaybePromise } from "langium";
import { UsingPath } from "../grammar/mdeoTypes.js";

const { AstUtils } = sharedImport("langium");

/**
 * Completion provider for the MDEO config language.
 *
 * Provides relative path completions for file path string properties such as
 * using paths (model transformation files).
 */
export class MdeoCompletionProvider extends BaseCompletionProvider {
    override readonly completionOptions: CompletionProviderOptions = { triggerCharacters: ['"', "/", "."] };

    private readonly documents: LangiumDocuments;
    private readonly reflection: AstReflection;

    /**
     * @param services Combined Langium services
     */
    constructor(services: ExtendedLangiumServices & AstSerializerAdditionalServices) {
        super(services);
        this.documents = services.shared.workspace.LangiumDocuments;
        this.reflection = services.shared.AstReflection;
    }

    /**
     * Extends the default completion with relative path suggestions for file path properties.
     *
     * Injects path completions before delegating to the default provider, so that
     * file path fields (e.g., model transformation "using" paths) offer workspace-relative
     * suggestions.
     *
     * @param context The current completion context
     * @param next Describes the grammar feature being completed, including its type and property
     * @param acceptor The acceptor function to register completion items
     * @returns A promise or void when completion is complete
     */
    protected override completionFor(
        context: CompletionContext,
        next: NextFeature,
        acceptor: CompletionAcceptor
    ): MaybePromise<void> {
        this.completionForRelativePath(context, next, acceptor);
        return super.completionFor(context, next, acceptor);
    }

    /**
     * Provides relative path completions for file path string properties in the
     * MDEO config language, such as "using" paths for model transformation files.
     *
     * @param context The current completion context
     * @param next Describes the grammar feature being completed
     * @param acceptor The acceptor function to register completion items
     * @returns void
     */
    private completionForRelativePath(
        context: CompletionContext,
        next: NextFeature,
        acceptor: CompletionAcceptor
    ): void {
        const node = context.node;
        if (node == undefined) {
            return;
        }

        let extensions: string[] | undefined;

        if (this.reflection.isInstance(node, UsingPath) && next.property === "path") {
            extensions = [".mt"];
        }

        if (extensions == undefined) {
            return;
        }

        try {
            const document = AstUtils.getDocument(node);
            const paths = computeRelativePathCompletions(document, this.documents, extensions);
            acceptRelativePathCompletions(context, acceptor, paths);
        } catch {
            // Ignore errors during completion
        }
    }
}

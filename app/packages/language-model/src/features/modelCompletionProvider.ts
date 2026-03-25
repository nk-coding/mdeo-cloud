import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import { computeRelativePathCompletions, acceptRelativePathCompletions, sharedImport } from "@mdeo/language-shared";
import type { CompletionAcceptor, CompletionContext, CompletionProviderOptions, NextFeature } from "langium/lsp";
import type { LangiumDocuments, MaybePromise } from "langium";
import { MetamodelFileImport } from "../grammar/modelTypes.js";

const { DefaultCompletionProvider } = sharedImport("langium/lsp");
const { AstUtils } = sharedImport("langium");

/**
 * Completion provider for the Model language.
 *
 * Provides relative path completions for the metamodel file path in
 * `using "..."` statements.
 */
export class ModelCompletionProvider extends DefaultCompletionProvider {
    override readonly completionOptions: CompletionProviderOptions = { triggerCharacters: ['"', "/", "."] };

    private readonly documents: LangiumDocuments;
    private readonly reflection: AstReflection;

    constructor(services: ExtendedLangiumServices) {
        super(services);
        this.documents = services.shared.workspace.LangiumDocuments;
        this.reflection = services.shared.AstReflection;
    }

    protected override completionFor(
        context: CompletionContext,
        next: NextFeature,
        acceptor: CompletionAcceptor
    ): MaybePromise<void> {
        this.completionForRelativePath(context, next, acceptor);
        return super.completionFor(context, next, acceptor);
    }

    private completionForRelativePath(
        context: CompletionContext,
        next: NextFeature,
        acceptor: CompletionAcceptor
    ): void {
        const node = context.node;
        if (node == undefined) {
            return;
        }

        if (!this.reflection.isInstance(node, MetamodelFileImport) || next.property !== "file") {
            return;
        }

        try {
            const document = AstUtils.getDocument(node);
            const paths = computeRelativePathCompletions(document, this.documents, [".mm"]);
            acceptRelativePathCompletions(context, acceptor, paths);
        } catch {
            // Ignore errors during completion
        }
    }
}

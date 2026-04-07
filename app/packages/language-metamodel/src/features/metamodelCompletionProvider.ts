import type { AstReflection, AstSerializerAdditionalServices, ExtendedLangiumServices } from "@mdeo/language-common";
import {
    BaseCompletionProvider,
    computeRelativePathCompletions,
    acceptRelativePathCompletions,
    sharedImport
} from "@mdeo/language-shared";
import type { CompletionAcceptor, CompletionContext, CompletionProviderOptions, NextFeature } from "langium/lsp";
import type { LangiumDocuments, MaybePromise } from "langium";
import { FileImport } from "../grammar/metamodelTypes.js";

const { AstUtils } = sharedImport("langium");

/**
 * Completion provider for the Metamodel language.
 *
 * Provides relative path completions for the file path in
 * `import "..."` statements.
 */
export class MetamodelCompletionProvider extends BaseCompletionProvider {
    override readonly completionOptions: CompletionProviderOptions = { triggerCharacters: ['"', "/", "."] };

    private readonly documents: LangiumDocuments;
    private readonly reflection: AstReflection;

    constructor(services: ExtendedLangiumServices & AstSerializerAdditionalServices) {
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

        if (!this.reflection.isInstance(node, FileImport) || next.property !== "file") {
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

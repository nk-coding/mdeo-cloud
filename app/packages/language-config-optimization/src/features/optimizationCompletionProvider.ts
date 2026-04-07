import type { AstReflection, ExtendedLangiumServices, AstSerializerAdditionalServices } from "@mdeo/language-common";
import { isMetamodelCompatible } from "@mdeo/language-metamodel";
import {
    BaseCompletionProvider,
    FileImportCompletionHelper,
    computeRelativePathCompletions,
    acceptRelativePathCompletions,
    resolveRelativePath,
    sharedImport,
    type AstFileImportItem
} from "@mdeo/language-shared";
import type { CompletionAcceptor, CompletionContext, CompletionProviderOptions, NextFeature } from "langium/lsp";
import type { AstNode, CompositeCstNode, LangiumDocument, LangiumDocuments } from "langium";
import {
    ConstraintReference,
    FunctionFileImport,
    FunctionImport,
    GoalSection,
    Objective,
    ProblemSection,
    ScriptFunction,
    type FunctionImportType,
    type GoalSectionType,
    type ProblemSectionType
} from "../grammar/optimizationTypes.js";
import { findProblemSection, getMetamodelUri } from "./util.js";
import type { ScriptType } from "@mdeo/language-script";

const { AstUtils } = sharedImport("langium");
const { CompletionItemKind } = sharedImport("vscode-languageserver-protocol");

/**
 * Concrete completion helper for optimization config function imports.
 *
 * Filters target scripts by metamodel compatibility with the config document's
 * problem section metamodel.
 */
class OptimizationFunctionImportHelper extends FileImportCompletionHelper<AstNode> {
    constructor(services: ExtendedLangiumServices & AstSerializerAdditionalServices) {
        super(
            ScriptFunction,
            FunctionFileImport.name,
            FunctionImport.name,
            services.AstSerializer,
            services.shared.workspace.IndexManager,
            services.shared.workspace.LangiumDocuments
        );
    }

    protected override shouldIncludeDocument(targetDoc: LangiumDocument, currentDoc: LangiumDocument): boolean {
        const root = currentDoc.parseResult?.value;
        if (root == undefined) {
            return false;
        }

        const problemSection = findProblemSection(root) as ProblemSectionType | undefined;
        const configMetamodelUri =
            problemSection != undefined ? getMetamodelUri(currentDoc, problemSection) : undefined;
        const configMetamodelDoc =
            configMetamodelUri != undefined ? this.langiumDocuments.getDocument(configMetamodelUri) : undefined;
        if (configMetamodelDoc == undefined) {
            return false;
        }

        const targetScript = targetDoc.parseResult?.value as ScriptType | undefined;
        const scriptMetamodelPath = targetScript?.metamodelImport?.file;
        if (scriptMetamodelPath == undefined) {
            return false;
        }

        const scriptMetamodelDoc = this.langiumDocuments.getDocument(
            resolveRelativePath(targetDoc, scriptMetamodelPath)
        );
        if (scriptMetamodelDoc == undefined) {
            return false;
        }

        return isMetamodelCompatible(scriptMetamodelDoc, configMetamodelDoc, this.langiumDocuments);
    }

    protected override buildCompletionItem(entityName: string, relativePath: string) {
        return {
            label: entityName,
            kind: CompletionItemKind.Function,
            labelDetails: { description: relativePath },
            sortText: "1"
        };
    }
}

/**
 * Completion provider for the Config-Optimization language.
 *
 * Provides completions for:
 * - constraint/objective function references (already-imported and not-yet-imported
 *   functions from compatible scripts, with automatic import insertion)
 * - Relative path completions for file path properties
 */
export class OptimizationCompletionProvider extends BaseCompletionProvider {
    override readonly completionOptions: CompletionProviderOptions = { triggerCharacters: ['"', "/", "."] };

    private readonly documents: LangiumDocuments;
    private readonly reflection: AstReflection;
    private readonly importHelper: OptimizationFunctionImportHelper;

    constructor(services: ExtendedLangiumServices & AstSerializerAdditionalServices) {
        super(services);
        this.documents = services.shared.workspace.LangiumDocuments;
        this.reflection = services.shared.AstReflection;
        this.importHelper = new OptimizationFunctionImportHelper(services);
    }

    /**
     * Routes completion to the appropriate handler based on the grammar feature being completed.
     */
    protected override async completionFor(
        context: CompletionContext,
        next: NextFeature,
        acceptor: CompletionAcceptor
    ): Promise<void> {
        const isConstraintRef =
            next.property === "constraint" && this.reflection.isInstance(context.node, ConstraintReference);
        const isObjectiveRef = next.property === "objective" && this.reflection.isInstance(context.node, Objective);

        if (isConstraintRef || isObjectiveRef) {
            await this.completionForFunctionReference(context, acceptor);
            return;
        }

        this.completionForRelativePath(context, next, acceptor);

        return super.completionFor(context, next, acceptor);
    }

    /**
     * Provides completion items for constraint and objective function references.
     *
     * First registers all already-imported functions (highest priority sort), then
     * uses the import helper to suggest not-yet-imported functions from compatible scripts.
     */
    private async completionForFunctionReference(
        context: CompletionContext,
        acceptor: CompletionAcceptor
    ): Promise<void> {
        const node = context.node;
        if (node == undefined) {
            return;
        }

        try {
            const document = AstUtils.getDocument(node);
            const goalSection = AstUtils.getContainerOfType(node, (node): node is GoalSectionType =>
                this.reflection.isInstance(node, GoalSection)
            ) as GoalSectionType | undefined;

            if (goalSection == undefined) {
                return;
            }

            for (const fileImport of goalSection.imports) {
                for (const imp of fileImport.imports) {
                    const funcName = (imp as FunctionImportType).name ?? imp.entity.ref?.name;
                    if (funcName == undefined) {
                        continue;
                    }
                    acceptor(context, {
                        label: funcName,
                        kind: CompletionItemKind.Function,
                        sortText: "0"
                    });
                }
            }

            await this.importHelper.computeCompletions(
                context,
                acceptor,
                document,
                goalSection.imports as ReadonlyArray<AstFileImportItem>,
                () => {
                    if (goalSection.imports.length > 0) {
                        return goalSection.imports[goalSection.imports.length - 1].$cstNode;
                    }
                    const sectionCst = goalSection.$cstNode as CompositeCstNode | undefined;
                    return sectionCst?.content.find((c) => c.text.trim() === "{");
                },
                undefined,
                1
            );
        } catch {
            // Ignore errors during completion on incomplete documents
        }
    }

    /**
     * Provides relative path completions for file path string properties.
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

        if (this.reflection.isInstance(context.node, ProblemSection) && next.property === "metamodel") {
            extensions = [".mm"];
        } else if (this.reflection.isInstance(context.node, ProblemSection) && next.property === "model") {
            extensions = [".m"];
        } else if (this.reflection.isInstance(node, FunctionFileImport) && next.property === "file") {
            extensions = [".script"];
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

import { sharedImport } from "@mdeo/language-shared";
import type { LangiumDocument, AstNodeDescription, AstNode, MultiMap } from "langium";
import {
    CallOutput,
    CallStatement,
    MatchStatement,
    ModelTransformation,
    PatternApplicationCondition,
    PatternObjectInstance,
    PatternVariable,
    TransformationParameter,
    TransformationSignature
} from "../grammar/modelTransformationTypes.js";
import type { ModelTransformationType } from "../grammar/modelTransformationTypes.js";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";
import type { CancellationToken } from "vscode-jsonrpc";

const { DefaultScopeComputation, AstUtils } = sharedImport("langium");

/**
 * The scope computation for the Model Transformation language.
 *
 * Registers PatternObjectInstance nodes as local symbols
 * in the closest non-Pattern/MatchStatement container, the parameters of the signature on the
 * transformation root and the outputs of a call in the block containing the call.
 */
export class ModelTransformationScopeComputation extends DefaultScopeComputation {
    /**
     * AST reflection service for type checking.
     */
    private readonly astReflection: AstReflection;

    constructor(services: ExtendedLangiumServices) {
        super(services);
        this.astReflection = services.shared.AstReflection;
    }

    /**
     * Collects exported symbols from a model transformation document.
     * Exports all PatternVariable and PatternObjectInstance nodes at any nesting depth.
     * This is necessary for findUniqueName to work correctly. The signature is exported too,
     * so that other transformations can import it.
     */
    override collectExportedSymbols(
        document: LangiumDocument,
        cancelToken?: CancellationToken
    ): Promise<AstNodeDescription[]> {
        return this.collectExportedSymbolsForNode(
            document.parseResult.value,
            document,
            (root) => {
                const result: AstNode[] = [];
                for (const node of AstUtils.streamAllContents(root as ModelTransformationType)) {
                    if (
                        this.astReflection.isInstance(node, PatternVariable) ||
                        this.astReflection.isInstance(node, PatternObjectInstance) ||
                        this.astReflection.isInstance(node, TransformationSignature)
                    ) {
                        result.push(node);
                    }
                }
                return result;
            },
            cancelToken
        );
    }

    /**
     * Registers a locally declared symbol.
     *
     * An instance declared inside a condition block belongs to that block's own graph, so it
     * is registered on the block: it is visible to the rest of the block, but not to the
     * enclosing pattern or to any sibling block.
     *
     * @param node The node to register
     * @param document The document being scoped
     * @param symbols The symbol table to add to
     */
    protected override addLocalSymbol(
        node: AstNode,
        document: LangiumDocument,
        symbols: MultiMap<AstNode, AstNodeDescription>
    ): void {
        if (this.astReflection.isInstance(node, TransformationParameter)) {
            const root = AstUtils.getContainerOfType(node, (n) =>
                this.astReflection.isInstance(n, ModelTransformation)
            );
            this.addSymbol(node, node.name, root, document, symbols);
            return;
        }
        if (this.astReflection.isInstance(node, CallOutput)) {
            const call = node.$container;
            const container = this.astReflection.isInstance(call, CallStatement) ? call.$container : undefined;
            this.addSymbol(node, node.name, container, document, symbols);
            return;
        }
        if (this.astReflection.isInstance(node, PatternObjectInstance)) {
            let container: AstNode | undefined;
            if (this.astReflection.isInstance(node.$container, PatternApplicationCondition)) {
                container = node.$container;
            } else {
                container = node.$container?.$container;
                if (this.astReflection.isInstance(container, MatchStatement)) {
                    container = container.$container;
                }
            }
            if (container != undefined) {
                const name = this.nameProvider.getName(node);
                if (name != undefined) {
                    symbols.add(container, this.descriptions.createDescription(node, name, document));
                }
            }
        }
    }

    /**
     * Adds a named node to the symbols of a container.
     *
     * @param node The node to register
     * @param name The node's name
     * @param container The container the name is visible in
     * @param document The document being scoped
     * @param symbols The symbol table to add to
     */
    private addSymbol(
        node: AstNode,
        name: string | undefined,
        container: AstNode | undefined,
        document: LangiumDocument,
        symbols: MultiMap<AstNode, AstNodeDescription>
    ): void {
        if (container != undefined && name != undefined && name !== "") {
            symbols.add(container, this.descriptions.createDescription(node, name, document));
        }
    }
}

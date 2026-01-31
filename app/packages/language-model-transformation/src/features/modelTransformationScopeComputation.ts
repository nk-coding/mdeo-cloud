import { sharedImport } from "@mdeo/language-shared";
import type { LangiumDocument, AstNodeDescription, AstNode, MultiMap } from "langium";
import { MatchStatement, PatternObjectInstance } from "../grammar/modelTransformationTypes.js";
import type { AstReflection, ExtendedLangiumServices } from "@mdeo/language-common";

const { DefaultScopeComputation } = sharedImport("langium");

/**
 * The scope computation for the Model Transformation language.
 *
 * Registers PatternObjectInstance nodes as local symbols
 * in the closest non-Pattern/MatchStatement container.
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

    protected override addLocalSymbol(
        node: AstNode,
        document: LangiumDocument,
        symbols: MultiMap<AstNode, AstNodeDescription>
    ): void {
        if (this.astReflection.isInstance(node, PatternObjectInstance)) {
            let container = node.$container?.$container;
            if (this.astReflection.isInstance(container, MatchStatement)) {
                container = container.$container;
            }
            if (container != undefined) {
                const name = this.nameProvider.getName(node);
                if (name != undefined) {
                    symbols.add(container, this.descriptions.createDescription(node, name, document));
                }
            }
        }
    }
}

import { BaseModelIdProvider, sharedImport } from "@mdeo/language-shared";
import type { ModelIdRegistry } from "@mdeo/language-shared";
import type { AstNode } from "langium";
import { GeneratedModel } from "../../../grammar/generatedModelTypes.js";
import type { AstReflection } from "@mdeo/language-common";
import { AstReflectionKey } from "@mdeo/language-shared";

const { injectable, inject } = sharedImport("inversify");

/**
 * Provides unique IDs for generated model AST nodes.
 * Since the generated model only has a single root node,
 * this is a minimal implementation.
 */
@injectable()
export class GeneratedModelModelIdProvider extends BaseModelIdProvider {
    @inject(AstReflectionKey)
    protected reflection!: AstReflection;

    /**
     * Gets the name/ID for an AST node.
     *
     * @param node The AST node
     * @returns The generated name/ID or undefined
     */
    getName(node: AstNode, _registry: ModelIdRegistry): string | undefined {
        if (this.reflection.isInstance(node, GeneratedModel)) {
            return "model-graph";
        }
        return undefined;
    }
}

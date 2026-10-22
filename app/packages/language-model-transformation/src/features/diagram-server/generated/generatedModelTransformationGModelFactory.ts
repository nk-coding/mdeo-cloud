import type { GModelRoot } from "@eclipse-glsp/server";
import { sharedImport, DefaultModelIdRegistry, type ModelIdRegistry } from "@mdeo/language-shared";
import { ModelTransformationGModelFactory } from "../modelTransformationGModelFactory.js";
import type { PartialGeneratedModelTransformation } from "../../../grammar/generatedModelTransformationPartialTypes.js";
import { adaptGeneratedModelTransformationRoot } from "./generatedModelTransformationAstAdapter.js";

const { injectable } = sharedImport("inversify");
const { GGraph } = sharedImport("@eclipse-glsp/server");

/**
 * GModel factory for generated model transformation files.
 * Reuses the regular transformation factory by adapting typed-ast JSON to a synthetic AST.
 */
@injectable()
export class GeneratedModelTransformationGModelFactory extends ModelTransformationGModelFactory {
    /**
     * A generated transformation is shown from its typed AST, which has no header in source.
     *
     * @returns `false`
     */
    protected override includesHeader(): boolean {
        return false;
    }

    override async createModelInternal(
        sourceModel: PartialGeneratedModelTransformation,
        _idRegistry: ModelIdRegistry
    ): Promise<GModelRoot> {
        const adaptedRoot = adaptGeneratedModelTransformationRoot(sourceModel);
        if (adaptedRoot == undefined) {
            return GGraph.builder().id("transformation-graph").addCssClass("editor-model-transformation").build();
        }

        const adaptedRegistry = new DefaultModelIdRegistry(adaptedRoot, this.modelIdProvider);
        return super.createModelInternal(adaptedRoot, adaptedRegistry);
    }
}

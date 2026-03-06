import type { CreateEdgeSchema } from "@mdeo/editor-protocol";
import { CreateEdgeSchemaResolver, sharedImport } from "@mdeo/language-shared";
import type { InitialCreateEdgeSchemaRequest, TargetCreateEdgeSchemaRequest } from "@mdeo/language-shared";

const { injectable } = sharedImport("inversify");

/**
 * Dummy resolver for model-transformation diagrams.
 */
@injectable()
export class ModelTransformationCreateEdgeSchemaResolver extends CreateEdgeSchemaResolver {
    override async getInitialSchema(_request: InitialCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined> {
        throw new Error("Create-edge schema resolution is not supported in model-transformation diagrams.");
    }

    override async getTargetSchema(_request: TargetCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined> {
        throw new Error("Create-edge schema resolution is not supported in model-transformation diagrams.");
    }
}

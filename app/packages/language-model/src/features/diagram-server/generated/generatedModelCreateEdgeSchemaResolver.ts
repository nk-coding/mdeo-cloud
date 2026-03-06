import type { CreateEdgeSchema } from "@mdeo/editor-protocol";
import { CreateEdgeSchemaResolver, sharedImport } from "@mdeo/language-shared";
import type { InitialCreateEdgeSchemaRequest, TargetCreateEdgeSchemaRequest } from "@mdeo/language-shared";

const { injectable } = sharedImport("inversify");

/**
 * Dummy resolver for generated-model diagrams.
 */
@injectable()
export class GeneratedModelCreateEdgeSchemaResolver extends CreateEdgeSchemaResolver {
    override async getInitialSchema(_request: InitialCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined> {
        throw new Error("Create-edge schema resolution is not supported in generated-model diagrams.");
    }

    override async getTargetSchema(_request: TargetCreateEdgeSchemaRequest): Promise<CreateEdgeSchema | undefined> {
        throw new Error("Create-edge schema resolution is not supported in generated-model diagrams.");
    }
}

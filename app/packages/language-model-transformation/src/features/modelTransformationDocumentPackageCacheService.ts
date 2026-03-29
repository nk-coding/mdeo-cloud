import type { LangiumDocument } from "langium";
import { DefaultDocumentPackageCacheService } from "@mdeo/language-expression";
import type { ModelTransformationType } from "../grammar/modelTransformationTypes.js";

/**
 * Model Transformation-specific package cache service that resolves the metamodel import
 * from the `import.file` property of the ModelTransformation root node.
 */
export class ModelTransformationDocumentPackageCacheService extends DefaultDocumentPackageCacheService {
    protected override getMetamodelImportFile(document: LangiumDocument): string | undefined {
        const root = document.parseResult?.value as ModelTransformationType | undefined;
        return root?.import?.file;
    }
}

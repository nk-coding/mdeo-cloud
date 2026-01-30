import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";
import { resolveRelativePath } from "@mdeo/language-shared";
import type { ModelTransformationType } from "../grammar/modelTransformationTypes.js";

/**
 * A collector for external references used in model transformation reference resolution.
 * Collects metamodel file imports from transformation files.
 */
export class ModelTransformationExternalReferenceCollector implements ExternalReferenceCollector {
    /**
     * Finds external references in the given documents.
     * Extracts metamodel file paths from import statements.
     *
     * @param docs The documents to search for external references.
     * @returns The collected external references.
     */
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const uris = docs.flatMap((doc) => {
            const transformation = doc.parseResult.value as ModelTransformationType;
            const imp = transformation.import?.file;
            if (imp != undefined) {
                return [resolveRelativePath(doc, imp)];
            } else {
                return [];
            }
        });
        return {
            local: [],
            external: uris
        };
    }
}

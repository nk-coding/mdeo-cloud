import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";
import { resolveRelativePath } from "@mdeo/language-shared";
import type { ModelType } from "../grammar/modelTypes.js";

/**
 * A collector for external references used in model reference resolution.
 */
export class ModelExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const uris = docs.flatMap((doc) => {
            const model = doc.parseResult.value as ModelType;
            const imp = model.import?.file;
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

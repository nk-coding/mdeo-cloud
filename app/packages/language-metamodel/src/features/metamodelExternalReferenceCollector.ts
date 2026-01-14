import type { ExternalReferenceCollector } from "@mdeo/language-common";
import type { LangiumDocument, URI } from "langium";
import type { MetaModelType } from "../grammar/metamodelTypes.js";
import { resolveRelativePath } from "@mdeo/language-shared";

/**
 * A collector for external references used in metamodel reference resolution.
 */
export class MetamodelExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): URI[] {
        return docs.flatMap((doc) => {
            const metamodel = doc.parseResult.value as MetaModelType;
            return metamodel.imports
                .filter((imp) => imp.file != undefined)
                .map((imp) => resolveRelativePath(doc, imp.file));
        });
    }
}

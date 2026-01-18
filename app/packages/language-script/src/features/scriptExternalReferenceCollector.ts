import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";
import type { ScriptType } from "../grammar/scriptTypes.js";
import { resolveRelativePath } from "@mdeo/language-shared";

/**
 * A collector for external references used in script reference resolution.
 */
export class ScriptExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const uris = docs.flatMap((doc) => {
            const script = doc.parseResult.value as ScriptType;
            return script.imports
                .filter((imp) => imp.file != undefined)
                .map((imp) => resolveRelativePath(doc, imp.file));
        });
        return {
            local: uris,
            external: []
        };
    }
}

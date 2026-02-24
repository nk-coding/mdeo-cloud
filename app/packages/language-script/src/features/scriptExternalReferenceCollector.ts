import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";
import type { ScriptType } from "../grammar/scriptTypes.js";
import { resolveRelativePath } from "@mdeo/language-shared";

/**
 * A collector for external references used in script reference resolution.
 * Collects both function imports (local) and metamodel imports (external).
 */
export class ScriptExternalReferenceCollector implements ExternalReferenceCollector {
    /**
     * Finds external references in the given documents.
     * Extracts function imports as local references and metamodel imports as external references.
     *
     * @param docs The documents to search for external references.
     * @returns The collected external references.
     */
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const localUris = docs.flatMap((doc) => {
            const script = doc.parseResult.value as ScriptType;
            return script.imports
                .filter((imp) => imp.file != undefined)
                .map((imp) => resolveRelativePath(doc, imp.file));
        });

        const externalUris = docs.flatMap((doc) => {
            const script = doc.parseResult.value as ScriptType;
            const metamodelImport = script.metamodelImport?.file;
            if (metamodelImport != undefined) {
                return [resolveRelativePath(doc, metamodelImport)];
            }
            return [];
        });

        return {
            local: localUris,
            external: externalUris
        };
    }
}

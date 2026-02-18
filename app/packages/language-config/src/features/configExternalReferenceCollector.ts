import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";

/**
 * External reference collector for the config language.
 * Config files don't have external references in the traditional sense (everything is inline),
 * so this returns empty collections.
 */
export class ConfigExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        return {
            local: [],
            external: []
        };
    }
}
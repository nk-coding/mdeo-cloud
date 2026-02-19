import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";

/**
 * External reference collector for the config language.
 * Config files don't have external references in the traditional sense (everything is inline),
 * so this returns empty collections.
 */
export class ConfigExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(): ExternalReferences {
        return {
            local: [],
            external: []
        };
    }
}

import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";

/**
 * A default implementation of {@link ExternalReferenceCollector} that returns no references.
 */
export class DefaultExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(_docs: LangiumDocument[]): ExternalReferences {
        return {
            local: [],
            external: []
        };
    }
}

import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { LangiumDocument } from "langium";
import { resolveRelativePath } from "@mdeo/language-shared";
import { getWrapperInterfaceName } from "@mdeo/language-config";
import { OPTIMIZATION_PLUGIN_NAME } from "../plugin/optimizationContributionPlugin.js";
import type { ProblemSectionType } from "../grammar/optimizationTypes.js";

/**
 * External reference collector for the config-optimization language.
 * Collects the metamodel file path from the problem section and returns it
 * as an external reference so it can be loaded and used for scope resolution.
 */
export class OptimizationExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const problemWrapperType = getWrapperInterfaceName("problem", OPTIMIZATION_PLUGIN_NAME);

        const uris = docs.flatMap((doc) => {
            const config = doc.parseResult.value as { sections?: unknown[] } | undefined;
            if (config == undefined || !Array.isArray(config.sections)) {
                return [];
            }

            for (const section of config.sections) {
                const node = section as { $type?: string; content?: ProblemSectionType };
                if (node.$type === problemWrapperType && node.content?.metamodel != null) {
                    return [resolveRelativePath(doc, node.content.metamodel)];
                }
            }

            return [];
        });

        return {
            local: [],
            external: uris
        };
    }
}
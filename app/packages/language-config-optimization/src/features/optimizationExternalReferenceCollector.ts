import type { ExternalReferenceCollector, ExternalReferences } from "@mdeo/language-common";
import type { AstNode, LangiumDocument, URI } from "langium";
import { resolveRelativePath } from "@mdeo/language-shared";
import { getWrapperInterfaceName } from "@mdeo/language-config";
import { OPTIMIZATION_PLUGIN_NAME } from "../plugin/optimizationContributionPlugin.js";
import type { GoalSectionType, ProblemSectionType } from "../grammar/optimizationTypes.js";

/**
 * External reference collector for the config-optimization language.
 * Collects the metamodel file path from the problem section and returns it
 * as an external reference so it can be loaded and used for scope resolution.
 */
export class OptimizationExternalReferenceCollector implements ExternalReferenceCollector {
    findExternalReferences(docs: LangiumDocument[]): ExternalReferences {
        const problemWrapperType = getWrapperInterfaceName("problem", OPTIMIZATION_PLUGIN_NAME);
        const goalWrapperType = getWrapperInterfaceName("goal", OPTIMIZATION_PLUGIN_NAME);

        const uris: URI[] = [];
        for (const doc of docs) {
            const config = doc.parseResult.value as { sections?: AstNode[] } | undefined;
            if (config == undefined || !Array.isArray(config.sections)) {
                continue;
            }

            for (const section of config.sections) {
                if (section.$type === problemWrapperType) {
                    const problemSection = section as { $type?: string; content?: ProblemSectionType };
                    if ((problemSection.content?.metamodel?.length ?? 0) > 0) {
                        uris.push(resolveRelativePath(doc, problemSection.content!.metamodel[0]));
                    }
                    if ((problemSection.content?.model?.length ?? 0) > 0) {
                        uris.push(resolveRelativePath(doc, problemSection.content!.model[0]));
                    }
                } else if (section.$type === goalWrapperType) {
                    const goalSection = section as { $type?: string; content?: GoalSectionType };
                    for (const imp of goalSection.content?.imports ?? []) {
                        if (imp.file != undefined) {
                            uris.push(resolveRelativePath(doc, imp.file));
                        }
                    }
                }
            }
        }

        return {
            local: [],
            external: uris
        };
    }
}

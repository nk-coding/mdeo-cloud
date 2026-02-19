import type { LangiumDocument, AstNode, URI } from "langium";
import { resolveRelativePath } from "@mdeo/language-shared";
import { Config, type ConfigType, getWrapperInterfaceName } from "@mdeo/language-config";
import { OPTIMIZATION_PLUGIN_NAME, type ProblemSectionType } from "@mdeo/language-config-optimization";
import type { MdeoMetamodelResolver } from "./mdeoMetamodelResolver.js";

/**
 * Default implementation of MdeoMetamodelResolver.
 * Resolves the metamodel URI by looking up the problem section from the optimization plugin.
 * This implementation is used in the browser environment.
 */
export class DefaultMdeoMetamodelResolver implements MdeoMetamodelResolver {
    /**
     * Gets the URI of the metamodel file from the problem section.
     *
     * @param document The current document
     * @returns The metamodel URI
     */
    getMetamodelUri(document: LangiumDocument): URI | undefined {
        const problemSection = this.findProblemSection(document);
        if ((problemSection?.metamodel?.length ?? 0) === 0) {
            return undefined;
        }
        return resolveRelativePath(document, problemSection!.metamodel[0]);
    }

    /**
     * Finds the problem section from the optimization plugin in the config document.
     *
     * @param document The current document
     * @returns The problem section, or undefined if not found
     */
    private findProblemSection(document: LangiumDocument): ProblemSectionType | undefined {
        const config = document.parseResult?.value as ConfigType | undefined;
        if (config == undefined || config.$type !== Config.name) {
            return undefined;
        }

        const problemWrapperType = getWrapperInterfaceName("problem", OPTIMIZATION_PLUGIN_NAME);
        for (const section of config.sections) {
            if (section.$type === problemWrapperType) {
                return (section as AstNode & { content: AstNode }).content as ProblemSectionType;
            }
        }

        return undefined;
    }
}

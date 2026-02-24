import type { AstNode, LangiumDocument, URI } from "langium";
import { resolveRelativePath, sharedImport } from "@mdeo/language-shared";
import { Config, type ConfigType, getWrapperInterfaceName } from "@mdeo/language-config";
import { type ProblemSectionType } from "../grammar/optimizationTypes.js";
import { OPTIMIZATION_PLUGIN_NAME } from "../plugin/optimizationContributionPlugin.js";

const { AstUtils } = sharedImport("langium");

/**
 * Finds the problem section in the optimization plugin of a config document.
 * Navigates up to the Config root and searches config.sections for the wrapper
 * whose $type matches the optimization problem wrapper name, returning its content.
 *
 * @param node Any AST node in the config document
 * @returns The problem section, or undefined if not found
 */
export function findProblemSection(node: AstNode): ProblemSectionType | undefined {
    const config = AstUtils.findRootNode(node) as ConfigType;

    if (config.$type !== Config.name) {
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

/**
 * Returns the resolved URI of the metamodel file referenced in the problem section.
 * Returns undefined if the problem section has no metamodel path.
 *
 * @param document The config document (used to resolve the relative path)
 * @param problemSection The problem section containing the metamodel path
 * @returns The metamodel URI, or undefined if not specified
 */
export function getMetamodelUri(document: LangiumDocument, problemSection: ProblemSectionType): URI | undefined {
    if (problemSection.metamodel.length === 0) {
        return undefined;
    }
    return resolveRelativePath(document, problemSection.metamodel[0]);
}

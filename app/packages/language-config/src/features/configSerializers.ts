import type { AstSerializerAdditionalServices, PrintContext } from "@mdeo/language-common";
import type { AstNode, LangiumCoreServices } from "langium";
import { Config, type ConfigType, type BaseConfigSectionType } from "../grammar/configTypes.js";
import { serializeNewlineSep, sharedImport } from "@mdeo/language-shared";
import type { Doc } from "prettier";
import type { ResolvedConfigContributionPlugins, SectionNamingInfo } from "../plugin/resolvePlugins.js";

const { doc } = sharedImport("prettier");

/**
 * Registers the config AST serializer for pretty-printing config AST nodes.
 *
 * @param services The Langium core services with AST serializer
 * @param resolvedPlugins Optional resolved plugin contributions (if available)
 */
export function registerConfigSerializers(
    services: LangiumCoreServices & AstSerializerAdditionalServices,
    resolvedPlugins: ResolvedConfigContributionPlugins
): void {
    const { AstSerializer } = services;

    AstSerializer.registerNodeSerializer(Config, (ctx) => printConfig(ctx));

    for (const sectionInfo of resolvedPlugins.sections.values()) {
        AstSerializer.registerNodeSerializer(sectionInfo.interface, (ctx) =>
            printConfigSectionWrapper(
                ctx as unknown as PrintContext<BaseConfigSectionType & { content: AstNode }>,
                sectionInfo
            )
        );
    }
}

/**
 * Prints a config node by serializing its sections with newlines in between.
 *
 * @param context The print context for the config node
 * @returns The formatted config as a Prettier Doc
 */
function printConfig(context: PrintContext<ConfigType>): Doc {
    return serializeNewlineSep(context, ["sections"], doc.builders);
}

/**
 * Prints a config section wrapper by adding the keyword prefix and then delegating to the original section serializer.
 *
 * @param context The print context for the section wrapper
 * @param sectionInfo The section naming information
 * @returns The formatted config section with keyword
 */
function printConfigSectionWrapper(
    context: PrintContext<BaseConfigSectionType & { content: AstNode }>,
    sectionInfo: SectionNamingInfo
): Doc {
    const { path, print } = context;
    const docs: Doc[] = [];

    const keyword = sectionInfo.requiresQualifiedName ? sectionInfo.qualifiedName : sectionInfo.sectionName;

    docs.push(keyword);
    docs.push(" ");
    docs.push(path.call(print, "content"));

    return docs;
}

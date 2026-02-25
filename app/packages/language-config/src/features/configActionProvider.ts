import type { GetFileActionsParams, GetFileActionsResponse, ActionIconNode } from "@mdeo/language-common";
import { convertIcon, FileCategory, parseUri, ActionDisplayLocation } from "@mdeo/language-common";
import type { LangiumCoreServices, LangiumSharedCoreServices, ServiceRegistry } from "langium";
import { Play } from "lucide";
import { sharedImport, type ActionProvider, type ActionHandlerRegistryAdditionalServices } from "@mdeo/language-shared";
import type { ConfigContributionPlugin } from "../plugin/configContributionPlugin.js";
import type { ResolvedConfigContributionPlugins, SectionNamingInfo } from "../plugin/resolvePlugins.js";
import type { ConfigType } from "../grammar/configTypes.js";
import { getServicesByLanguageId } from "./util.js";

const { URI } = sharedImport("langium");

/**
 * Action provider for config files.
 * Exposes the regular "run" file action when exactly one executable section is present.
 */
export class ConfigActionProvider implements ActionProvider {
    private readonly sectionTypeToNamingInfo: Map<string, SectionNamingInfo>;

    constructor(
        private readonly sharedServices: LangiumSharedCoreServices,
        private readonly serviceRegistry: ServiceRegistry,
        private readonly resolvedPlugins: ResolvedConfigContributionPlugins,
        private readonly plugins: ConfigContributionPlugin[]
    ) {
        this.sectionTypeToNamingInfo = new Map();
        for (const [, info] of this.resolvedPlugins.sections.entries()) {
            this.sectionTypeToNamingInfo.set(info.interface.name, info);
        }
    }

    async getFileActions(params: GetFileActionsParams): Promise<GetFileActionsResponse> {
        if (params.languageId !== "config") {
            return { actions: [] };
        }

        const parsedUri = parseUri(URI.parse(params.fileUri));
        if (parsedUri.category !== FileCategory.RegularFile) {
            return { actions: [] };
        }

        if (!(await this.hasForwardableExecutableSection(params.fileUri))) {
            return { actions: [] };
        }

        const runIcon: ActionIconNode = convertIcon(Play);
        return {
            actions: [
                {
                    name: "Run",
                    icon: runIcon,
                    key: "run",
                    displayLocations: [ActionDisplayLocation.EDITOR_TITLE, ActionDisplayLocation.CONTEXT_MENU]
                }
            ]
        };
    }

    private async hasForwardableExecutableSection(uri: string): Promise<boolean> {
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(URI.parse(uri));
        if (
            document == undefined ||
            document.parseResult.lexerErrors.length > 0 ||
            document.parseResult.parserErrors.length > 0
        ) {
            return false;
        }

        const config = document.parseResult.value as ConfigType;
        if (!Array.isArray(config.sections)) {
            return false;
        }

        const executableInfos = config.sections
            .map((section) => this.sectionTypeToNamingInfo.get(section.$type))
            .filter((info): info is SectionNamingInfo => info != undefined)
            .filter((info) => {
                const sectionDef = info.plugin.sections.find((section) => section.name === info.sectionName);
                return sectionDef?.executable === true;
            });

        if (executableInfos.length !== 1) {
            return false;
        }

        const executablePlugin = executableInfos[0].plugin;
        if (!this.plugins.includes(executablePlugin)) {
            return false;
        }

        const pluginServices = getServicesByLanguageId(this.serviceRegistry, executablePlugin.languageKey) as
            | (LangiumCoreServices & Partial<ActionHandlerRegistryAdditionalServices>)
            | undefined;
        const runHandler = pluginServices?.action?.ActionHandlerRegistry?.getHandler("run");
        return runHandler != undefined;
    }
}

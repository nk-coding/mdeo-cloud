import type {
    ActionStartParams,
    ActionStartResponse,
    ActionSubmitParams,
    ActionSubmitResponse,
    FileMenuActionData
} from "@mdeo/language-common";
import type { LangiumCoreServices, LangiumSharedCoreServices, ServiceRegistry } from "langium";
import type { ActionHandler, ActionHandlerRegistryAdditionalServices } from "@mdeo/language-shared";
import { sharedImport } from "@mdeo/language-shared";
import type { ConfigContributionPlugin } from "../plugin/configContributionPlugin.js";
import type { ResolvedConfigContributionPlugins, SectionNamingInfo } from "../plugin/resolvePlugins.js";
import type { ConfigType } from "../grammar/configTypes.js";
import { getServicesByLanguageId } from "../features/util.js";

const { URI } = sharedImport("langium");

/**
 * Run action handler for config files.
 * Delegates the action to the run handler of the contribution plugin that owns
 * the single executable section present in the config document.
 */
export class RunConfigActionHandler implements ActionHandler {
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

    async startAction(params: ActionStartParams): Promise<ActionStartResponse> {
        const uri = this.extractUri(params.data);
        if (uri == undefined) {
            return {
                kind: "error",
                message: "Missing config file URI for run action"
            };
        }

        const executableInfo = this.findExecutableSection(uri);
        if (executableInfo == undefined) {
            return {
                kind: "error",
                message: "Exactly one executable section must be present to run this config"
            };
        }

        const pluginServices = getServicesByLanguageId(this.serviceRegistry, executableInfo.plugin.languageKey) as
            | (LangiumCoreServices & Partial<ActionHandlerRegistryAdditionalServices>)
            | undefined;
        const handler = pluginServices?.action?.ActionHandlerRegistry?.getHandler("run");
        if (handler == undefined) {
            return {
                kind: "error",
                message: `Plugin language '${executableInfo.plugin.languageKey}' does not provide a run action handler`
            };
        }

        return await handler.startAction({
            ...params,
            languageId: executableInfo.plugin.languageKey
        });
    }

    async submitAction(params: ActionSubmitParams): Promise<ActionSubmitResponse> {
        const uri = this.extractUri(params.config.data);
        if (uri == undefined) {
            return {
                kind: "error",
                message: "Missing config file URI for run action"
            };
        }

        const executableInfo = this.findExecutableSection(uri);
        if (executableInfo == undefined) {
            return {
                kind: "error",
                message: "Exactly one executable section must be present to run this config"
            };
        }

        const pluginServices = getServicesByLanguageId(this.serviceRegistry, executableInfo.plugin.languageKey) as
            | (LangiumCoreServices & Partial<ActionHandlerRegistryAdditionalServices>)
            | undefined;
        const handler = pluginServices?.action?.ActionHandlerRegistry?.getHandler("run");
        if (handler == undefined) {
            return {
                kind: "error",
                message: `Plugin language '${executableInfo.plugin.languageKey}' does not provide a run action handler`
            };
        }

        return await handler.submitAction({
            ...params,
            config: {
                ...params.config,
                languageId: executableInfo.plugin.languageKey
            }
        });
    }

    private extractUri(data: unknown): string | undefined {
        const typedData = data as Partial<FileMenuActionData> | undefined;
        return typeof typedData?.uri === "string" ? typedData.uri : undefined;
    }

    private findExecutableSection(uri: string): SectionNamingInfo | undefined {
        const document = this.sharedServices.workspace.LangiumDocuments.getDocument(URI.parse(uri));
        if (
            document == undefined ||
            document.parseResult.lexerErrors.length > 0 ||
            document.parseResult.parserErrors.length > 0
        ) {
            return undefined;
        }

        const config = document.parseResult.value as ConfigType;
        if (!Array.isArray(config.sections)) {
            return undefined;
        }

        const executableInfos = config.sections
            .map((section) => this.sectionTypeToNamingInfo.get(section.$type))
            .filter((info): info is SectionNamingInfo => info != undefined)
            .filter((info) => {
                if (!this.plugins.includes(info.plugin)) {
                    return false;
                }
                const sectionDef = info.plugin.sections.find((section) => section.name === info.sectionName);
                return sectionDef?.executable === true;
            });

        if (executableInfos.length !== 1) {
            return undefined;
        }

        return executableInfos[0];
    }
}

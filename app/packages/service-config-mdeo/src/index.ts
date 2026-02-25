import { Settings2 } from "lucide";
import { convertIcon } from "@mdeo/language-common";
import type { MdeoServices } from "@mdeo/language-config-mdeo";
import {
    parseServiceConfigFromEnv,
    type ServiceConfig,
    type ServicePluginDefinition,
    type LanguageServiceConfig,
    initializePluginContext,
    astHandler,
    AST_HANDLER_KEY,
    startLanguageService
} from "@mdeo/service-common";
import type { LanguagePlugin } from "@mdeo/plugin";
import { ServiceMdeoMetamodelResolver } from "./serviceMdeoMetamodelResolver.js";

const icon = convertIcon(Settings2);

/**
 * Language plugin definition for the config-mdeo language.
 * This is a generated language that provides services for MDEO sections.
 */
const configMdeoLanguagePlugin: LanguagePlugin = {
    id: "config-mdeo",
    name: "Config MDEO",
    extension: undefined,
    newFileAction: false,
    icon,
    serverPlugin: {
        import: "language.js"
    },
    graphicalEditorPlugin: undefined,
    textualEditorPlugin: undefined,
    isGenerated: true
};

initializePluginContext();

const { configMdeoPluginProvider, createMdeoContributionPlugin } = await import("@mdeo/language-config-mdeo");
const { mdeoRequestHandler, MDEO_REQUEST_KEY } = await import("./handler/mdeoRequestHandler.js");

/**
 * Plugin definition for the config-mdeo service.
 */
const configMdeoServicePlugin: ServicePluginDefinition = {
    id: "config-mdeo-service",
    name: "Config MDEO",
    description: "Language support for config MDEO sections (search and solver)",
    icon,
    languagePlugins: [configMdeoLanguagePlugin],
    contributionPlugins: [
        {
            languageId: "config",
            description: "Provides search and solver section support for config language",
            additionalKeywords: [
                "search",
                "solver",
                "mutations",
                "using",
                "create",
                "delete",
                "mutate",
                "add",
                "remove",
                "provider",
                "algorithm",
                "parameters",
                "termination"
            ],
            serverContributionPlugins: [createMdeoContributionPlugin()]
        }
    ]
};

const envConfig = parseServiceConfigFromEnv();

/**
 * Language configuration for the config-mdeo language.
 */
const configMdeoLanguageConfig: LanguageServiceConfig<MdeoServices> = {
    languagePlugin: configMdeoLanguagePlugin,
    languagePluginProvider: configMdeoPluginProvider,
    serviceModule: {
        MdeoMetamodelResolver: () => new ServiceMdeoMetamodelResolver()
    },
    fileDataHandlers: {
        [AST_HANDLER_KEY]: astHandler
    },
    requestHandlers: {
        [MDEO_REQUEST_KEY]: mdeoRequestHandler
    }
};

const config: ServiceConfig<any> = {
    ...envConfig,
    plugin: configMdeoServicePlugin,
    languages: [configMdeoLanguageConfig]
};

await startLanguageService(config);

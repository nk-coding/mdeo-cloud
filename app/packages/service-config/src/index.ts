import { Settings } from "lucide";
import {
    defaultLanguageConfiguration,
    defaultMonarchTokenProvider,
    serializeMonarchTokensProvider,
    type ActionIconNode
} from "@mdeo/language-common";
import {
    parseServiceConfigFromEnv,
    type ServiceConfig,
    type ServicePluginDefinition,
    type LanguageServiceConfig,
    initializePluginContext,
    startLanguageService
} from "@mdeo/service-common";
import { convertIcon } from "@mdeo/language-common";
import type { ConfigAdditionalServices } from "@mdeo/language-config";
import type { LanguagePlugin } from "@mdeo/plugin";
import { DefaultScopeProvider } from "langium";

const icon: ActionIconNode = convertIcon(Settings);

/**
 * Language plugin definition for the config language.
 */
const configLanguagePlugin: LanguagePlugin = {
    id: "config",
    name: "Config",
    extension: ".config",
    newFileAction: true,
    icon,
    serverPlugin: {
        import: "static/language.js"
    },
    graphicalEditorPlugin: undefined,
    textualEditorPlugin: {
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: serializeMonarchTokensProvider({
            ...defaultMonarchTokenProvider,
            keywords: []
        })
    },
    isGenerated: false
};

/**
 * Plugin definition for the config service.
 */
const configServicePlugin: ServicePluginDefinition = {
    id: "config-service",
    name: "Config",
    description: "Language support for configuration files (.config)",
    icon,
    languagePlugins: [configLanguagePlugin],
    contributionPlugins: []
};

initializePluginContext();

const { configPluginProvider } = await import("@mdeo/language-config");
const { configDataHandler, CONFIG_DATA_KEY } = await import("./handler/configFileDataHandler.js");

const envConfig = parseServiceConfigFromEnv();

/**
 * Language configuration for the config language.
 */
const configLanguageConfig: LanguageServiceConfig<ConfigAdditionalServices> = {
    languagePlugin: configLanguagePlugin,
    languagePluginProvider: configPluginProvider,
    serviceModule: {
        references: {
            ScopeProvider: (services) => new DefaultScopeProvider(services)
        }
    },
    fileDataHandlers: {
        [CONFIG_DATA_KEY]: configDataHandler
    }
};

const config: ServiceConfig<any> = {
    ...envConfig,
    plugin: configServicePlugin,
    languages: [configLanguageConfig]
};

await startLanguageService(config);

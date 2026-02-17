import { Settings2 } from "lucide";
import { convertIcon } from "@mdeo/language-common";
import type { ExternalReferenceAdditionalServices } from "@mdeo/language-common";
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

const icon = convertIcon(Settings2);

/**
 * Language plugin definition for the config-optimization language.
 * This is a generated language that provides services for optimization sections.
 */
const configOptimizationLanguagePlugin: LanguagePlugin = {
    id: "config-optimization",
    name: "Config Optimization",
    extension: ".config_opt",
    newFileAction: false,
    icon,
    serverPlugin: {
        import: "static/language.js"
    },
    graphicalEditorPlugin: undefined,
    textualEditorPlugin: undefined,
    isGenerated: true
};

initializePluginContext();

const { configOptimizationPluginProvider, createOptimizationContributionPlugin } = await import("@mdeo/language-config-optimization");

/**
 * Plugin definition for the config-optimization service.
 */
const configOptimizationServicePlugin: ServicePluginDefinition = {
    id: "config-optimization-service",
    name: "Config Optimization",
    description: "Language support for config optimization sections",
    icon,
    languagePlugins: [configOptimizationLanguagePlugin],
    contributionPlugins: [{
        languageId: "config",
        description: "Provides optimization section support for config language",
        additionalKeywords: [],
        serverContributionPlugins: [createOptimizationContributionPlugin()]
    }]
};

const envConfig = parseServiceConfigFromEnv();

/**
 * Language configuration for the config-optimization language.
 */
const configOptimizationLanguageConfig: LanguageServiceConfig<ExternalReferenceAdditionalServices> = {
    languagePlugin: configOptimizationLanguagePlugin,
    languagePluginProvider: configOptimizationPluginProvider,
    handlers: {
        [AST_HANDLER_KEY]: astHandler
    }
};

const config: ServiceConfig<any> = {
    ...envConfig,
    plugin: configOptimizationServicePlugin,
    languages: [configOptimizationLanguageConfig]
};

await startLanguageService(config);
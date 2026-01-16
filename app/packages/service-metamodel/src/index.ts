import { Network } from "lucide";
import {
    defaultLanguageConfiguration,
    defaultMonarchTokenProvider,
    serializeMonarchTokensProvider
} from "@mdeo/language-common";
import {
    startLanguageService,
    convertIcon,
    parseServiceConfigFromEnv,
    type ServiceConfig,
    type ServicePluginDefinition,
    initializePluginContext,
    astHandler,
    AST_HANDLER_KEY
} from "@mdeo/service-common";
import type { MetamodelServices } from "@mdeo/language-metamodel";

/**
 * Plugin definition for the metamodel service
 */
const metamodelServicePlugin: ServicePluginDefinition = {
    id: "metamodel-service",
    name: "Metamodel",
    description: "Language support for metamodel definitions (.mm files)",
    icon: convertIcon(Network),
    languagePlugin: {
        id: "metamodel",
        name: "Metamodel",
        extension: ".mm",
        defaultContent: undefined,
        icon: convertIcon(Network),
        serverPlugin: {
            import: "static/language.js"
        },
        editorPlugin: {
            import: "static/editor.js",
            stylesUrl: "static/styles.css"
        },
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: serializeMonarchTokensProvider({
            ...defaultMonarchTokenProvider,
            keywords: ["class", "extends", "abstract", "import", "from", "as"]
        })
    },
    contributionPlugins: []
};

initializePluginContext();

const { metamodelPluginProvider } = await import("@mdeo/language-metamodel");

const envConfig = parseServiceConfigFromEnv();

const config: ServiceConfig<MetamodelServices> = {
    ...envConfig,
    plugin: metamodelServicePlugin,
    languagePluginProvider: metamodelPluginProvider,
    handlers: {
        [AST_HANDLER_KEY]: astHandler
    }
};

await startLanguageService(config);

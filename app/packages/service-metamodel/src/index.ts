import { Network } from "lucide";
import {
    defaultLanguageConfiguration,
    defaultMonarchTokenProvider,
    serializeMonarchTokensProvider
} from "@mdeo/language-common";
import {
    startLanguageService,
    parseServiceConfigFromEnv,
    type ServiceConfig,
    type ServicePluginDefinition,
    initializePluginContext,
    astHandler,
    AST_HANDLER_KEY
} from "@mdeo/service-common";
import { convertIcon } from "@mdeo/language-common";
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
        icon: convertIcon(Network),
        serverPlugin: {
            import: "static/language.js"
        },
        graphicalEditorPlugin: {
            import: "static/editor.js",
            stylesUrl: "static/styles.css"
        },
        textualEditorPlugin: {
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: serializeMonarchTokensProvider({
                ...defaultMonarchTokenProvider,
                keywords: ["class", "extends", "abstract", "import", "from", "as", "enum"]
            })
        }
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

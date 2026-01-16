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
import type { ModelServices } from "@mdeo/language-model";

/**
 * Plugin definition for the model service
 */
const modelServicePlugin: ServicePluginDefinition = {
    id: "model-service",
    name: "Model",
    description: "Language support for model definitions (.m files)",
    icon: convertIcon(Network).map((entry) => {
        if (entry[0] === "rect") {
            return [
                "rect",
                {
                    ...entry[1],
                    fill: "currentColor"
                }
            ];
        } else {
            return entry;
        }
    }),
    languagePlugin: {
        id: "model",
        name: "Model",
        extension: ".m",
        defaultContent: undefined,
        icon: convertIcon(Network).map((entry) => {
            if (entry[0] === "rect") {
                return [
                    "rect",
                    {
                        ...entry[1],
                        fill: "currentColor"
                    }
                ];
            } else {
                return entry;
            }
        }),
        serverPlugin: {
            import: "static/language.js"
        },
        editorPlugin: undefined,
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: serializeMonarchTokensProvider({
            ...defaultMonarchTokenProvider,
            keywords: ["using"]
        })
    },
    contributionPlugins: []
};

initializePluginContext();

const { modelPluginProvider } = await import("@mdeo/language-model");

const envConfig = parseServiceConfigFromEnv();

const config: ServiceConfig<ModelServices> = {
    ...envConfig,
    plugin: modelServicePlugin,
    languagePluginProvider: modelPluginProvider,
    handlers: {
        [AST_HANDLER_KEY]: astHandler
    }
};

await startLanguageService(config);

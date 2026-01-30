import { Network } from "lucide";
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
    initializePluginContext,
    astHandler,
    AST_HANDLER_KEY,
    startLanguageService
} from "@mdeo/service-common";
import { convertIcon } from "@mdeo/language-common";
import type { ModelServices } from "@mdeo/language-model";

const icon: ActionIconNode = convertIcon(Network).map((entry) => {
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
});

/**
 * Plugin definition for the model service
 */
const modelServicePlugin: ServicePluginDefinition = {
    id: "model-service",
    name: "Model",
    description: "Language support for model definitions (.m files)",
    icon,
    languagePlugin: {
        id: "model",
        name: "Model",
        extension: ".m",
        newFileAction: true,
        icon,
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
                keywords: ["using"]
            })
        }
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

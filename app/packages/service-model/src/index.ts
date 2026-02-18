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
    type LanguageServiceConfig,
    initializePluginContext,
    astHandler,
    AST_HANDLER_KEY,
    startLanguageService
} from "@mdeo/service-common";
import { convertIcon } from "@mdeo/language-common";
import type { ModelServices, GeneratedModelServices } from "@mdeo/language-model";
import type { LanguagePlugin } from "@mdeo/plugin";

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
 * Language plugin definition for the model language.
 */
const modelLanguagePlugin: LanguagePlugin = {
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
        stylesUrl: "static/styles.css",
        stylesCls: "editor-model"
    },
    textualEditorPlugin: {
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: serializeMonarchTokensProvider({
            ...defaultMonarchTokenProvider,
            keywords: ["using"]
        })
    },
    isGenerated: false
};

/**
 * Language plugin definition for the generated model language.
 */
const generatedModelLanguagePlugin: LanguagePlugin = {
    id: "model_gen",
    name: "Generated Model",
    extension: ".m_gen",
    newFileAction: false,
    icon,
    serverPlugin: {
        import: "static/generatedLanguage.js"
    },
    graphicalEditorPlugin: {
        import: "static/editor.js",
        stylesUrl: "static/styles.css",
        stylesCls: "editor-model"
    },
    textualEditorPlugin: undefined,
    isGenerated: true
};

/**
 * Plugin definition for the model service.
 */
const modelServicePlugin: ServicePluginDefinition = {
    id: "model-service",
    name: "Model",
    description: "Language support for model definitions (.m and .m_gen files)",
    icon,
    languagePlugins: [modelLanguagePlugin, generatedModelLanguagePlugin],
    contributionPlugins: []
};

initializePluginContext();

const { modelPluginProvider } = await import("@mdeo/language-model");
const { generatedModelPluginProvider } = await import("@mdeo/language-model");
const { modelDataHandler, MODEL_DATA_HANDLER_KEY } = await import("./handler/modelDataHandler.js");

const envConfig = parseServiceConfigFromEnv();

/**
 * Language configuration for the model language.
 */
const modelLanguageConfig: LanguageServiceConfig<ModelServices> = {
    languagePlugin: modelLanguagePlugin,
    languagePluginProvider: modelPluginProvider,
    fileDataHandlers: {
        [AST_HANDLER_KEY]: astHandler,
        [MODEL_DATA_HANDLER_KEY]: modelDataHandler
    }
};

/**
 * Language configuration for the generated model language.
 */
const generatedModelLanguageConfig: LanguageServiceConfig<GeneratedModelServices> = {
    languagePlugin: generatedModelLanguagePlugin,
    languagePluginProvider: generatedModelPluginProvider,
    fileDataHandlers: {
        [AST_HANDLER_KEY]: astHandler
    }
};

const config: ServiceConfig<any> = {
    ...envConfig,
    plugin: modelServicePlugin,
    languages: [modelLanguageConfig, generatedModelLanguageConfig]
};

await startLanguageService(config);

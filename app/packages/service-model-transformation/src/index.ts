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
import type { ModelTransformationServices } from "@mdeo/language-model-transformation";

const icon: ActionIconNode = [
    [
        "path",
        {
            d: "M 19 15 L 22 18"
        }
    ],
    [
        "path",
        {
            d: "M 22 18 L 19 21"
        }
    ],
    [
        "path",
        {
            d: "M2 18h20"
        }
    ],
    [
        "path",
        {
            d: "M4.8 10.4V8.6a.6.6 0 0 1 .6-.6h7.2a.6.6 0 0 1 .6.6v1.8"
        }
    ],
    [
        "path",
        {
            d: "M9 8V5.6"
        }
    ],
    [
        "rect",
        {
            x: "11.4",
            y: "10.4",
            width: "3.6",
            height: "3.6",
            rx: ".6",
            fill: "currentColor"
        }
    ],
    [
        "rect",
        {
            x: "3",
            y: "10.4",
            width: "3.6",
            height: "3.6",
            rx: ".6",
            fill: "currentColor"
        }
    ],
    [
        "rect",
        {
            x: "7.2",
            y: "2",
            width: "3.6",
            height: "3.6",
            rx: ".6",
            fill: "currentColor"
        }
    ]
];

/**
 * Plugin definition for the model transformation service.
 */
const modelTransformationServicePlugin: ServicePluginDefinition = {
    id: "model-transformation-service",
    name: "Model Transformation",
    description: "Language support for model transformation definitions (.mt files)",
    icon,
    languagePlugin: {
        id: "model-transformation",
        name: "Model Transformation",
        extension: ".mt",
        newFileAction: true,
        icon,
        serverPlugin: {
            import: "static/language.js"
        },
        textualEditorPlugin: {
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: serializeMonarchTokensProvider({
                ...defaultMonarchTokenProvider,
                keywords: [
                    "using",
                    "match",
                    "if",
                    "then",
                    "else",
                    "while",
                    "until",
                    "for",
                    "do",
                    "var",
                    "create",
                    "delete",
                    "forbid",
                    "where",
                    "kill",
                    "stop",
                    "true",
                    "false",
                    "null"
                ]
            })
        },
        graphicalEditorPlugin: undefined
    },
    contributionPlugins: []
};

initializePluginContext();

const { modelTransformationPluginProvider } = await import("@mdeo/language-model-transformation");

const envConfig = parseServiceConfigFromEnv();

const config: ServiceConfig<ModelTransformationServices> = {
    ...envConfig,
    plugin: modelTransformationServicePlugin,
    languagePluginProvider: modelTransformationPluginProvider,
    handlers: {
        [AST_HANDLER_KEY]: astHandler
    }
};

await startLanguageService(config);

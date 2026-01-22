import { FileCode } from "lucide";
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
    AST_HANDLER_KEY,
    astHandler
} from "@mdeo/service-common";
import { convertIcon } from "@mdeo/language-common";
import type { ScriptServices } from "@mdeo/language-script";

/**
 * Plugin definition for the script service
 */
const scriptServicePlugin: ServicePluginDefinition = {
    id: "script-service",
    name: "Script",
    description: "Language support for script definitions (.s files)",
    icon: convertIcon(FileCode),
    languagePlugin: {
        id: "script",
        name: "Script",
        extension: ".fn",
        icon: convertIcon(FileCode),
        serverPlugin: {
            import: "static/language.js"
        },
        graphicalEditorPlugin: undefined,
        textualEditorPlugin: {
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: serializeMonarchTokensProvider({
                ...defaultMonarchTokenProvider,
                keywords: [
                    "import",
                    "from",
                    "as",
                    "fun",
                    "return",
                    "if",
                    "else",
                    "while",
                    "for",
                    "break",
                    "continue",
                    "var",
                    "true",
                    "false",
                    "null"
                ]
            })
        }
    },
    contributionPlugins: []
};

initializePluginContext();

const { scriptPluginProvider } = await import("@mdeo/language-script");
const { typedAstHandler, TYPED_AST_HANDLER_KEY } = await import("./handler/typedAstHandler.js");

const envConfig = parseServiceConfigFromEnv();

const config: ServiceConfig<ScriptServices> = {
    ...envConfig,
    plugin: scriptServicePlugin,
    languagePluginProvider: scriptPluginProvider,
    handlers: {
        [AST_HANDLER_KEY]: astHandler,
        [TYPED_AST_HANDLER_KEY]: typedAstHandler
    }
};

await startLanguageService(config);

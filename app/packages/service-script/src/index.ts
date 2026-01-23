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
const { ScriptExecutionHandler } = await import("./handler/scriptExecutionHandler.js");

const envConfig = parseServiceConfigFromEnv();

/**
 * URL of the script-execution backend service.
 * Defaults to http://localhost:8081 if not specified.
 */
const scriptExecutionServiceUrl = process.env.SCRIPT_EXECUTION_SERVICE_URL ?? "http://localhost:8081";

/**
 * Execution handler for script executions.
 * Forwards execution requests to the script-execution backend service.
 */
const scriptExecutionHandler = new ScriptExecutionHandler(scriptExecutionServiceUrl);

const config: ServiceConfig<ScriptServices> = {
    ...envConfig,
    plugin: scriptServicePlugin,
    languagePluginProvider: scriptPluginProvider,
    handlers: {
        [AST_HANDLER_KEY]: astHandler,
        [TYPED_AST_HANDLER_KEY]: typedAstHandler
    },
    executionHandlers: [scriptExecutionHandler]
};

await startLanguageService(config);

/**
 * Export the execution handler for external access
 */
export { scriptExecutionHandler };

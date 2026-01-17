import { FileCode } from "lucide";
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
    initializePluginContext
} from "@mdeo/service-common";
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
        extension: ".s",
        defaultContent: undefined,
        icon: convertIcon(FileCode),
        serverPlugin: {
            import: "static/language.js"
        },
        editorPlugin: undefined,
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: serializeMonarchTokensProvider({
            ...defaultMonarchTokenProvider,
            keywords: [
                "import",
                "from",
                "as",
                "function",
                "return",
                "if",
                "else",
                "while",
                "for",
                "break",
                "continue",
                "let",
                "const",
                "true",
                "false",
                "null",
                "new",
                "this"
            ]
        })
    },
    contributionPlugins: []
};

initializePluginContext();

const { scriptPluginProvider } = await import("@mdeo/language-script");

const envConfig = parseServiceConfigFromEnv();

const config: ServiceConfig<ScriptServices> = {
    ...envConfig,
    plugin: scriptServicePlugin,
    languagePluginProvider: scriptPluginProvider,
    handlers: {}
};

await startLanguageService(config);

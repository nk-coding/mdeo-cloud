import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import { convertIcon } from "@/lib/convertIcon";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Code } from "lucide";
import scriptServerPluginUrl from "../modules/scriptPlugin.js?url";

/**
 * Plugin for script language support (.fn files).
 * Provides syntax highlighting, language configuration, and LSP integration for script files.
 */
export const scriptPlugin: ResolvedWorkbenchLanguagePlugin = {
    id: "script",
    extension: ".fn",
    name: "Script",
    serverPlugin: {
        import: scriptServerPluginUrl
    },
    editorPlugin: undefined,
    languageConfiguration: defaultLanguageConfiguration,
    monarchTokensProvider: {
        ...defaultMonarchTokenProvider,
        keywords: [
            "true",
            "false",
            "if",
            "else",
            "while",
            "do",
            "for",
            "in",
            "var",
            "return",
            "fun",
            "import",
            "from",
            "as"
        ]
    },
    icon: convertIcon(Code),
    contributionPlugins: []
};

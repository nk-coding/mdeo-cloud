import type { Plugin } from "@/data/plugin/plugin";
import { convertIcon } from "@/lib/convertIcon";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Code } from "lucide";

/**
 * Plugin for script language support (.fn files).
 * Provides syntax highlighting, language configuration, and LSP integration for script files.
 */
export const scriptPlugin: Plugin = {
    id: "script-plugin",
    languagePlugins: [
        {
            id: "script",
            extension: ".fn",
            name: "Script",
            serverPlugin: {
                import: "/modules/scriptPlugin.js"
            },
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
            icon: convertIcon(Code)
        }
    ],
    serverContributionPlugins: [],
};

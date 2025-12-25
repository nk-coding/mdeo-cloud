import type { Plugin } from "@/data/plugin/plugin";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";

export const examplePlugin: Plugin = {
    id: "example-plugin",
    languagePlugins: [
        {
            id: "metamodel",
            extension: ".mm",
            name: "Metamodel",
            serverPlugin: {
                import: "/modules/metamodelPlugin.js"
            },
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: {
                ...defaultMonarchTokenProvider,
                keywords: ["class", "extends", "abstract", "import", "from"]
            }
        }
    ],
    serverContributionPlugins: []
};

export const examplePlugin2: Plugin = {
    id: "example-plugin-2",
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
                    // Literals
                    "true",
                    "false",
                    // Statements
                    "if",
                    "else",
                    "while",
                    "do",
                    "for",
                    "in",
                    "var",
                    // Functions
                    "fun",
                    // Imports
                    "import",
                    "from",
                    "as"
                ]
            }
        }
    ],
    serverContributionPlugins: []
};

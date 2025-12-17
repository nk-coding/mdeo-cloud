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
                import: "/modules/defaultPlugin.js"
            },
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: {
                ...defaultMonarchTokenProvider,
                keywords: ["class", "extends", "abstract"]
            }
        }
    ],
    serverContributionPlugins: []
};

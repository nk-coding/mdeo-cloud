import type { Plugin } from "@/data/plugin/plugin";

export const examplePlugin: Plugin = {
    id: "example-plugin",
    languagePlugins: [
        {
            id: "metamodel",
            extension: ".mm",
            name: "Metamodel",
            serverPlugin: {
                import: "/modules/defaultPlugin.js"
            }
        }
    ],
    serverContributionPlugins: []
};

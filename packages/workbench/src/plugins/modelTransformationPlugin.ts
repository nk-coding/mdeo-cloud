import type { WorkbenchPlugin } from "@/data/plugin/plugin";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";

/**
 * Plugin for model transformation language support (.mt files).
 * Provides syntax highlighting, language configuration, and LSP integration for model transformation files.
 */
export const modelTransformationPlugin: WorkbenchPlugin = {
    id: "model-transformation-plugin",
    languagePlugins: [
        {
            id: "model-transformation",
            extension: ".mt",
            name: "Model Transformation",
            serverPlugin: {
                import: "/modules/metamodelPlugin.js"
            },
            editorPlugin: undefined,
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: {
                ...defaultMonarchTokenProvider,
                keywords: []
            },
            icon: [
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
            ]
        }
    ],
    serverContributionPlugins: []
};

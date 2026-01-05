import type { WorkbenchPlugin } from "@/data/plugin/plugin";
import { convertIcon } from "@/lib/convertIcon";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Network } from "lucide";

/**
 * Plugin for model language support (.m files).
 * Provides syntax highlighting, language configuration, and LSP integration for model files.
 */
export const modelPlugin: WorkbenchPlugin = {
    id: "model-plugin",
    languagePlugins: [
        {
            id: "model",
            extension: ".m",
            name: "Model",
            serverPlugin: {
                import: "/modules/modelPlugin.js"
            },
            editorPlugin: undefined,
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: {
                ...defaultMonarchTokenProvider,
                keywords: []
            },
            icon: convertIcon(Network).map((entry) => {
                if (entry[0] === "rect") {
                    return [
                        "rect",
                        {
                            ...entry[1],
                            fill: "currentColor"
                        }
                    ];
                } else {
                    return entry;
                }
            })
        }
    ],
    serverContributionPlugins: []
};

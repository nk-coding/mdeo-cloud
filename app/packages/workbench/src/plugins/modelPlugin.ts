import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import { convertIcon } from "@/lib/convertIcon";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Network } from "lucide";
import modelServerPluginUrl from "../modules/modelPlugin.js?url";

/**
 * Plugin for model language support (.m files).
 * Provides syntax highlighting, language configuration, and LSP integration for model files.
 */
export const modelPlugin: ResolvedWorkbenchLanguagePlugin = {
    id: "model",
    extension: ".m",
    name: "Model",
    serverPlugin: {
        import: modelServerPluginUrl
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
    }),
    contributionPlugins: []
};

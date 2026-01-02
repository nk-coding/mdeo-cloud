import type { WorkbenchPlugin } from "@/data/plugin/plugin";
import { convertIcon } from "@/lib/convertIcon";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Network } from "lucide";
import { metamodelEditorPlugin } from "@mdeo/editor-metamodel";

/**
 * Plugin for metamodel language support (.mm files).
 * Provides syntax highlighting, language configuration, and LSP integration for metamodel definitions.
 */
export const metamodelPlugin: WorkbenchPlugin = {
    id: "metamodel-plugin",
    languagePlugins: [
        {
            id: "metamodel",
            extension: ".mm",
            name: "Metamodel",
            serverPlugin: {
                import: "/modules/metamodelPlugin.js"
            },
            editorPlugin: metamodelEditorPlugin,
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: {
                ...defaultMonarchTokenProvider,
                keywords: ["class", "extends", "abstract", "import", "from"]
            },
            icon: convertIcon(Network)
        }
    ],
    serverContributionPlugins: []
};

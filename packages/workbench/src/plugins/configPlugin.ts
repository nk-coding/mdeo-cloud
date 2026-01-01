import type { Plugin } from "@/data/plugin/plugin";
import { convertIcon } from "@/lib/convertIcon";
import { defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Settings2 } from "lucide";

/**
 * Plugin for configuration language support (.config files).
 * Provides syntax highlighting, language configuration, and LSP integration for configuration files.
 */
export const configPlugin: Plugin = {
    id: "config-plugin",
    languagePlugins: [
        {
            id: "config",
            extension: ".config",
            name: "Config",
            serverPlugin: {
                import: "/modules/metamodelPlugin.js"
            },
            languageConfiguration: defaultLanguageConfiguration,
            monarchTokensProvider: {
                ...defaultMonarchTokenProvider,
                keywords: []
            },
            icon: convertIcon(Settings2)
        }
    ],
    serverContributionPlugins: []
};

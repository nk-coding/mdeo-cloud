import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";
import { convertIcon, defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Settings2 } from "lucide";
import configServerPluginUrl from "../modules/configPlugin.js?url";

/**
 * Plugin for configuration language support (.config files).
 * Provides syntax highlighting, language configuration, and LSP integration for configuration files.
 */
export const configPlugin: ResolvedWorkbenchLanguagePlugin = {
    id: "config",
    extension: ".config",
    name: "Config",
    serverPlugin: {
        import: configServerPluginUrl
    },
    graphicalEditorPlugin: undefined,
    textualEditorPlugin: {
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: {
            ...defaultMonarchTokenProvider,
            keywords: []
        }
    },
    icon: convertIcon(Settings2),
    contributionPlugins: []
};

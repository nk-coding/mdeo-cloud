import { convertIcon, defaultLanguageConfiguration, defaultMonarchTokenProvider } from "@mdeo/language-common";
import { Network } from "lucide";
import { metamodelEditorPlugin } from "@mdeo/editor-metamodel";
import editorMetamodelStylesUrl from "@mdeo/editor-metamodel/styles?url";
import metamodelServerPluginUrl from "../modules/metamodelPlugin.js?url";
import type { ResolvedWorkbenchLanguagePlugin } from "@/data/plugin/plugin";

/**
 * Plugin for metamodel language support (.mm files).
 * Provides syntax highlighting, language configuration, and LSP integration for metamodel definitions.
 */
export const metamodelPlugin: ResolvedWorkbenchLanguagePlugin = {
    id: "metamodel",
    extension: ".mm",
    name: "Metamodel",
    serverPlugin: {
        import: metamodelServerPluginUrl
    },
    graphicalEditorPlugin: {
        containerConfiguration: metamodelEditorPlugin,
        stylesUrl: editorMetamodelStylesUrl
    },
    textualEditorPlugin: {
        languageConfiguration: defaultLanguageConfiguration,
        monarchTokensProvider: {
            ...defaultMonarchTokenProvider,
            keywords: ["class", "extends", "abstract", "import", "from", "as"]
        }
    },
    icon: convertIcon(Network),
    contributionPlugins: []
};

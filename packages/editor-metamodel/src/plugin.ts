import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import type { EditorPlugin, PluginContext } from "@mdeo/editor-common";

/**
 * Editor plugin for the metamodel editor
 */
export const metamodelEditorPlugin: EditorPlugin = {
    configure: function (context: PluginContext): ContainerConfiguration {
        return [];
    }
};

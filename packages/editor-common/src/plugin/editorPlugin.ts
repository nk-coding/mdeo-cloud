import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";
import type { PluginContext } from "./pluginContext.js";

/**
 * Interface for GLSP editor plugins
 */
export interface EditorPlugin {
    /**
     * Called when the GLSP module is being configured
     *
     * @param context the plugin context used to access shared dependencies
     * @return an array of container configurations to be applied
     */
    configure(context: PluginContext): ContainerConfiguration;
}

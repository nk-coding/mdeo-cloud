import type { ContainerConfiguration } from "@eclipse-glsp/sprotty";

/**
 * Plugin configuration for a graphical editor.
 * Contains both the container configuration and the URL to the styles.
 */
export interface EditorPlugin {
    /**
     * Container configuration for the GLSP/Sprotty container.
     * This configures the model elements, views, and other diagram features.
     */
    containerConfiguration: ContainerConfiguration;

    /**
     * URL to the CSS styles for this editor.
     * This should be a URL that can be loaded by the browser.
     */
    stylesUrl: string;
}

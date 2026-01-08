import { sharedImport } from "../sharedImport.js";

const { GChildElement } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for HTML text labels.
 * Contains text content that can be rendered as HTML.
 */
export class GLabel extends GChildElement {
    /**
     * The text content to be displayed
     */
    text!: string;
}

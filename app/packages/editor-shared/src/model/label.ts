import type { EditLabelValidationResult } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";

const { GChildElement, editLabelFeature } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Client-side model for HTML text labels.
 * Contains text content that can be rendered as HTML.
 */
export class GLabel extends GChildElement {
    static readonly DEFAULT_FEATURES = [editLabelFeature];

    /**
     * The text content to be displayed
     */
    text!: string;

    /**
     * Indicates if the label is read-only
     */
    readonly?: boolean;

    /**
     * Indicates if the label is currently in edit mode
     */
    editMode: boolean = false;

    /**
     * Indicates if the label should be rendered as multiline (textarea instead of input)
     */
    isMultiLine?: boolean;

    /**
     * Temporarily stores the edited text before applying
     */
    tempText?: string;

    /**
     * Stores the validation result for the current edit
     */
    validationResult?: EditLabelValidationResult;
}

import type { EditableLabel as SprottyEditableLabel, GModelElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { isEditableLabel, isWithEditableLabel } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Extended editable label interface with read-only support.
 */
export interface EditableLabel extends SprottyEditableLabel {
    /**
     * Indicates if the label is read-only
     */
    readonly?: boolean;
}

/**
 * Gets the editable label from a model element if available and not read-only.
 *
 * @param element The model element to check
 * @returns The editable label or undefined if not available or read-only
 */
export function getEditableLabel(element: GModelElement): (EditableLabel & GModelElement) | undefined {
    if (isEditableLabel(element) && (element as EditableLabel).readonly !== true) {
        return element;
    } else if (
        isWithEditableLabel(element) &&
        element.editableLabel &&
        (element.editableLabel as EditableLabel).readonly !== true
    ) {
        return element.editableLabel;
    }
    return undefined;
}

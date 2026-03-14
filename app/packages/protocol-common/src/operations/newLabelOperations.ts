import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Base interface for operations that create new labels in the model.
 * Extends the standard GLSP Operation with properties specific to new label creation.
 *
 * New label operations are dispatched when a user creates a new element (e.g., property, enum entry)
 * via a context action and commits the label text. Unlike ApplyLabelEditOperation which modifies
 * existing labels, these operations create both the element and its label atomically on the server.
 *
 * Server-side handlers should:
 * 1. Validate the labelText
 * 2. Create the domain model element
 * 3. Set the element's label/name to labelText
 * 4. Add the element to the parent element
 * 5. Dispatch SetModelAction with the updated model
 */
export interface NewLabelOperation extends Operation {
    /**
     * The ID of the parent element that will contain the newly created element.
     * For example, when creating a property, this is the containing class ID.
     */
    parentElementId: string;

    /**
     * The text that will be assigned to the new element's label.
     * For example, the property name typed by the user.
     */
    labelText: string;
}

import type { Operation } from "@eclipse-glsp/protocol";

/**
 * Operation to trigger an action dialog from the GLSP client.
 * This operation is sent to the server when an action needs to be triggered
 * from the diagram editor (e.g., via context menu or toolbar).
 */
export interface TriggerActionOperation extends Operation {
    kind: "triggerAction";

    /**
     * The type of action to trigger (e.g., "execute", "new-file").
     * This value maps to the `type` field in `ActionStartParams` when
     * the operation is forwarded to the workbench via LSP notification.
     */
    actionType: string;

    /**
     * The language identifier for which this action is triggered
     */
    languageId: string;

    /**
     * Additional data specific to the action type.
     * The structure depends on the actionType.
     */
    data: unknown;
}

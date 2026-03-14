import type { Action, GModelElementSchema } from "@eclipse-glsp/protocol";

/**
 * Action to locally insert one or more pre-built GModel element templates into the
 * diagram for inline editing of a new label.
 *
 * The server is responsible for determining the full template structure (including
 * whether compartments, dividers, or wrapper nodes are needed) and encodes that
 * structure directly in {@link templates}.  The client inserts the templates at the
 * given {@link insertIndex} inside {@link parentElementId} without needing to know
 * anything about compartment IDs or wrapper node types.
 *
 * At least one element in {@link templates} must be (or contain) a label element
 * whose ID matches {@link labelId}.  That label must carry `editMode: true`,
 * `isNewLabel: true`, `newLabelOperationKind`, and `parentElementId` so that the
 * label view can auto-focus and dispatch the correct server operation on commit.
 *
 * When the edit is cancelled or committed, a {@link RemoveNewLabelAction} removes
 * all top-level template elements by their IDs.
 */
export interface InsertNewLabelAction extends Action {
    kind: typeof InsertNewLabelAction.KIND;

    /**
     * The ID of the GModel element into whose `children` the templates are inserted.
     */
    parentElementId: string;

    /**
     * The index in the parent's `children` array at which the templates are spliced in.
     */
    insertIndex: number;

    /**
     * One or more pre-built GModel element schemas to insert.
     * The server constructs these using the same builders used by the diagram's
     * GModelFactory, so the visual result matches exactly what a persisted element
     * would look like.
     */
    templates: GModelElementSchema[];

    /**
     * The ID of the label element within {@link templates} (possibly nested inside a
     * compartment or wrapper node).  The insertion command uses this ID to locate
     * the label after insertion and to populate {@link GLabel.insertedElementIds} and
     * {@link GLabel.containerElementId} for use by {@link RemoveNewLabelAction}.
     */
    labelId: string;
}

export namespace InsertNewLabelAction {
    export const KIND = "insertNewLabel";

    /**
     * Creates an {@link InsertNewLabelAction}.
     *
     * @param options The action payload (all fields except kind).
     * @returns A new insert-new-label action.
     */
    export function create(options: Omit<InsertNewLabelAction, "kind">): InsertNewLabelAction {
        return { kind: KIND, ...options };
    }
}

/**
 * Action to remove previously inserted template elements from the GModel.
 *
 * Dispatched when a new-label edit is cancelled or committed.  On commit it is
 * paired with the corresponding server operation; on cancel it is dispatched alone.
 *
 * All IDs in {@link insertedElementIds} are direct children of
 * {@link containerElementId} and are removed from it.
 */
export interface RemoveNewLabelAction extends Action {
    kind: typeof RemoveNewLabelAction.KIND;

    /**
     * The IDs of all root-level template elements that were inserted by the
     * corresponding {@link InsertNewLabelAction}.  These are direct children of
     * {@link containerElementId}.
     */
    insertedElementIds: string[];

    /**
     * The ID of the GModel element that directly contains the inserted elements.
     * Used both to locate the elements for removal and to trigger a bounds
     * recalculation on the enclosing top-level node afterward.
     */
    containerElementId: string;
}

export namespace RemoveNewLabelAction {
    export const KIND = "removeNewLabel";

    /**
     * Creates a {@link RemoveNewLabelAction}.
     *
     * @param options The action payload (all fields except kind).
     * @returns A new remove-new-label action.
     */
    export function create(options: Omit<RemoveNewLabelAction, "kind">): RemoveNewLabelAction {
        return { kind: KIND, ...options };
    }
}

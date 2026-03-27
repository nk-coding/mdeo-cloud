import type { Action } from "@eclipse-glsp/sprotty";

/**
 * Action dispatched by {@link IssueMarkerMouseListener} when the mouse enters
 * the area of an issue-marker badge.
 *
 * The UI extension handles this action by making the active marker's popup
 * panel visible.
 */
export interface IssueMarkerHoverEnterAction extends Action {
    kind: typeof IssueMarkerHoverEnterAction.KIND;
    /**
     * Model ID of the {@link GIssueMarker} whose badge the mouse entered.
     */
    markerId: string;
}

export namespace IssueMarkerHoverEnterAction {
    export const KIND = "issueMarkerHoverEnter";

    /**
     * Creates an {@link IssueMarkerHoverEnterAction} for the given marker.
     *
     * @param markerId The model ID of the issue marker whose badge was entered.
     * @returns A new action with kind {@link KIND}.
     */
    export function create(markerId: string): IssueMarkerHoverEnterAction {
        return { kind: KIND, markerId };
    }

    /**
     * Type-guard that narrows `action` to {@link IssueMarkerHoverEnterAction}.
     *
     * @param action Any action object.
     * @returns `true` when `action` is an {@link IssueMarkerHoverEnterAction}.
     */
    export function is(action: Action): action is IssueMarkerHoverEnterAction {
        return action.kind === KIND;
    }
}

/**
 * Action dispatched by {@link IssueMarkerMouseListener} when the mouse leaves
 * all issue-marker badge areas without entering a new one.
 *
 * The UI extension handles this action by hiding the active popup panel.
 * While the pointer is over the popup itself, pointer events on the SVG are
 * blocked by the panel, so no leave action is dispatched until the pointer
 * leaves both the badge and the popup.
 */
export interface IssueMarkerHoverLeaveAction extends Action {
    kind: typeof IssueMarkerHoverLeaveAction.KIND;
    /**
     * Model ID of the {@link GIssueMarker} whose badge the mouse left.
     */
    markerId: string;
}

export namespace IssueMarkerHoverLeaveAction {
    export const KIND = "issueMarkerHoverLeave";

    /**
     * Creates an {@link IssueMarkerHoverLeaveAction} for the given marker.
     *
     * @param markerId The model ID of the issue marker whose badge was left.
     * @returns A new action with kind {@link KIND}.
     */
    export function create(markerId: string): IssueMarkerHoverLeaveAction {
        return { kind: KIND, markerId };
    }

    /**
     * Type-guard that narrows `action` to {@link IssueMarkerHoverLeaveAction}.
     *
     * @param action Any action object.
     * @returns `true` when `action` is an {@link IssueMarkerHoverLeaveAction}.
     */
    export function is(action: Action): action is IssueMarkerHoverLeaveAction {
        return action.kind === KIND;
    }
}

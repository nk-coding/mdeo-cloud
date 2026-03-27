import type { Action, GModelElement } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { isIssueMarker } from "./issueMarker.js";
import { IssueMarkerHoverEnterAction, IssueMarkerHoverLeaveAction } from "./issueMarkerActions.js";
import type { IssueMarkerUIExtension } from "./issueMarkerUIExtension.js";
import { ISSUE_MARKER_UI_EXTENSION_TOKEN } from "./tokens.js";

const { injectable, inject } = sharedImport("inversify");
const { MouseListener } = sharedImport("@eclipse-glsp/sprotty");

/**
 * GLSP mouse listener that detects when the pointer enters or leaves an issue-marker
 * badge area and dispatches the appropriate hover actions to the action-handling
 * infrastructure.
 *
 * The sprotty `target` argument of `mouseMove` is the model element under the cursor.
 * When the transparent hit-area `<rect>` in the badge SVG is hit, sprotty resolves it
 * to the `GIssueMarker` model element, allowing direct detection without manual
 * badge-position hit-testing.
 *
 * On badge enter  → dispatches {@link IssueMarkerHoverEnterAction}
 * On badge leave  → dispatches {@link IssueMarkerHoverLeaveAction}
 *
 * Registered as `TYPES.MouseListener` via {@link decorationModule}.
 */
@injectable()
export class IssueMarkerMouseListener extends MouseListener {
    @inject(ISSUE_MARKER_UI_EXTENSION_TOKEN)
    private uiExtension!: IssueMarkerUIExtension;

    private lastEnteredMarkerId: string | undefined;

    override mouseMove(target: GModelElement, _event: MouseEvent): Action[] {
        if (isIssueMarker(target) && target.issues.length > 0) {
            if (target.id !== this.lastEnteredMarkerId) {
                this.lastEnteredMarkerId = target.id;
                return [IssueMarkerHoverEnterAction.create(target.id)];
            }
        } else if (this.lastEnteredMarkerId != null) {
            const leaveId = this.lastEnteredMarkerId;
            this.lastEnteredMarkerId = undefined;
            return [IssueMarkerHoverLeaveAction.create(leaveId)];
        }

        return [];
    }

    override mouseOut(_target: GModelElement, event: MouseEvent): Action[] {
        const relatedTarget = event.relatedTarget as Element | null;
        if (relatedTarget != null && this.uiExtension.isPopupElement(relatedTarget)) {
            return [];
        }
        if (this.lastEnteredMarkerId != null) {
            const leaveId = this.lastEnteredMarkerId;
            this.lastEnteredMarkerId = undefined;
            return [IssueMarkerHoverLeaveAction.create(leaveId)];
        }
        return [];
    }
}

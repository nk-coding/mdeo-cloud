import type { Action, GModelElement } from "@eclipse-glsp/sprotty";
import { RevealSourceAction } from "@mdeo/protocol-common";
import { sharedImport } from "../../sharedImport.js";
import { isIssueMarker } from "../decoration/issueMarker.js";

const { injectable } = sharedImport("inversify");
const { MouseListener } = sharedImport("@eclipse-glsp/sprotty");

/**
 * GLSP mouse listener that dispatches a {@link RevealSourceAction} in two cases:
 *
 * <ul>
 *   <li><b>Alt+click</b> on any graphical element → reveals the source of that element.</li>
 *   <li><b>Double-click</b> on an issue marker badge → reveals the source of the element
 *       that owns the marker (the parent element in the graph model).</li>
 * </ul>
 *
 * <p>On macOS both {@code Alt} and {@code Option} map to {@code event.altKey}, so no
 * additional platform handling is required.
 *
 * <p>Registered as {@code TYPES.MouseListener} via {@link revealSourceModule}.
 */
@injectable()
export class RevealSourceMouseListener extends MouseListener {
    /**
     * Handles mouse-down events. Dispatches {@link RevealSourceAction} when the alt key
     * is held (alt+click reveal source shortcut).
     *
     * @param target The graphical element under the cursor.
     * @param event The raw browser mouse event.
     * @returns A {@link RevealSourceAction} when alt is pressed, otherwise an empty array.
     */
    override mouseDown(target: GModelElement, event: MouseEvent): Action[] {
        if (event.altKey) {
            return [RevealSourceAction.create(this.resolveElementId(target))];
        }
        return [];
    }

    /**
     * Handles double-click events. Dispatches {@link RevealSourceAction} when the user
     * double-clicks an issue marker badge, revealing the source of the parent element.
     *
     * @param target The graphical element that was double-clicked.
     * @returns A {@link RevealSourceAction} when the target is an issue marker, else empty.
     */
    override doubleClick(target: GModelElement): Action[] {
        if (isIssueMarker(target)) {
            return [RevealSourceAction.create(this.resolveElementId(target))];
        }
        return [];
    }

    /**
     * Resolves the element ID to use for the reveal-source action.
     * For issue markers the parent element's ID is used; for all other elements the
     * element's own ID is returned.
     *
     * @param target The element to resolve.
     * @returns The ID of the element or its parent (for issue markers).
     */
    private resolveElementId(target: GModelElement): string {
        if (isIssueMarker(target) && target.parent != undefined) {
            return target.parent.id;
        }
        return target.id;
    }
}

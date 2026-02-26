import type { GModelRoot } from "@eclipse-glsp/sprotty";
import { GEdge } from "../../model/edge.js";
import type { EdgeEditTool } from "./edgeEditTool.js";
import { sharedImport } from "../../sharedImport.js";
import type { Action, GModelElement, ISelectionListener } from "@eclipse-glsp/client";

const { isSelected, DragAwareMouseListener } = sharedImport("@eclipse-glsp/client");

/**
 * Listener that handles edge selection management only.
 * When an edge is selected, registers feedback listeners.
 * When an edge is unselected, disposes feedback listeners.
 */
export class EdgeEditListener extends DragAwareMouseListener implements ISelectionListener {
    /**
     * The currently selected edge.
     */
    protected edge?: GEdge;

    constructor(protected tool: EdgeEditTool) {
        super();
    }

    /**
     * Sets the edge as selected for editing.
     */
    protected setEdgeSelected(edge: GEdge): void {
        this.edge = edge;
        this.tool.registerFeedbackListeners();
    }

    /**
     * Checks if an edge is currently selected.
     */
    protected isEdgeSelected(): boolean {
        return this.edge != undefined && isSelected(this.edge);
    }

    override mouseDown(target: GModelElement, event: MouseEvent): Action[] {
        const result: Action[] = super.mouseDown(target, event);
        if (event.button === 0) {
            if (target != this.edge) {
                if (this.isEdgeSelected()) {
                    this.dispose();
                }
                if (target instanceof GEdge) {
                    this.setEdgeSelected(target);
                }
            }
        } else if (event.button === 2) {
            this.dispose();
        }
        return result;
    }

    selectionChanged(root: Readonly<GModelRoot>, selectedElements: string[]): void {
        if (this.edge != undefined) {
            if (selectedElements.indexOf(this.edge.id) > -1) {
                return;
            }

            for (const elementId of selectedElements.reverse()) {
                const element = root.index.getById(elementId);
                if (element instanceof GEdge && isSelected(element)) {
                    this.setEdgeSelected(element);
                    return;
                }
            }

            this.dispose();
        }
    }

    override dispose(): void {
        this.edge = undefined;
        this.tool.deregisterFeedbackListeners();
        super.dispose();
    }
}

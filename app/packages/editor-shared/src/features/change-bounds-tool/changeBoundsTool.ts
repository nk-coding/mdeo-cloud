import type { ISelectionListener } from "@eclipse-glsp/client";
import type { MouseListener } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { ChangeBoundsListener } from "./changeBoundsListener.js";
import { FeedbackMoveMouseListener } from "./feedbackMoveMouseListener.js";
import { EdgeRouter } from "../edge-rourting/edgeRouter.js";

const { injectable, inject } = sharedImport("inversify");
const { ChangeBoundsTool: GLSPChangeBoundsTool } = sharedImport("@eclipse-glsp/client");

/**
 * Custom change bounds tool that supports custom resize handles and move behavior.
 * Overrides the default GLSP change bounds tool to work with custom SVG-based resize handles.
 */
@injectable()
export class ChangeBoundsTool extends GLSPChangeBoundsTool {
    /**
     * The edge router used to recalculate edge routings after bounds changes.
     */
    @inject(EdgeRouter) readonly edgeRouter!: EdgeRouter;

    /**
     * Creates a custom change bounds listener that handles resize operations.
     *
     * @returns The change bounds listener
     */
    protected override createChangeBoundsListener(): MouseListener & ISelectionListener {
        return new ChangeBoundsListener(this);
    }

    /**
     * Creates a custom move mouse listener that handles move operations.
     *
     * @returns The move mouse listener
     */
    protected override createMoveMouseListener(): MouseListener {
        return new FeedbackMoveMouseListener(this);
    }
}

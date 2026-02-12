import type { SetModelAction as SetModelActionType, UpdateModelAction } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import type { CommandExecutionContext, CommandReturn, IActionDispatcher } from "@eclipse-glsp/sprotty";
import { createFitToScreenAction } from "./fitToScreen.js";
import { GNode } from "../../model/node.js";
import { LayoutAction } from "../toolbox/layoutAction.js";

const { injectable, inject } = sharedImport("inversify");
const { Command, TYPES, SetModelAction, Point, isViewport } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Command that initializes the viewport layout when necessary
 */
@injectable()
export class InitializeViewportLayoutCommand extends Command {
    /**
     * Dispatcher to send additional actions if necessary, e.g., to fit the viewport to the content after initialization
     */
    @inject(TYPES.IActionDispatcher) protected readonly actionDispatcher!: IActionDispatcher;

    constructor(@inject(TYPES.Action) protected readonly action: SetModelActionType | UpdateModelAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const root = context.root;
        if (isViewport(root) && root.zoom === 0) {
            if (
                this.action.kind === SetModelAction.KIND &&
                [...root.index.all()].every(
                    (element) =>
                        !(element instanceof GNode) || Point.equals(Point.ORIGIN, element.meta.position ?? Point.ORIGIN)
                )
            ) {
                this.actionDispatcher.dispatch(LayoutAction.create());
            } else {
                console.log("fit to screen dispatched");
                this.actionDispatcher.dispatchOnceModelInitialized(
                    createFitToScreenAction(false, this.action.newRoot.children?.map((child) => child.id) ?? [])
                );
            }
        }
        return root;
    }

    undo(): CommandReturn {
        throw new Error("Method not implemented.");
    }

    redo(): CommandReturn {
        throw new Error("Method not implemented.");
    }
}

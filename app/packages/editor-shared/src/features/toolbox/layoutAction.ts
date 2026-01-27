import type { Action } from "@eclipse-glsp/protocol";
import { sharedImport } from "../../sharedImport.js";
import type { ActionDispatcher, CommandExecutionContext, CommandReturn } from "@eclipse-glsp/sprotty";
import type { LayoutOperation } from "@mdeo/editor-protocol";

const { injectable, inject } = sharedImport("inversify");
const { Command, TYPES, isBoundsAware } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Action to trigger layouting of the diagram
 */
export interface LayoutAction extends Action {
    kind: typeof LayoutAction.KIND;
}

export namespace LayoutAction {
    export const KIND = "layout-action";

    /**
     * Creates a new layout action
     *
     * @return the created layout action
     */
    export function create(): LayoutAction {
        return {
            kind: KIND
        };
    }
}

/**
 * Command to perform layouting of the diagram
 */
@injectable()
export class LayoutCommand extends Command {
    static readonly KIND = LayoutAction.KIND;

    /**
     * The action dispatcher, used to dispatch actions
     */
    @inject(TYPES.IActionDispatcher) protected readonly actionDispatcher!: ActionDispatcher;

    override execute(context: CommandExecutionContext): CommandReturn {
        const bounds = Object.fromEntries(
            [...context.root.index.all()]
                .filter(isBoundsAware)
                .map((element) => [element.id, { width: element.bounds.width, height: element.bounds.height }])
        );

        const operation: LayoutOperation = {
            kind: "layout",
            isOperation: true,
            bounds
        };
        this.actionDispatcher.dispatch(operation);
        return context.root;
    }

    override undo(): CommandReturn {
        throw new Error("Method not implemented.");
    }

    override redo(): CommandReturn {
        throw new Error("Method not implemented.");
    }
}

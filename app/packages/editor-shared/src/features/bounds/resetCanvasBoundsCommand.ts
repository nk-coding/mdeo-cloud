import type { CommandExecutionContext, CommandReturn } from "@eclipse-glsp/sprotty";
import type { ResetCanvasBoundsAction } from "@mdeo/editor-protocol";
import { sharedImport } from "../../sharedImport.js";

const { injectable, inject } = sharedImport("inversify");
const { Command, TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { Bounds } = sharedImport("@eclipse-glsp/protocol");

/**
 * Command for ResetCanvasBoundsAction.
 * Resets the canvas bounds to empty, forcing a recalculation on the next render.
 */
@injectable()
export class ResetCanvasBoundsCommand extends Command {
    static readonly KIND: ResetCanvasBoundsAction["kind"] = "resetCanvasBoundsAction";

    @inject(TYPES.Action) private action!: ResetCanvasBoundsAction;

    override execute(context: CommandExecutionContext): CommandReturn {
        context.root.canvasBounds = Bounds.EMPTY;
        return context.root;
    }

    override undo(context: CommandExecutionContext): CommandReturn {
        return context.root;
    }

    override redo(context: CommandExecutionContext): CommandReturn {
        return context.root;
    }
}

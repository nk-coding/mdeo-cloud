import type { CommandExecutionContext, CommandReturn, IActionDispatcher } from "@eclipse-glsp/sprotty";
import type { FeedbackCommand } from "@eclipse-glsp/client";
import { sharedImport } from "../../sharedImport.js";
import { hasNodeLayoutMetadata } from "./nodeMetadata.js";

const { injectable, inject } = sharedImport("inversify");
const { SetBoundsCommand, TYPES } = sharedImport("@eclipse-glsp/sprotty");
const { Ranked, SetBoundsFeedbackAction, LocalRequestBoundsAction } = sharedImport("@eclipse-glsp/client");

/**
 * Replacement for the GLSP SetBoundsFeedbackCommand that uses meta for preferred size storage.
 */
@injectable()
export class SetBoundsFeedbackCommand extends SetBoundsCommand implements FeedbackCommand {
    /** The action kind for set bounds feedback */
    static override readonly KIND: string = SetBoundsFeedbackAction.KIND;

    /** The rank of this feedback command */
    readonly rank: number = Ranked.DEFAULT_RANK;

    /** The action dispatcher for sending actions */
    @inject(TYPES.IActionDispatcher) protected actionDispatcher!: IActionDispatcher;

    /**
     * Executes the set bounds feedback command.
     * Updates the metadata of elements with their new preferred size and requests bounds update.
     *
     * @param context The command execution context
     * @returns The command return value with local bounds request
     */
    override execute(context: CommandExecutionContext): CommandReturn {
        super.execute(context);

        this.action.bounds.forEach((bounds) => {
            const element = context.root.index.getById(bounds.elementId);
            if (element != undefined && hasNodeLayoutMetadata(element)) {
                const meta = element.meta ?? {};
                meta.prefHeight = bounds.newSize.height;
                meta.prefWidth = bounds.newSize.width;
                element.meta = meta;
            }
        });
        const elementIDs = this.action.bounds.map((bounds) => bounds.elementId);
        return LocalRequestBoundsAction.fromCommand(context, this.actionDispatcher, this.action, elementIDs);
    }
}

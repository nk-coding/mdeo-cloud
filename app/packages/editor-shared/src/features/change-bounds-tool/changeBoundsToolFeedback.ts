import type { CommandExecutionContext, CommandReturn } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";

const { injectable } = sharedImport("inversify");
const { FeedbackCommand, ShowChangeBoundsToolResizeFeedbackAction, HideChangeBoundsToolResizeFeedbackAction } =
    sharedImport("@eclipse-glsp/client");

/**
 * No-op implementation which does not show resize handles
 */
@injectable()
export class NoopShowChangeBoundsToolResizeFeedbackCommand extends FeedbackCommand {
    static readonly KIND = ShowChangeBoundsToolResizeFeedbackAction.KIND;

    execute(context: CommandExecutionContext): CommandReturn {
        return context.root;
    }
}

/**
 * No-op implementation which does not show resize handles
 */
@injectable()
export class NoopHideChangeBoundsToolResizeFeedbackCommand extends FeedbackCommand {
    static readonly KIND = HideChangeBoundsToolResizeFeedbackAction.KIND;

    execute(context: CommandExecutionContext): CommandReturn {
        return context.root;
    }
}

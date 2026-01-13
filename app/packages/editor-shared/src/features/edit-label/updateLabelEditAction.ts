import { sharedImport } from "../../sharedImport.js";
import type { GLabel } from "../../model/label.js";
import type {
    Action,
    IActionDispatcher,
    CommandExecutionContext,
    CommandReturn,
    BoundsAware
} from "@eclipse-glsp/sprotty";

const { injectable, inject } = sharedImport("inversify");
const { Command, TYPES, findParentByFeature, isSizeable } = sharedImport("@eclipse-glsp/sprotty");
const { LocalRequestBoundsAction } = sharedImport("@eclipse-glsp/client");

export interface UpdateLabelEditAction extends Action {
    kind: "updateLabelEdit";
    labelId: string;
    tempText?: string;
    editMode: boolean;
}

export namespace UpdateLabelEditAction {
    export const KIND = "updateLabelEdit";

    export function create(labelId: string, editMode: boolean, tempText?: string): UpdateLabelEditAction {
        return {
            kind: KIND,
            labelId,
            editMode,
            tempText
        };
    }
}

/**
 * Command that updates a label's edit mode and temporary text.
 * When entering edit mode, requests bounds update to adjust the UI.
 */
@injectable()
export class UpdateLabelEditCommand extends Command {
    static readonly KIND = UpdateLabelEditAction.KIND;

    @inject(TYPES.IActionDispatcher) protected actionDispatcher!: IActionDispatcher;

    constructor(@inject(TYPES.Action) protected readonly action: UpdateLabelEditAction) {
        super();
    }

    override execute(context: CommandExecutionContext): CommandReturn {
        const label = context.root.index.getById(this.action.labelId) as GLabel;
        label.editMode = this.action.editMode;
        if (this.action.tempText != undefined) {
            label.tempText = this.action.tempText;
        } else if (!this.action.editMode) {
            label.tempText = undefined;
        }
        const resized = findParentByFeature<BoundsAware>(label, isSizeable);
        return LocalRequestBoundsAction.fromCommand(
            context,
            this.actionDispatcher,
            this.action,
            resized != undefined ? [resized.id] : undefined
        );
    }

    override undo(context: CommandExecutionContext): CommandReturn {
        throw new Error("Method not implemented.");
    }

    override redo(context: CommandExecutionContext): CommandReturn {
        throw new Error("Method not implemented.");
    }
}

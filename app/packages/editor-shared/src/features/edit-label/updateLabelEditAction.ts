import { sharedImport } from "../../sharedImport.js";
import type { GLabel } from "../../model/label.js";
import type {
    Action,
    IActionDispatcher,
    CommandExecutionContext,
    CommandReturn,
    BoundsAware,
    EditLabelValidationResult
} from "@eclipse-glsp/sprotty";

const { injectable, inject } = sharedImport("inversify");
const { Command, TYPES, findParentByFeature, isSizeable } = sharedImport("@eclipse-glsp/sprotty");
const { LocalRequestBoundsAction } = sharedImport("@eclipse-glsp/client");

/**
 * Action to update the edit mode and temporary text of a label.
 */
export interface UpdateLabelEditAction extends Action {
    kind: "updateLabelEdit";
    /**
     * ID of the label to update
     */
    labelId: string;
    /**
     * Temporary text to set while editing, only relevant in edit mode
     */
    tempText?: string;
    /**
     * Whether the label is in edit mode
     */
    editMode: boolean;
    /**
     * Validation result for the current edit, only relevant in edit mode
     */
    validationResult?: EditLabelValidationResult;
}

export namespace UpdateLabelEditAction {
    export const KIND = "updateLabelEdit";

    export function create(
        labelId: string,
        editMode: boolean,
        tempText?: string,
        validationResult?: EditLabelValidationResult
    ): UpdateLabelEditAction {
        return {
            kind: KIND,
            labelId,
            editMode,
            tempText,
            validationResult
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
        const label = context.root.index.getById(this.action.labelId) as GLabel | undefined;
        if (label == undefined) {
            return LocalRequestBoundsAction.fromCommand(context, this.actionDispatcher, this.action);
        }
        label.editMode = this.action.editMode;
        if (this.action.tempText != undefined) {
            label.tempText = this.action.tempText;
        } else if (!this.action.editMode) {
            label.tempText = undefined;
        }
        if (this.action.validationResult !== undefined) {
            label.validationResult = this.action.validationResult;
        } else if (!this.action.editMode) {
            label.validationResult = undefined;
        }
        const resized = findParentByFeature<BoundsAware>(label, isSizeable);
        return LocalRequestBoundsAction.fromCommand(
            context,
            this.actionDispatcher,
            this.action,
            resized != undefined ? [resized.id] : undefined
        );
    }

    override undo(): CommandReturn {
        throw new Error("Method not implemented.");
    }

    override redo(): CommandReturn {
        throw new Error("Method not implemented.");
    }
}

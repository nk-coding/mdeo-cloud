import type {
    Action,
    EditableLabel,
    GModelElement,
    KeyListener as KeyListenerType,
    MouseListener as MouseListenerType
} from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../sharedImport.js";
import { getEditableLabel } from "./editableLabel.js";
import { UpdateLabelEditAction } from "./updateLabelEditAction.js";

const { injectable } = sharedImport("inversify");
const { DirectLabelEditTool } = sharedImport("@eclipse-glsp/client");
const { MouseListener, KeyListener, matchesKeystroke, isSelectable, toArray } = sharedImport("@eclipse-glsp/sprotty");

@injectable()
export class EditLabelTool extends DirectLabelEditTool {
    protected override createEditLabelKeyListener(): KeyListenerType {
        return new EditLabelKeyListener();
    }

    protected override createEditLabelMouseListener(): MouseListenerType {
        return new EditLabelMouseListener();
    }
}

export class EditLabelMouseListener extends MouseListener {
    override doubleClick(target: GModelElement): (Action | Promise<Action>)[] {
        const editableLabel = getEditableLabel(target);
        if (editableLabel) {
            return [UpdateLabelEditAction.create(editableLabel.id, true)];
        }
        return [];
    }
}

export class EditLabelKeyListener extends KeyListener {
    override keyDown(element: GModelElement, event: KeyboardEvent): Action[] {
        if (matchesKeystroke(event, "F2")) {
            const editableLabels = toArray(element.index.all().filter((e) => isSelectable(e) && e.selected))
                .map(getEditableLabel)
                .filter((e): e is EditableLabel & GModelElement => e !== undefined);
            if (editableLabels.length === 1) {
                return [UpdateLabelEditAction.create(editableLabels[0].id, true)];
            }
        }
        return [];
    }
}

import type { CommandExecutionContext, CommandReturn, IActionDispatcher } from "@eclipse-glsp/sprotty";
import { InsertNewLabelAction, RemoveNewLabelAction } from "@mdeo/protocol-common";
import { sharedImport } from "../../sharedImport.js";
import type { GLabel } from "../../model/label.js";

const { injectable, inject } = sharedImport("inversify");
const { TYPES, GParentElement, GChildElement } = sharedImport("@eclipse-glsp/sprotty");
const { FeedbackCommand, LocalRequestBoundsAction } = sharedImport("@eclipse-glsp/client");

/**
 * Feedback command that inserts pre-built GModel element templates into the diagram
 * for inline label editing.
 *
 * The server is fully responsible for building the template structure (including
 * compartments, dividers, and wrapper nodes).  This command simply:
 * 1. Inserts all {@link InsertNewLabelAction.templates} at
 *    {@link InsertNewLabelAction.insertIndex} inside the element identified by
 *    {@link InsertNewLabelAction.parentElementId}.
 * 2. Locates the label element by {@link InsertNewLabelAction.labelId} and stores
 *    the tracking fields {@code insertedElementIds} and {@code containerElementId}
 *    on it so the label view can later build a {@link RemoveNewLabelAction}.
 * 3. Requests a bounds recalculation on the enclosing top-level node.
 *
 * Templates must include a label element with {@code editMode: true} and
 * {@code isNewLabel: true} so that the label view auto-focuses for input.
 */
@injectable()
export class InsertNewLabelCommand extends FeedbackCommand {
    static readonly KIND = InsertNewLabelAction.KIND;

    /**
     * Action dispatcher used to trigger a bounds recalculation after insertion.
     */
    @inject(TYPES.IActionDispatcher)
    protected actionDispatcher!: IActionDispatcher;

    constructor(@inject(TYPES.Action) protected action: InsertNewLabelAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const parent = context.root.index.getById(this.action.parentElementId);
        if (parent == undefined || !(parent instanceof GParentElement)) {
            return context.root;
        }

        let offset = 0;
        for (const templateSchema of this.action.templates) {
            const element = context.modelFactory.createElement(templateSchema);
            parent.add(element, this.action.insertIndex + offset);
            offset++;
        }

        const label = context.root.index.getById(this.action.labelId) as GLabel | undefined;
        if (label != undefined) {
            label.insertedElementIds = this.action.templates.map((t) => t.id);
            label.containerElementId = this.action.parentElementId;
        }

        return LocalRequestBoundsAction.fromCommand(context, this.actionDispatcher, this.action);
    }
}

/**
 * Feedback command that removes previously inserted template elements from the GModel.
 *
 * All element IDs listed in {@link RemoveNewLabelAction.insertedElementIds} are
 * removed from the element identified by {@link RemoveNewLabelAction.containerElementId}.
 * A bounds recalculation is then requested on the enclosing top-level node.
 */
@injectable()
export class RemoveNewLabelCommand extends FeedbackCommand {
    static readonly KIND = RemoveNewLabelAction.KIND;

    /**
     * Action dispatcher used to trigger a bounds recalculation after removal.
     */
    @inject(TYPES.IActionDispatcher)
    protected actionDispatcher!: IActionDispatcher;

    constructor(@inject(TYPES.Action) protected action: RemoveNewLabelAction) {
        super();
    }

    execute(context: CommandExecutionContext): CommandReturn {
        const parent = context.root.index.getById(this.action.containerElementId);
        if (parent == undefined || !(parent instanceof GParentElement)) {
            return context.root;
        }

        for (const elementId of this.action.insertedElementIds) {
            const element = context.root.index.getById(elementId);
            if (element != undefined && element instanceof GChildElement && element.parent === parent) {
                parent.remove(element);
            }
        }

        return LocalRequestBoundsAction.fromCommand(context, this.actionDispatcher, this.action);
    }
}

import { GLabelView, sharedImport } from "@mdeo/editor-shared";
import type { GLabel } from "@mdeo/editor-shared";
import type { Operation } from "@eclipse-glsp/protocol";
import { AddHeaderDeclarationOperation } from "@mdeo/protocol-model-transformation";
import type { GHeaderRowLabel } from "../model/headerNode.js";

const { injectable } = sharedImport("inversify");

/**
 * View for the title of the header box: the transformation's file name, bold and centred.
 */
@injectable()
export class GHeaderTitleLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        return {
            ...super.getClasses(model),
            "text-sm": true,
            "font-semibold": true,
            "text-center": true,
            block: true
        };
    }
}

/**
 * View for a row of the header box: an import or a declaration in the font of the source, with
 * a highlight while selected. A new row (added from the toolbox) commits through
 * {@link AddHeaderDeclarationOperation}; its name is preselected for typing over it.
 */
@injectable()
export class GHeaderRowLabelView extends GLabelView {
    protected override getClasses(model: Readonly<GLabel>): Record<string, boolean> {
        const selected = (model as GHeaderRowLabel).selected === true;
        return {
            ...super.getClasses(model),
            "text-sm": true,
            "font-mono": true,
            "whitespace-pre": true,
            block: true,
            "rounded-sm": true,
            "px-1": true,
            "bg-accent": selected,
            "text-accent-foreground": selected
        };
    }

    protected override createNewLabelOperation(model: Readonly<GLabel>, editText: string): Operation {
        if (model.newLabelOperationKind !== AddHeaderDeclarationOperation.NEW_LABEL_KIND) {
            return super.createNewLabelOperation(model, editText);
        }
        return AddHeaderDeclarationOperation.create({
            headerNodeId: model.parentElementId!,
            labelText: editText
        });
    }

    /**
     * Focuses the edit control; in a declaration, selects its name (`input1` of
     * `in input1: int`), which is what a new row usually needs changed, and otherwise
     * everything.
     *
     * @param element The edit control
     */
    protected override focusAndSelect(element: HTMLTextAreaElement): void {
        setTimeout(() => {
            element.focus();
            const name = /^(\s*(?:in|out)\s+)([^\s:]+)/.exec(element.value);
            if (name != undefined) {
                element.setSelectionRange(name[1].length, name[1].length + name[2].length);
            } else {
                element.select();
            }
        });
    }
}

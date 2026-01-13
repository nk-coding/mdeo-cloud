import type { IView, IActionDispatcher, RenderingContext } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { Attrs, VNode, VNodeData } from "snabbdom";
import type { GLabel } from "../model/label.js";
import { UpdateLabelEditAction } from "../features/edit-label/updateLabelEditAction.js";

const { injectable, inject } = sharedImport("inversify");
const { html, TYPES, ApplyLabelEditOperation } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering GLabel elements with inline editing support.
 * Uses a textarea with field-sizing-content for automatic sizing.
 */
@injectable()
export class GLabelView implements IView {
    @inject(TYPES.IActionDispatcher) protected actionDispatcher!: IActionDispatcher;

    render(model: Readonly<GLabel>, context: RenderingContext): VNode | undefined {
        if (model.editMode) {
            return this.renderEditControl(model, context);
        }

        return html(
            "span",
            {
                class: {
                    ...this.getClasses(model)
                }
            },
            model.text
        );
    }

    /**
     * Renders the edit control for a label in edit mode.
     * Always uses a textarea element with field-sizing-content for automatic sizing.
     *
     * @param model The label model to render
     * @param context The rendering context
     * @returns The VNode for the edit control
     */
    protected renderEditControl(model: Readonly<GLabel>, context: RenderingContext): VNode {
        const currentText = model.tempText ?? model.text;

        const data: VNodeData = {};
        const attrs: Attrs = {};

        if (context.targetKind === "hidden") {
            data.hook = {
                insert: (vnode: VNode) => {
                    (vnode.elm as HTMLTextAreaElement).value = currentText;
                },
                update: (oldVnode: VNode, vnode: VNode) => {
                    (vnode.elm as HTMLTextAreaElement).value = currentText;
                }
            };
        } else {
            data.hook = {
                insert: (vnode: VNode) => {
                    this.focusAndSelect(vnode.elm as HTMLTextAreaElement);
                    (vnode.elm as HTMLTextAreaElement).value = currentText;
                }
            };
            data.on = {
                keydown: (event: KeyboardEvent) => this.handleKeyDown(event, model),
                blur: () => this.handleBlur(model),
                input: (event: Event) => this.updateTempText(event, model),
                click: (event: Event) => event.stopPropagation(),
                mousedown: (event: Event) => event.stopPropagation(),
                mouseup: (event: Event) => event.stopPropagation(),
                keyup: (event: Event) => event.stopPropagation(),
                keypress: (event: Event) => event.stopPropagation()
            };
        }

        const textarea = html("textarea", {
            class: {
                ...this.getEditControlClasses(),
                ...this.getClasses(model)
            },
            attrs,
            ...data
        });
        return html("form", null, textarea);
    }

    /**
     * Returns the CSS classes for the edit control.
     *
     * @returns Record of CSS class names and their enabled state
     */
    protected getEditControlClasses(): Record<string, boolean> {
        return {
            "border-input": true,
            "placeholder:text-muted-foreground": true,
            "focus-visible:border-ring": true,
            "focus-visible:ring-ring/50": true,
            "dark:bg-input/30": true,
            "w-full": true,
            "min-w-0": true,
            "rounded-md": true,
            border: true,
            "bg-transparent": true,
            "text-base": true,
            "shadow-xs": true,
            "transition-[color,box-shadow]": true,
            "outline-none": true,
            "focus-visible:ring-[3px]": true,
            "disabled:cursor-not-allowed": true,
            "disabled:opacity-50": true,
            "md:text-sm": true,
            "field-sizing-content": true,
            "px-3": true,
            "py-1": true,
            "resize-none": true,
            block: true
        };
    }

    /**
     * Handles keyboard events during label editing.
     * Enter applies the edit, Escape cancels it.
     *
     * @param event The keyboard event
     * @param model The label model being edited
     */
    protected handleKeyDown(event: KeyboardEvent, model: Readonly<GLabel>): void {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            this.applyLabelEdit(model);
        } else if (event.key === "Escape") {
            event.preventDefault();
            this.cancelEdit(model);
        }
        event.stopPropagation();
    }

    /**
     * Handles blur events by applying the current label edit.
     *
     * @param model The label model being edited
     */
    protected handleBlur(model: Readonly<GLabel>): void {
        this.applyLabelEdit(model);
    }

    /**
     * Updates the temporary text value as the user types.
     *
     * @param event The input event
     * @param model The label model being edited
     */
    protected updateTempText(event: Event, model: Readonly<GLabel>): void {
        const target = event.target as HTMLInputElement | HTMLTextAreaElement;
        this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, true, target.value));
    }

    /**
     * Applies the label edit by dispatching an operation if the text changed,
     * or exits edit mode if the text is unchanged.
     *
     * @param model The label model being edited
     */
    protected applyLabelEdit(model: Readonly<GLabel>): void {
        const tempText = model.tempText ?? model.text;

        if (tempText !== model.text) {
            this.actionDispatcher.dispatch(ApplyLabelEditOperation.create({ labelId: model.id, text: tempText }));
        } else {
            this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, false));
        }
    }

    /**
     * Cancels the label edit and exits edit mode without saving changes.
     *
     * @param model The label model being edited
     */
    protected cancelEdit(model: Readonly<GLabel>): void {
        this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, false));
    }

    /**
     * Focuses the edit control and sets the selection.
     * For textarea elements, places cursor at the start.
     * For input elements, selects all text.
     *
     * @param element The input or textarea element to focus
     */
    protected focusAndSelect(element: HTMLTextAreaElement): void {
        setTimeout(() => {
            element.focus();
            element.select();
        });
    }

    /**
     * Returns additional CSS classes for the label element.
     * Can be overridden by subclasses to add custom styling.
     *
     * @param _model The label model
     * @returns Record of CSS class names and their enabled state
     */
    protected getClasses(_model: Readonly<GLabel>): Record<string, boolean> {
        return {};
    }
}

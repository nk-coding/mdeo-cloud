import type {
    IView,
    IActionDispatcher,
    RenderingContext,
    IEditLabelValidator,
    EditLabelValidationResult
} from "@eclipse-glsp/sprotty";
import { sharedImport } from "../sharedImport.js";
import type { Attrs, VNode, VNodeData } from "snabbdom";
import type { GLabel } from "../model/label.js";
import { UpdateLabelEditAction } from "../features/edit-label/updateLabelEditAction.js";
import { RemoveNewLabelAction } from "@mdeo/protocol-common";
import type { Operation } from "@eclipse-glsp/protocol";

const { injectable, inject } = sharedImport("inversify");
const { html, TYPES, ApplyLabelEditOperation, matchesKeystroke } = sharedImport("@eclipse-glsp/sprotty");

/**
 * View for rendering GLabel elements with inline editing support.
 * Uses a textarea with field-sizing-content for automatic sizing.
 * Supports validation via IEditLabelValidator and IEditLabelValidationDecorator.
 */
@injectable()
export abstract class GLabelView implements IView {
    /**
     * Injected action dispatcher for dispatching label update actions.
     */
    @inject(TYPES.IActionDispatcher) protected actionDispatcher!: IActionDispatcher;

    /**
     * Injected label validator for validating label edits.
     */
    @inject(TYPES.IEditLabelValidator) protected labelValidator!: IEditLabelValidator;

    /**
     * Timeout ID for debounced validation calls.
     */
    protected validationTimeout: number | undefined = undefined;

    /**
     * Renders the label element, switching between display and edit mode.
     *
     * @param model The label model to render
     * @param context The rendering context
     * @returns The rendered VNode or undefined
     */
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
                blur: () => this.applyLabelEdit(model),
                input: (event: Event) => this.handleInput(event, model),
                click: (event: Event) => event.stopPropagation(),
                mousedown: (event: Event) => {
                    const target = event.target as HTMLTextAreaElement;
                    if (target.selectionStart !== target.selectionEnd) {
                        target.selectionEnd = target.selectionStart;
                    }
                    event.stopPropagation();
                },
                mouseup: (event: Event) => event.stopPropagation(),
                keyup: (event: Event) => event.stopPropagation(),
                keypress: (event: Event) => event.stopPropagation()
            };
        }

        const hasError = model.validationResult && model.validationResult.severity === "error";
        if (hasError) {
            attrs["aria-invalid"] = "true";
        }

        const textarea = html("textarea", {
            class: {
                ...this.getEditControlClasses(model),
                ...this.getClasses(model)
            },
            attrs,
            ...data
        });

        return html(
            "div",
            { class: { "label-edit-container": true } },
            textarea,
            this.renderErrorMessage(model.validationResult!)
        );
    }

    /**
     * Renders an error message for validation failures.
     * Shows a single line with ellipsis by default, expands on hover to show full message.
     *
     * @param validationResult The validation result to render
     * @returns The VNode for the error message, or undefined if no error
     */
    protected renderErrorMessage(validationResult: EditLabelValidationResult | undefined): VNode | undefined {
        if (!validationResult?.message) {
            return undefined;
        }

        const containerClasses: Record<string, boolean> = {
            relative: true,
            "mt-1.5": true,
            group: true
        };

        const collapsedClasses: Record<string, boolean> = {
            "text-destructive": true,
            "text-sm": true,
            "font-normal": true,
            truncate: true,
            "overflow-hidden": true,
            "whitespace-nowrap": true,
            "group-hover:invisible": true
        };

        const expandedClasses: Record<string, boolean> = {
            invisible: true,
            "group-hover:visible": true,
            absolute: true,
            "top-0": true,
            "left-0": true,
            "right-0": true,
            "text-destructive": true,
            "text-sm": true,
            "font-normal": true,
            "bg-popover": true,
            "text-popover-foreground": true,
            "rounded-md": true,
            border: true,
            "border-destructive": true,
            "p-2": true,
            "shadow-md": true,
            "z-50": true,
            "whitespace-normal": true,
            "break-words": true
        };

        const collapsed = html(
            "div",
            {
                class: collapsedClasses
            },
            validationResult.message
        );

        const expanded = html(
            "div",
            {
                class: expandedClasses
            },
            validationResult.message
        );

        return html(
            "div",
            {
                class: containerClasses,
                attrs: {
                    role: "alert"
                }
            },
            collapsed,
            expanded
        );
    }

    /**
     * Handles keyboard events during label editing.
     * Enter applies the edit, Escape cancels it.
     *
     * @param event The keyboard event
     * @param model The label model being edited
     */
    protected handleKeyDown(event: KeyboardEvent, model: Readonly<GLabel>): void {
        if (matchesKeystroke(event, "Enter") && !event.shiftKey) {
            event.preventDefault();
            this.applyLabelEdit(model);
        } else if (matchesKeystroke(event, "Escape")) {
            event.preventDefault();
            this.cancelEdit(model);
        }
        event.stopPropagation();
    }

    /**
     * Handles input events and updates the temporary text value.
     *
     * @param event The input event
     * @param model The label model being edited
     */
    protected handleInput(event: Event, model: Readonly<GLabel>): void {
        const target = event.target as HTMLTextAreaElement;
        const newText = target.value;

        this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, true, newText));
        this.performLabelValidation(model, newText);
    }

    /**
     * Performs label validation with a debounce.
     *
     * @param model The label model being edited
     * @param value The value to validate
     */
    protected performLabelValidation(model: Readonly<GLabel>, value: string): void {
        if (this.validationTimeout) {
            window.clearTimeout(this.validationTimeout);
        }
        this.validationTimeout = window.setTimeout(() => this.validateLabel(model, value), 200);
    }

    /**
     * Validates a label value and updates the model with the result.
     *
     * @param model The label model being edited
     * @param value The value to validate
     * @returns The validation result
     */
    protected async validateLabel(model: Readonly<GLabel>, value: string): Promise<EditLabelValidationResult> {
        const result = await this.labelValidator.validate(value, model);

        this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, true, value, result));

        return result;
    }

    /**
     * Applies the label edit by dispatching an operation if the text changed,
     * or exits edit mode if the text is unchanged.
     *
     * For new labels (isNewLabel = true), dispatches a custom operation from the registry
     * instead of the standard ApplyLabelEditOperation. For existing labels, follows the
     * standard edit flow. In new-label mode, an empty text value is treated as a cancel.
     *
     * @param model The label model being edited
     */
    protected async applyLabelEdit(model: Readonly<GLabel>): Promise<void> {
        const tempText = model.tempText ?? model.text;

        if (model.isNewLabel && tempText.trim() === "") {
            this.cancelEdit(model);
            return;
        }

        if (tempText === model.text && !model.isNewLabel) {
            this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, false));
            return;
        }

        const isValid = (await this.validateLabel(model, tempText)).severity !== "error";
        if (!isValid) {
            this.cancelEdit(model);
            return;
        }

        if (model.isNewLabel) {
            const operation = this.createNewLabelOperation(model, tempText);
            if (operation) {
                this.actionDispatcher.dispatchAll([
                    RemoveNewLabelAction.create({
                        insertedElementIds: model.insertedElementIds ?? [],
                        containerElementId: model.containerElementId ?? ""
                    }),
                    operation
                ]);
            } else {
                this.cancelEdit(model);
            }
        } else {
            this.actionDispatcher.dispatch(ApplyLabelEditOperation.create({ labelId: model.id, text: tempText }));
        }
    }

    /**
     * Creates the commit operation for a newly created label.
     *
     * Subclasses can use {@code newLabelOperationKind} and {@code parentElementId}
     * to dispatch label-specific operations.
     *
     * @param _model The label model being edited
     * @param _editText The text entered by the user
     * @returns The created operation, or void if no operation should be dispatched
     */
    protected createNewLabelOperation(_model: Readonly<GLabel>, _editText: string): Operation {
        throw new Error("createNewLabelOperation must be implemented by subclasses to handle new label commits");
    }

    /**
     * Cancels the label edit and exits edit mode without saving changes.
     * For new labels, dispatches a {@link RemoveNewLabelAction} to remove the temporary element.
     *
     * @param model The label model being edited
     */
    protected cancelEdit(model: Readonly<GLabel>): void {
        if (model.isNewLabel) {
            this.actionDispatcher.dispatch(
                RemoveNewLabelAction.create({
                    insertedElementIds: model.insertedElementIds ?? [],
                    containerElementId: model.containerElementId ?? ""
                })
            );
        } else {
            this.actionDispatcher.dispatch(UpdateLabelEditAction.create(model.id, false));
        }
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

    /**
     * Returns the CSS classes for the edit control.
     *
     * @param model The label model being edited
     * @returns Record of CSS class names and their enabled state
     */
    protected getEditControlClasses(model: Readonly<GLabel>): Record<string, boolean> {
        const hasError = !!(model.validationResult && model.validationResult.severity === "error");

        return {
            "border-input": !hasError,
            "border-destructive": hasError,
            "placeholder:text-muted-foreground": true,
            "focus-visible:border-ring": !hasError,
            "focus-visible:border-destructive": hasError,
            "focus-visible:ring-ring/50": !hasError,
            "focus-visible:ring-destructive/30": hasError,
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
}

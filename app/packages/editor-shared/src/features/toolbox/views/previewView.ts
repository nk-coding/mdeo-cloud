import type { VNode } from "snabbdom";
import type { Action, GhostElement, TriggerNodeCreationAction } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";
import type { ToolboxEditEntry } from "../toolboxTypes.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");
const { TriggerNodeCreationAction: TriggerNodeCreationActionNS } = sharedImport("@eclipse-glsp/protocol");

/**
 * Extracts the ghost element from a palette item's trigger action if available.
 *
 * @param item The toolbox edit entry
 * @returns The ghost element if present, undefined otherwise
 */
function extractGhostElement(item: ToolboxEditEntry): GhostElement | undefined {
    const actions = item.paletteItem.actions;
    if (!actions || actions.length === 0) {
        return undefined;
    }

    const triggerAction = actions.find((action: Action) => TriggerNodeCreationActionNS.is(action)) as
        | TriggerNodeCreationAction
        | undefined;

    return triggerAction?.ghostElement;
}

/**
 * Position the preview `element` vertically aligned to the corresponding
 * toolbox item inside the nearest `.toolbox-details` container.
 *
 * @param element The preview container element created by snabbdom.
 * @param itemId The `data-item-id` of the toolbox item to align to.
 */
function positionPreviewRelativeToToolbox(element: HTMLElement, itemId: string): void {
    const toolboxDetails = element.closest(".toolbox");
    if (toolboxDetails == null) {
        return;
    }

    const itemButton = toolboxDetails.querySelector(`[data-item-id="${itemId}"]`) as HTMLElement | null;
    if (itemButton == null) {
        return;
    }

    const offset = itemButton.getBoundingClientRect().top - toolboxDetails.getBoundingClientRect().top;
    element.style.top = `${offset}px`;
}

/**
 * Generates the preview view for a palette item.
 * Shows a ghost element preview when available, otherwise falls back to text.
 * Uses a hook to calculate Y position relative to the toolbox.
 *
 * @param context The toolbox context
 * @param item The item to show preview for
 * @returns The preview VNode or undefined if not showing
 */
export function generatePreviewView(context: Toolbox, item: ToolboxEditEntry): VNode | undefined {
    if (context.showPreviewFor !== item.id) {
        return undefined;
    }

    const ghostElement = extractGhostElement(item);
    if (ghostElement == undefined) {
        return undefined;
    }

    return html(
        "div",
        {
            class: {
                "toolbox-preview": true,
                absolute: true,
                "right-[calc(100%+var(--editor-spacing)*2)]": true,
                "z-50": true,
                "rounded-md": true,
                border: true,
                "border-border": true,
                "bg-popover": true,
                "p-1": true,
                "shadow-md": true,
                "w-50": true
            },
            attrs: {
                role: "tooltip",
                "aria-live": "polite",
                "data-preview-for": item.id
            },
            hook: {
                insert: (vnode: any) => {
                    const element = vnode.elm as HTMLElement;
                    positionPreviewRelativeToToolbox(element, item.id);
                },
                update: (oldVnode: any, vnode: any) => {
                    const element = vnode.elm as HTMLElement;
                    positionPreviewRelativeToToolbox(element, item.id);
                }
            }
        },
        html(
            "div",
            {
                class: {
                    "bg-background": true,
                    "p-3": true,
                    "rounded-md": true
                }
            },
            context.previewRenderer.renderPreview(ghostElement)
        )
    );
}

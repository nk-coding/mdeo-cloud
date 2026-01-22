import type { VNode } from "snabbdom";
import type { Action, GhostElement, TriggerNodeCreationAction, GModelElementSchema } from "@eclipse-glsp/sprotty";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";
import type { ToolboxEditEntry } from "../toolboxTypes.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");
const { TriggerNodeCreationAction: TriggerNodeCreationActionNS } = sharedImport("@eclipse-glsp/protocol");

/**
 * Checks if an element template is a schema object.
 *
 * @param template The element template to check
 * @returns True if template is a GModelElementSchema, false if it's a string
 */
function isElementSchema(template: string | GModelElementSchema): template is GModelElementSchema {
    return typeof template !== "string";
}

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
 * Generates an SVG preview from a ghost element template.
 *
 * @param ghostElement The ghost element containing the template
 * @param itemName The name of the item for display
 * @returns The SVG VNode representing the ghost element
 */
function generateGhostElementPreview(ghostElement: GhostElement, itemName: string): VNode {
    const template = ghostElement.template;
    const width = 100;
    const height = 60;
    const displayId = isElementSchema(template) ? template.id : template;

    return html(
        "svg",
        {
            attrs: {
                width: Math.min(width, 120),
                height: Math.min(height, 80),
                viewBox: `0 0 ${width} ${height}`,
                "aria-hidden": "true"
            },
            class: {
                "preview-ghost": true
            }
        },
        html("rect", {
            attrs: {
                x: 1,
                y: 1,
                width: width - 2,
                height: height - 2,
                rx: 4,
                ry: 4,
                fill: "hsl(var(--muted))",
                stroke: "hsl(var(--border))",
                "stroke-width": 1,
                "stroke-dasharray": "4,2"
            }
        }),
        html(
            "text",
            {
                attrs: {
                    x: width / 2,
                    y: height / 2,
                    "text-anchor": "middle",
                    "dominant-baseline": "middle",
                    fill: "hsl(var(--muted-foreground))",
                    "font-size": 10
                }
            },
            displayId ?? itemName
        )
    );
}

/**
 * Generates a text-only preview when no ghost element is available.
 *
 * @param item The toolbox edit entry
 * @returns The text preview VNode
 */
function generateTextPreview(item: ToolboxEditEntry): VNode {
    return html(
        "div",
        {
            class: {
                "preview-text": true,
                "text-sm": true,
                "text-muted-foreground": true
            }
        },
        `Preview: ${item.name}`
    );
}

/**
 * Generates the preview view for a palette item.
 * Shows a ghost element preview when available, otherwise falls back to text.
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

    return html(
        "div",
        {
            class: {
                "toolbox-preview": true,
                absolute: true,
                "left-full": true,
                "top-0": true,
                "ml-2": true,
                "z-50": true,
                "rounded-md": true,
                border: true,
                "border-border": true,
                "bg-popover": true,
                "p-2": true,
                "shadow-md": true,
                "min-w-32": true
            },
            attrs: {
                role: "tooltip",
                "aria-live": "polite"
            }
        },
        html(
            "div",
            {
                class: {
                    "preview-content": true
                }
            },
            ghostElement ? generateGhostElementPreview(ghostElement, item.name) : generateTextPreview(item)
        )
    );
}

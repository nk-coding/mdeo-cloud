import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";
import type { ToolboxEditEntry } from "../toolboxTypes.js";
import { listItemClasses } from "./styles.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");

/**
 * Generates a single palette item view with optional preview.
 *
 * @param context The toolbox context
 * @param item The toolbox edit entry to render
 * @param index The index of the item in the list
 * @param isSelected Whether this item is currently selected
 * @returns The palette item VNode wrapped in a container
 */
export function generateToolboxItemView(
    context: Toolbox,
    item: ToolboxEditEntry,
    index: number = 0,
    isSelected: boolean = false
): VNode {
    return html(
        "button",
        {
            class: {
                ...listItemClasses,
                "toolbox-item": true,
                "bg-accent": isSelected
            },
            attrs: {
                "aria-label": `Create ${item.name}`,
                role: "option",
                "aria-selected": isSelected ? "true" : "false",
                tabindex: isSelected ? "0" : "-1",
                "data-item-index": index,
                "data-item-id": item.id
            },
            on: {
                click: (event: Event) => {
                    event.preventDefault();
                    context.showPreviewFor = undefined;
                    context.onPaletteItemClick(item);
                },
                mouseenter: () => {
                    context.showPreviewFor = item.id;
                    context.update();
                },
                mouseleave: () => {
                    context.showPreviewFor = undefined;
                    context.update();
                },
                mousedown: (event: Event) => {
                    event.preventDefault();
                }
            }
        },
        html(
            "span",
            {
                class: {
                    truncate: true
                }
            },
            item.name
        )
    );
}

/**
 * Generates a group of palette items with a header.
 *
 * @param context The toolbox context
 * @param groupName The name of the group
 * @param items The items in the group
 * @param startIndex The starting index for items in this group
 * @returns The group VNode
 */
export function generateToolboxItemGroupView(
    context: Toolbox,
    groupName: string,
    items: ToolboxEditEntry[],
    startIndex: number = 0
): VNode {
    return html(
        "div",
        {
            class: {
                "toolbox-group": true,
                "mb-2": true
            }
        },
        html(
            "div",
            {
                class: {
                    "toolbox-group-header": true,
                    "px-2": true,
                    "py-1": true,
                    "text-xs": true,
                    "font-medium": true,
                    "text-muted-foreground": true,
                    uppercase: true,
                    "tracking-wider": true
                }
            },
            groupName
        ),
        html(
            "div",
            {
                class: {
                    "toolbox-group-items": true
                },
                attrs: {
                    role: "listbox",
                    "aria-label": groupName
                },
                on: {
                    keydown: (event: KeyboardEvent) => {
                        handlePaletteKeyDown(context, event, items, startIndex);
                    }
                }
            },
            ...items.map((item, idx) =>
                generateToolboxItemView(context, item, startIndex + idx, context.selectedItemIndex === startIndex + idx)
            )
        )
    );
}

/**
 * Handles keyboard navigation within the palette items list.
 *
 * @param context The toolbox context
 * @param event The keyboard event
 * @param items The list of items in the current group
 * @param startIndex The starting index for items in this group
 */
function handlePaletteKeyDown(
    context: Toolbox,
    event: KeyboardEvent,
    items: ToolboxEditEntry[],
    startIndex: number
): void {
    const totalItems = items.length;
    if (totalItems === 0) {
        return;
    }

    const currentLocal = context.selectedItemIndex - startIndex;

    switch (event.key) {
        case "ArrowDown":
            event.preventDefault();
            if (currentLocal < totalItems - 1) {
                context.selectedItemIndex = startIndex + currentLocal + 1;
            } else {
                context.selectedItemIndex = startIndex;
            }
            context.update();
            focusSelectedItem(context);
            break;

        case "ArrowUp":
            event.preventDefault();
            if (currentLocal > 0) {
                context.selectedItemIndex = startIndex + currentLocal - 1;
            } else {
                context.selectedItemIndex = startIndex + totalItems - 1;
            }
            context.update();
            focusSelectedItem(context);
            break;

        case "Enter":
        case " ":
            event.preventDefault();
            if (currentLocal >= 0 && currentLocal < totalItems) {
                context.showPreviewFor = undefined;
                context.onPaletteItemClick(items[currentLocal]);
            }
            break;

        case "Home":
            event.preventDefault();
            context.selectedItemIndex = startIndex;
            context.update();
            focusSelectedItem(context);
            break;

        case "End":
            event.preventDefault();
            context.selectedItemIndex = startIndex + totalItems - 1;
            context.update();
            focusSelectedItem(context);
            break;
    }
}

/**
 * Focuses the currently selected palette item.
 *
 * @param context The toolbox context
 */
function focusSelectedItem(context: Toolbox): void {
    requestAnimationFrame(() => {
        const element = document.querySelector(
            `[data-item-index="${context.selectedItemIndex}"]`
        ) as HTMLElement | null;
        element?.focus();
    });
}

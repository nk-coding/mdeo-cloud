import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";
import type { ToolboxEditEntry } from "../toolboxTypes.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");

import { generateSearchInputView } from "./searchInputView.js";
import { generateToolboxItemView, generateToolboxItemGroupView } from "./toolboxItemView.js";

/**
 * Generates the details panel view containing search and palette items.
 *
 * @param context The toolbox context
 * @returns The details panel VNode, always rendered with transition classes
 */
export function generateDetailsPanelView(context: Toolbox): VNode {
    const isVisible = context.isOpen && context.isBottomPanelOpen;
    return html(
        "div",
        {
            class: {
                flex: true,
                "flex-col": true,
                "w-68": true,
                "overflow-hidden": true,
                "shadow-[0_8px_16px_rgba(0,0,0,0.12)]": true,
                "rounded-md": true,
                "max-h-0": !isVisible,
                "opacity-0": !isVisible,
                "max-h-[600px]": isVisible,
                "opacity-100": isVisible,
                "transition-all": isVisible,
                "duration-300": isVisible
            }
        },
        html(
            "div",
            {
                class: {
                    "rounded-md": true,
                    border: true,
                    "border-border": true,
                    "bg-toolbox": true
                }
            },
            html(
                "div",
                {
                    class: {
                        "toolbox-search-container": true,
                        "px-3": true,
                        "py-2": true,
                        "border-b": true,
                        "border-border": true
                    }
                },
                generateSearchInputView(context)
            ),
            html(
                "div",
                {
                    class: {
                        "toolbox-items-container": true,
                        "flex-1": true,
                        "overflow-y-auto": true,
                        "px-1": true,
                        "py-2": true
                    }
                },
                ...generatePaletteItemsList(context)
            )
        )
    );
}

/**
 * Generates the palette items list, either grouped or filtered.
 *
 * @param context The toolbox context
 * @returns Array of VNodes for palette items
 */
function generatePaletteItemsList(context: Toolbox): VNode[] {
    const items = context.getFilteredItems();

    if (items.length === 0) {
        return [generateEmptyState(context)];
    }

    if (context.searchString.length > 0) {
        return items.map((item, index) =>
            generateToolboxItemView(context, item, index, context.selectedItemIndex === index)
        );
    }

    return generateGroupedItems(context, items);
}

/**
 * Generates grouped palette items.
 *
 * @param context The toolbox context
 * @param items The items to group
 * @returns Array of grouped VNodes
 */
function generateGroupedItems(context: Toolbox, items: ToolboxEditEntry[]): VNode[] {
    const groups = new Map<string, ToolboxEditEntry[]>();

    for (const item of items) {
        const group = item.group || "Other";
        if (!groups.has(group)) {
            groups.set(group, []);
        }
        groups.get(group)!.push(item);
    }

    let currentIndex = 0;
    const sortedGroups = [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
    const result: VNode[] = [];

    for (const [groupName, groupItems] of sortedGroups) {
        result.push(generateToolboxItemGroupView(context, groupName, groupItems, currentIndex));
        currentIndex += groupItems.length;
    }

    return result;
}

/**
 * Generates an empty state view when no items match.
 *
 * @param context The toolbox context
 * @returns The empty state VNode
 */
function generateEmptyState(context: Toolbox): VNode {
    const message = context.searchString.length > 0 ? "No matching elements" : "No elements available";

    return html(
        "div",
        {
            class: {
                "toolbox-empty": true,
                flex: true,
                "items-center": true,
                "justify-center": true,
                "py-8": true,
                "text-sm": true,
                "text-muted-foreground": true
            }
        },
        message
    );
}

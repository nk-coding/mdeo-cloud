import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";
import type { ToolboxEditEntry, ToolboxGroupKey } from "../toolboxTypes.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");

import { generateSearchInputView } from "./searchInputView.js";
import { generateToolboxItemView, generateToolboxItemGroupView } from "./toolboxItemView.js";
import { generateScrollView } from "./scrollView.js";
import { generatePreviewView } from "./previewView.js";

/**
 * Generates the details panel view containing search and palette items.
 *
 * @param context The toolbox context
 * @returns The details panel VNode, always rendered with transition classes
 */
export function generateDetailsPanelView(context: Toolbox): VNode {
    const isVisible = context.isBottomPanelOpen;
    return html(
        "div",
        {
            class: {
                flex: true,
                "flex-col": true,
                "mt-2": true,
                "overflow-hidden": true,
                "shadow-[0_8px_16px_rgba(0,0,0,0.12)]": true,
                "rounded-md": true,
                "opacity-0": !isVisible,
                "opacity-100": isVisible,
                "transition-all": isVisible,
                "duration-300": isVisible,
                "pointer-events-auto": true,
            }
        },
        generateDetailsPanelContent(context),
    );
}

/**
 * Renders the inner content of the details panel: search input, palette item list,
 * and optional item preview.  Returns `undefined` when the panel is collapsed so
 * that no DOM content is created while the panel is hidden.
 *
 * @param context The toolbox context providing visibility state and item data.
 * @returns The content VNode, or `undefined` when `context.isBottomPanelOpen` is `false`.
 */
function generateDetailsPanelContent(context: Toolbox): VNode | undefined {
    if (!context.isBottomPanelOpen) {
        return undefined;
    }
    return html(
        "div",
        {
            class: {
                "toolbox-details": true,
                "rounded-md": true,
                border: true,
                "border-border": true,
                "bg-toolbox": true,
                flex: true,
                "flex-col": true,
                "h-full": true
            }
        },
        ...context.generateDetailsExtension(),
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
                    "overflow-hidden": true
                }
            },
            generateScrollView(context.detailsScrollState, () => generatePaletteItemsList(context), {
                "flex-1": true,
                "px-1": true,
                "py-2": true,
                "h-full": true
            })
        ),
        generatePreviewForCurrentItem(context)
    );
}

/**
 * Generates the preview for the currently hovered item.
 *
 * @param context The toolbox context
 * @returns The preview VNode or undefined
 */
function generatePreviewForCurrentItem(context: Toolbox): VNode | undefined {
    if (!context.showPreviewFor) {
        return undefined;
    }

    const items = context.getFilteredItems();
    const currentItem = items.find((item) => item.id === context.showPreviewFor);

    if (!currentItem) {
        return undefined;
    }

    return generatePreviewView(context, currentItem);
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
    const groups = new Map<ToolboxGroupKey, ToolboxEditEntry[]>();

    for (const item of items) {
        const group = item.group || "Other";
        if (!groups.has(group)) {
            groups.set(group, []);
        }
        groups.get(group)!.push(item);
    }

    let currentIndex = 0;
    const sortedGroups = [...groups.entries()].sort(([a], [b]) => a.sortString.localeCompare(b.sortString));
    const result: VNode[] = [];

    for (const [{ name }, groupItems] of sortedGroups) {
        result.push(generateToolboxItemGroupView(context, name, groupItems, currentIndex));
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

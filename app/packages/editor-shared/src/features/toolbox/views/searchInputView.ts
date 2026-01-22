import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";
import { inputClasses } from "./styles.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");
const { Search } = sharedImport("lucide");

import { generateIcon } from "./iconView.js";

/**
 * Debounce delay in milliseconds for search input.
 */
const SEARCH_DEBOUNCE_DELAY = 150;

/**
 * Generates the search input component for filtering palette items.
 *
 * @param context The toolbox context
 * @returns The search input VNode
 */
export function generateSearchInputView(context: Toolbox): VNode {
    return html(
        "div",
        {
            class: {
                "toolbox-search": true,
                relative: true
            }
        },
        html(
            "div",
            {
                class: {
                    absolute: true,
                    "left-2": true,
                    "top-1/2": true,
                    "-translate-y-1/2": true,
                    "text-muted-foreground": true,
                    "pointer-events-none": true
                }
            },
            generateIcon(Search, ["h-4", "w-4"])
        ),
        html("input", {
            class: {
                ...inputClasses,
                "pl-8": true,
                "pr-3": true
            },
            attrs: {
                type: "text",
                placeholder: "Search..."
            },
            props: {
                value: context.searchString
            },
            hook: {
                update: (_oldVnode: VNode, vnode: VNode) => {
                    const element = vnode.elm as HTMLInputElement;
                    if (element && element.value !== context.searchString) {
                        element.value = context.searchString;
                    }
                }
            },
            on: {
                input: (event: Event) => {
                    const target = event.target as HTMLInputElement;
                    debouncedSearchUpdate(context, target.value);
                },
                keydown: (event: KeyboardEvent) => {
                    if (event.key === "Escape") {
                        context.searchString = "";
                        context.selectedItemIndex = 0;
                        context.update();
                    }
                }
            }
        })
    );
}

/**
 * Updates the search string with debouncing to prevent excessive updates.
 *
 * @param context The toolbox context
 * @param value The new search value
 */
function debouncedSearchUpdate(context: Toolbox, value: string): void {
    if (context.searchDebounceTimeout !== undefined) {
        clearTimeout(context.searchDebounceTimeout);
    }

    context.searchDebounceTimeout = window.setTimeout(() => {
        context.searchString = value;
        context.selectedItemIndex = 0;
        context.searchDebounceTimeout = undefined;
        context.update();
    }, SEARCH_DEBOUNCE_DELAY);
}

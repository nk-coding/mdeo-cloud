import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");

import { generateToolbarView } from "./toolbarView.js";
import { generateDetailsPanelView } from "./detailsPanelView.js";
import { generateErrorView } from "./errorView.js";

/**
 * Generates the complete toolbox view.
 *
 * @param context The toolbox context
 * @returns The complete toolbox VNode
 */
export function generateToolboxView(context: Toolbox): VNode {
    return html(
        "div",
        {
            class: {
                toolbox: true,
                absolute: true,
                "top-4": true,
                "right-4": true,
                flex: true,
                "flex-col": true,
                "z-50": true,
                "pointer-events-none": true
            }
        },
        generateToolboxInternal(context),
        generateErrorView(context)
    );
}

/**
 * Generates the internal toolbox layout.
 *
 * @param context The toolbox context
 * @returns The internal toolbox VNode
 */
function generateToolboxInternal(context: Toolbox): VNode {
    const hasItems = context.hasToolboxItems();

    return html(
        "div",
        {
            class: {
                flex: true,
                "flex-col": true,
                "gap-2": true,
                "w-[calc(var(--editor-spacing)*68-2px)]": context.isOpen,
                "h-[50vh]": context.isOpen,
                "w-[calc(var(--editor-spacing)*10+2px)]": !context.isOpen,
                "h-[calc(var(--editor-spacing)*10+2px)]": !context.isOpen,
                "mb-2": !context.isOpen,
                "transition-all": true,
                "duration-300": true,
                "ease-in-out": true,
                "pointer-events-none": true
            },
            on: {
                keydown: (event: KeyboardEvent) => {
                    context.handleKeyDown(event);
                },
                keyup: (event: KeyboardEvent) => {
                    context.handleKeyUp(event);
                }
            }
        },
        generateToolbarView(context),
        hasItems ? generateDetailsPanelView(context) : undefined
    );
}

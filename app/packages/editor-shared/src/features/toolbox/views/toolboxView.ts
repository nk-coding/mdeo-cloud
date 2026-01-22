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
                // Root container needs positioning to anchor the toolbox and error panel
                // Positioned absolutely in top-right corner like hylimo
                absolute: true,
                "top-4": true,
                "right-4": true,
                flex: true,
                "flex-col": true,
                "z-50": true
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
    return html(
        "div",
        {
            class: {
                flex: true,
                "flex-col": true,
                "gap-2": true,
                "w-[calc(var(--editor-spacing)*10+2px)]": !context.isOpen,
                "max-h-[calc(var(--editor-spacing)*10+2px)]": !context.isOpen,
                "mb-2": !context.isOpen
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
        generateDetailsPanelView(context)
    );
}

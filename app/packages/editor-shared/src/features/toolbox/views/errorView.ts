import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { Toolbox } from "../toolbox.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");
const { AlertTriangle } = sharedImport("lucide");

import { generateIcon } from "./iconView.js";

/**
 * Generates the error display compartment for the toolbox.
 * Shows error messages when the diagram is in an invalid state.
 * Displays as a compact button that expands on hover to show full error messages.
 * Uses max-height for both collapsed and expanded states to allow smooth Y-direction
 * expansion while keeping scrollability for very large content.
 *
 * @param context The toolbox context
 * @returns The error display VNode or undefined if no errors
 */
export function generateErrorView(context: Toolbox): VNode | undefined {
    if (!context.errorState || context.errorState.messages.length === 0) {
        return undefined;
    }

    return html(
        "div",
        {
            class: {
                absolute: true,
                "top-full": true,
                "right-0": true,
                "mt-2": context.isBottomPanelOpen && context.isOpen,
                "w-10": true,
                "max-h-10": true,
                "box-content": true,
                "hover:w-[calc(var(--editor-spacing)*68-2px)]": true,
                "hover:max-h-[300px]": true,
                "rounded-md": true,
                border: true,
                "border-border": true,
                "shadow-[0_8px_16px_rgba(0,0,0,0.12)]": true,
                "bg-toolbox": true,
                "overflow-hidden": true,
                "transition-all": true,
                "duration-300": true,
                "ease-in-out": true,
                flex: true,
                "flex-col": true,
                group: true
            }
        },
        generateErrorContainer(context)
    );
}

/**
 * Generates the error container with background and content.
 *
 * @param context The toolbox context
 * @returns The error container VNode
 */
function generateErrorContainer(context: Toolbox): VNode {
    return html(
        "div",
        {
            class: {
                "bg-destructive/10": true,
                flex: true,
                "overflow-hidden": true,
                "flex-1": true,
                relative: true
            }
        },
        generateErrorButton(),
        generateErrorList(context)
    );
}

/**
 * Generates the error button (icon) that's visible in collapsed state.
 *
 * @returns The error button VNode
 */
function generateErrorButton(): VNode {
    return html(
        "div",
        {
            class: {
                "size-8": true,
                absolute: true,
                "top-1": true,
                "right-1": true,
                "rounded-md": true,
                "inline-flex": true,
                "items-center": true,
                "justify-center": true,
                "text-destructive": true,
                "transition-opacity": true,
                "duration-200": true,
                "group-hover:opacity-0": true,
                "pointer-events-none": true
            }
        },
        generateIcon(AlertTriangle, ["h-4", "w-4"])
    );
}

/**
 * Generates the error list that appears on hover.
 *
 * @param context The toolbox context
 * @returns The error list VNode
 */
function generateErrorList(context: Toolbox): VNode {
    return html(
        "div",
        {
            class: {
                "overflow-y-auto": true,
                flex: true,
                "flex-col": true,
                "w-full": true,
                "px-2": true,
                "py-2": true,
                "text-destructive": true,
                "text-sm": true,
                "opacity-0": true,
                "transition-opacity": true,
                "duration-200": true,
                "group-hover:opacity-100": true
            }
        },
        ...context.errorState!.messages.map((message) => generateErrorItem(message))
    );
}

/**
 * Generates a single error item.
 *
 * @param message The error message
 * @returns The error item VNode
 */
function generateErrorItem(message: string): VNode {
    return html(
        "div",
        {
            class: {
                "py-1.5": true,
                "px-1": true,
                "select-text": true,
                "break-words": true,
                "border-b": true,
                "border-destructive/20": true,
                "last:border-b-0": true
            }
        },
        message
    );
}

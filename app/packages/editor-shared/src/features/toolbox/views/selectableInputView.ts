import type { VNode } from "snabbdom";
import { sharedImport } from "../../../sharedImport.js";
import type { IconNode } from "lucide";
import type { Toolbox } from "../toolbox.js";
import { inputClasses } from "./styles.js";

const { html } = sharedImport("@eclipse-glsp/sprotty");
const { ChevronDown } = sharedImport("lucide");

import { generateIcon } from "./iconView.js";

/**
 * Generates a selectable input component with icon and dropdown.
 * Used for connection type selectors and similar UI elements.
 *
 * @param context The toolbox context
 * @param icon The icon to display
 * @param currentValue The currently selected value
 * @param placeholder The placeholder text when no value is selected
 * @param onOpen Callback when the dropdown is opened
 * @returns The selectable input VNode
 */
export function generateSelectableInputView(
    context: Toolbox,
    icon: IconNode,
    currentValue: string,
    placeholder: string,
    onOpen: () => void
): VNode {
    return html(
        "div",
        {
            class: {
                ...inputClasses,
                "selectable-input": true,
                flex: true,
                "items-center": true,
                "gap-2": true,
                "cursor-pointer": true,
                "hover:bg-accent": true
            },
            on: {
                click: (event: Event) => {
                    event.stopPropagation();
                    onOpen();
                    context.update();
                }
            }
        },
        generateIcon(icon, ["h-4", "w-4", "text-muted-foreground"]),
        html(
            "span",
            {
                class: {
                    "flex-1": true,
                    truncate: true,
                    "text-muted-foreground": !currentValue
                }
            },
            currentValue || placeholder
        ),
        generateIcon(ChevronDown, ["h-4", "w-4", "text-muted-foreground"])
    );
}
